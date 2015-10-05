package gov.usgs.cida.dsas.wps;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

import gov.usgs.cida.dsas.util.BaselineDistanceAccumulator;
import gov.usgs.cida.dsas.util.CRSUtils;
import gov.usgs.cida.dsas.util.GeomAsserts;
import gov.usgs.cida.dsas.util.LayerImportUtil;
import gov.usgs.cida.dsas.util.UTMFinder;
import gov.usgs.cida.dsas.exceptions.PoorlyDefinedBaselineException;
import gov.usgs.cida.dsas.exceptions.UnsupportedCoordinateReferenceSystemException;
import gov.usgs.cida.dsas.wps.geom.Intersection;
import gov.usgs.cida.dsas.wps.geom.ProxyDatumBias;
import gov.usgs.cida.dsas.wps.geom.ShorelineFeature;
import gov.usgs.cida.dsas.wps.geom.ShorelineSTRTreeBuilder;
import gov.usgs.cida.dsas.wps.geom.Transect;
import gov.usgs.cida.utilities.features.AttributeGetter;
import gov.usgs.cida.utilities.features.Constants.Orientation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.joda.time.DateTime;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static gov.usgs.cida.dsas.wps.geom.Intersection.calculateIntersections;
import static gov.usgs.cida.utilities.features.Constants.*;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
@DescribeProcess(
        title = "Generate Transects and Intersections",
        description = "Create a transect layer from the baseline and shorelines",
        version = "1.0.0")
public class CreateTransectsAndIntersectionsProcess implements GeoServerProcess {

    private LayerImportUtil importer;
    
    public CreateTransectsAndIntersectionsProcess(ImportProcess importProcess, Catalog catalog) {
        this.importer = new LayerImportUtil(catalog, importProcess);
    }

    /** May actually want to return reference to new layer
     *  Check whether we need an offset at the start of the baseline
     * 
     *  With new maxLength algorithm, we may want to allow a maxLength parameter to speed up calculation
	 * @param shorelines feature collection of shorelines
	 * @param baseline feature collection of baselines
	 * @param biasRef feature collection of PDBC bias reference line
	 * @param spacing spacing in meters of transects along baseline
	 * @param smoothing how much smoothing to apply to transect generation
	 * @param farthest whether to use nearest or farthest intersection of shoreline (default false)
	 * @param workspace workspace in which to create new layers
	 * @param store store in which to create new layers
	 * @param transectLayer name of created transect layer
	 * @param intersectionLayer name of created intersection layer
	 * @return layer names of transects and intersections
	 * @throws java.lang.Exception no exceptions caught, may throw anything
     */
    @DescribeResult(name = "transects", description = "Layer containing Transects normal to baseline")
    public String execute(
            @DescribeParameter(name = "shorelines", min = 1, max = 1) SimpleFeatureCollection shorelines,
            @DescribeParameter(name = "baseline", min = 1, max = 1) SimpleFeatureCollection baseline,
            @DescribeParameter(name = "biasRef", min = 0, max = 1) SimpleFeatureCollection biasRef,
            @DescribeParameter(name = "spacing", min = 1, max = 1) Double spacing,
            @DescribeParameter(name = "smoothing", min = 0, max = 1) Double smoothing, 
            @DescribeParameter(name = "farthest", min = 0, max = 1) Boolean farthest,
            @DescribeParameter(name = "workspace", min = 1, max = 1) String workspace,
            @DescribeParameter(name = "store", min = 1, max = 1) String store,
            @DescribeParameter(name = "transectLayer", min = 1, max = 1) String transectLayer,
            @DescribeParameter(name = "intersectionLayer", min = 1, max = 1) String intersectionLayer) throws Exception {
        // defaults
        if (smoothing == null) { smoothing = 0d; }
        if (farthest == null) {farthest = false; }
        return new Process(shorelines, baseline, biasRef, spacing, smoothing, farthest, workspace, store, transectLayer, intersectionLayer).execute();
    }
    
    protected class Process {
        private static final double MIN_TRANSECT_LENGTH = 50.0d; // meters
        private static final double TRANSECT_PADDING = 5.0d; // meters
        
        private final FeatureCollection<SimpleFeatureType, SimpleFeature> shorelineFeatureCollection;
        private final FeatureCollection<SimpleFeatureType, SimpleFeature> baselineFeatureCollection;
        private final FeatureCollection<SimpleFeatureType, SimpleFeature> biasRefFeatureCollection;
        private final double spacing;
        private final double smoothing;
        private final boolean useFarthest;
        private final boolean doNotPerformBiasCorrection;
        private final String workspace;
        private final String store;
        private final String transectLayer;
        private final String intersectionLayer;
        
        private CoordinateReferenceSystem utmCrs;
        
        private STRtree strTree;
        private STRtree biasTree;
        private SimpleFeatureType shorelineFeatureType;
        private SimpleFeatureType transectFeatureType;
        private SimpleFeatureType intersectionFeatureType;
        private SimpleFeatureType biasIncomingFeatureType;
        private PreparedGeometry preparedShorelines;
        
        private double maxTransectLength;
        private int transectId;
        
        private SimpleFeatureCollection resultTransectsCollection;
        private SimpleFeatureCollection resultIntersectionsCollection;
        
        protected Process(FeatureCollection<SimpleFeatureType, SimpleFeature> shorelines,
                FeatureCollection<SimpleFeatureType, SimpleFeature> baseline,
                FeatureCollection<SimpleFeatureType, SimpleFeature> biasRef,
                double spacing,
                double smoothing,
                Boolean farthest,
                String workspace,
                String store,
                String transectLayer,
                String intersectionLayer) {
            this.shorelineFeatureCollection = shorelines;
            this.baselineFeatureCollection = baseline;

            if (biasRef == null) {
                this.doNotPerformBiasCorrection = true;
                this.biasRefFeatureCollection = null;
            } else {
                this.doNotPerformBiasCorrection = false;
                this.biasRefFeatureCollection = biasRef;
            }
            

            this.spacing = spacing;
            this.smoothing = smoothing;
            this.useFarthest = farthest;

            this.workspace = workspace;
            this.store = store;
            this.transectLayer = transectLayer;
            this.intersectionLayer = intersectionLayer;
            
            this.maxTransectLength = 0; // start out small
            this.transectId = 0; // start ids at 0
            
            // Leave these null to start, they get populated after error checks occur (somewhat expensive)
            this.strTree = null;
            this.biasTree = null;
            this.shorelineFeatureType = null;
            this.transectFeatureType = null;
            this.intersectionFeatureType = null;
            this.biasIncomingFeatureType = null;
            this.preparedShorelines = null;
            
            this.resultTransectsCollection = null;
            this.resultIntersectionsCollection = null;
        }
        
        protected String execute() throws Exception {
            importer.checkIfLayerExists(workspace, transectLayer);
            importer.checkIfLayerExists(workspace, intersectionLayer);
            
            CoordinateReferenceSystem shorelinesCrs = CRSUtils.getCRSFromFeatureCollection(shorelineFeatureCollection);
            CoordinateReferenceSystem baselineCrs = CRSUtils.getCRSFromFeatureCollection(baselineFeatureCollection);
            CoordinateReferenceSystem biasCrs = null;
            if (!doNotPerformBiasCorrection) {
                biasCrs = CRSUtils.getCRSFromFeatureCollection(biasRefFeatureCollection);
            }

            if (!CRS.equalsIgnoreMetadata(shorelinesCrs, REQUIRED_CRS_WGS84)) {
                throw new UnsupportedCoordinateReferenceSystemException("Shorelines are not in accepted projection");
            }
            if (!CRS.equalsIgnoreMetadata(baselineCrs, REQUIRED_CRS_WGS84)) {
                throw new UnsupportedCoordinateReferenceSystemException("Baseline is not in accepted projection");
            }
            if (!doNotPerformBiasCorrection && !CRS.equalsIgnoreMetadata(biasCrs, REQUIRED_CRS_WGS84)) {
                throw new UnsupportedCoordinateReferenceSystemException("Bias reference is not in accepted projection");
            }
            this.utmCrs = UTMFinder.findUTMZoneCRSForCentroid((SimpleFeatureCollection)shorelineFeatureCollection);
            if (this.utmCrs == null) {
                throw new IllegalStateException("Must have usable UTM zone to continue");
            }
            
            SimpleFeatureCollection transformedShorelines = CRSUtils.transformFeatureCollection(shorelineFeatureCollection, REQUIRED_CRS_WGS84, utmCrs);
            SimpleFeatureCollection transformedBaselines = CRSUtils.transformFeatureCollection(baselineFeatureCollection, REQUIRED_CRS_WGS84, utmCrs);
            SimpleFeatureCollection transformedBiasRef = null;
            if (!doNotPerformBiasCorrection) {
                transformedBiasRef = CRSUtils.transformFeatureCollection(biasRefFeatureCollection, REQUIRED_CRS_WGS84, utmCrs);
            }

            // this could be from a parameter?
            this.maxTransectLength = calculateMaxDistance(transformedShorelines, transformedBaselines);
            
            MultiLineString shorelineGeometry = CRSUtils.getLinesFromFeatureCollection(transformedShorelines);
            MultiLineString baselineGeometry = CRSUtils.getLinesFromFeatureCollection(transformedBaselines);

            this.strTree = new ShorelineSTRTreeBuilder(transformedShorelines).build();
            if (!doNotPerformBiasCorrection) {
                this.biasTree = new ShorelineSTRTreeBuilder(transformedBiasRef).build();
            }
 
            this.shorelineFeatureType = transformedShorelines.getSchema();
            this.transectFeatureType = Transect.buildFeatureType(utmCrs);
            this.intersectionFeatureType = Intersection.buildSimpleFeatureType(transformedShorelines, utmCrs);
            if (!doNotPerformBiasCorrection) {
                this.biasIncomingFeatureType = biasRefFeatureCollection.getSchema();
            }
            this.preparedShorelines = PreparedGeometryFactory.prepare(shorelineGeometry);
            GeomAsserts.assertBaselinesDoNotCrossShorelines(preparedShorelines, baselineGeometry);
            
            Transect[] vectsOnBaseline = getEvenlySpacedOrthoVectorsAlongBaseline(transformedBaselines, shorelineGeometry, spacing);
            
            trimTransectsToFeatureCollection(vectsOnBaseline, transformedShorelines);
            String createdTransectLayer = importer.importLayer(resultTransectsCollection, workspace, store, transectLayer, utmCrs, ProjectionPolicy.REPROJECT_TO_DECLARED);
            String createdIntersectionLayer = importer.importLayer(resultIntersectionsCollection, workspace, store, intersectionLayer, utmCrs, ProjectionPolicy.REPROJECT_TO_DECLARED);
            return createdTransectLayer + "," + createdIntersectionLayer;
        }

        protected Transect[] getEvenlySpacedOrthoVectorsAlongBaseline(SimpleFeatureCollection baseline, MultiLineString shorelines, double spacing) {
            List<Transect> vectList = new LinkedList<Transect>();
            
            BaselineDistanceAccumulator accumulator = new BaselineDistanceAccumulator();
            AttributeGetter attGet = new AttributeGetter(baseline.getSchema());
			
            SimpleFeatureIterator features = null;
			try {
				features = baseline.features();
				while (features.hasNext()) {
					SimpleFeature feature = features.next();
					String orientVal = (String)attGet.getValue(BASELINE_ORIENTATION_ATTR, feature);
					Orientation orientation = Orientation.fromAttr(orientVal);
					if (orientation == Orientation.UNKNOWN) {
						// default to seaward
						orientation = Orientation.SEAWARD;
					}
					String baselineId = feature.getID();

					MultiLineString lines = CRSUtils.getMultilineFromFeature(feature);
					for (int i=0; i<lines.getNumGeometries(); i++) { // probably only one Linestring
						LineString line = (LineString)lines.getGeometryN(i);
						int direction = shorelineDirection(line, shorelines);

						double baseDist = accumulator.accumulate(line);

						vectList.addAll(handleLineString(line, spacing, orientation, direction, baselineId, baseDist)); // rather than SEAWARD, get from baseline feature
					}
				}
			} finally {
				if (null != features) {
					features.close();
				}
			}
            Transect[] vectArr = new Transect[vectList.size()];
            return vectList.toArray(vectArr);
        }
        
        /**
         * 
         * @param vectsOnBaseline
         * @param baseline
         * @param shorelines
         * @return 
         */
        protected void trimTransectsToFeatureCollection(Transect[] vectsOnBaseline, SimpleFeatureCollection shorelines) {
            if (vectsOnBaseline.length == 0) {
                return;
            } 
            List<SimpleFeature> transectFeatures = new LinkedList<SimpleFeature>();
            List<SimpleFeature> intersectionFeatures = new LinkedList<SimpleFeature>();
            AttributeGetter attGet = new AttributeGetter(intersectionFeatureType);
            AttributeGetter biasGetter = new AttributeGetter(biasIncomingFeatureType);
            // grow by about 200?
            double guessTransectLength = MIN_TRANSECT_LENGTH * 4;
            
            for (Transect transect : vectsOnBaseline) {
                Map<DateTime, Intersection> allIntersections = Maps.newHashMap();
                double startDistance = 0;
                ProxyDatumBias biasCorrection = null;
                
                do {
                    Transect subTransect = transect.subTransect(startDistance, guessTransectLength);
                    startDistance += guessTransectLength;
                    Intersection.updateIntersectionsWithSubTransect
                            (allIntersections, transect.getOriginPoint(), subTransect, strTree, useFarthest, attGet);
                    if (biasCorrection == null && !doNotPerformBiasCorrection) {
                        biasCorrection = getBiasValue(subTransect, biasTree, biasGetter);
                    }
                }
                while (startDistance < maxTransectLength);
                
                if (!allIntersections.isEmpty()) {  // ignore non-crossing lines
                    
                    double transectLength = Intersection.absoluteFarthest(MIN_TRANSECT_LENGTH, allIntersections.values());
                    transect.setLength(transectLength + TRANSECT_PADDING);
                    transect.setBias(biasCorrection);
                    SimpleFeature feature = transect.createFeature(transectFeatureType);
                    transectFeatures.add(feature);

                    for (Intersection intersection : allIntersections.values()) {
                        // do I need to worry about order?
                    	intersection.setBias(biasCorrection);
                        intersectionFeatures.add(intersection.createFeature(intersectionFeatureType));
                    }
                }
            }
            resultTransectsCollection = DataUtilities.collection(transectFeatures);
            resultIntersectionsCollection = DataUtilities.collection(intersectionFeatures);
        }

        /**
         * Vectors point 90&deg; counterclockwise currently
         * @param lineString line along which to get vectors
         * @param spacing how often to create vectors along line
		 * @param orientation
		 * @param orthoDirection
		 * @param baselineId
		 * @param accumulatedBaselineLength
         * @return List of fancy vectors I concocted
         */
        protected List<Transect> handleLineString(LineString lineString, 
                double spacing, 
                Orientation orientation, 
                int orthoDirection,
                String baselineId,
                double accumulatedBaselineLength) {
            List<LineSegment> intervals = findIntervals(lineString, true, spacing, smoothing);
            List<Transect> transects = new ArrayList<>(intervals.size());
            for (LineSegment interval : intervals) {
                transects.add(
                    Transect.generatePerpendicularVector(
                        interval.p0, interval, orientation, transectId++, baselineId, accumulatedBaselineLength, orthoDirection));
                accumulatedBaselineLength += spacing;
            }
            return transects;
        }
        
        /**
         * Gives direction to point transects as long as baseline is good
         * This is pretty much just hacked up at this point
         * Any better way of doing this would be smart
         * 
         * Use Shoreward orientation so distances are positive (otherwise we should use absolute distance)
         * @param baseline
         * @param shorelines
         * @return 
         */
        protected int shorelineDirection(LineString baseline, Geometry shorelines) {
            Coordinate[] coordinates = baseline.getCoordinates();
            int n = coordinates.length;
            LineSegment a = new LineSegment(coordinates[0], coordinates[1]);
            LineSegment b = new LineSegment(coordinates[n-1], coordinates[n-2]);
            LineSegment m = null;
            if (n > 2) {
                m = new LineSegment(coordinates[(int)n/2], coordinates[(int)n/2+1]);
            }
            
            double[] averages = new double[] { 0d, 0d };
            Transect vector =
                    Transect.generatePerpendicularVector(coordinates[0], a, Orientation.SHOREWARD, 0, "0", Double.NaN, Angle.CLOCKWISE);
            averages[0] = averageDistance(vector);
            vector.rotate180Deg();
            averages[1] = averageDistance(vector);
            
            vector = Transect.generatePerpendicularVector(coordinates[n-1], b, Orientation.SHOREWARD, 0, "0", Double.NaN, Angle.COUNTERCLOCKWISE);
            double currentAvg = averageDistance(vector);
            averages[0] = (averages[0] < currentAvg) ? averages[0] : currentAvg;
            vector.rotate180Deg();
            currentAvg = averageDistance(vector);
            averages[1] = (averages[1] < currentAvg) ? averages[1] : currentAvg;
            
            if (m != null) {
                vector = Transect.generatePerpendicularVector(coordinates[(int)n/2], m, Orientation.SHOREWARD, 0, "0", Double.NaN, Angle.CLOCKWISE);
                currentAvg = averageDistance(vector);
                averages[0] = (averages[0] < currentAvg) ? averages[0] : currentAvg;
                vector.rotate180Deg();
                currentAvg = averageDistance(vector);
                averages[1] = (averages[1] < currentAvg) ? averages[1] : currentAvg;
            }
            
            if (averages[0] < averages[1]) {
                return Angle.CLOCKWISE;
            }
            else if (averages[0] > averages[1]) {
                return Angle.COUNTERCLOCKWISE;
            }
            throw new PoorlyDefinedBaselineException("Baseline is ambiguous, transect direction cannot be determined");
        }
        
        private double averageDistance(Transect transect) {
            double average = Double.MAX_VALUE;
            transect.setLength(maxTransectLength);
            double total = 0d;
            AttributeGetter instersectionGetter = new AttributeGetter(intersectionFeatureType);
            Map<DateTime, Intersection> intersections = Intersection.calculateIntersections(transect, strTree, useFarthest, instersectionGetter);
            for (Intersection point : intersections.values()) {
                total += point.getDistance();
            }
            if (intersections.size() > 0) {
                average = total / intersections.size();
            }
            return average;
        }
    }
    
    // NOTE: For each segment p0 is interval coord, with p1 = p0 + direction of segment as unit vector.
    public static List<LineSegment> findIntervals(LineString lineString, boolean includeOrigin, double interval) {
        LinkedList<LineSegment> intervalList = new LinkedList<LineSegment>();
        List<LineSegment> segmentList = toLineSegments(lineString);       
        double progress = 0;
        double next = includeOrigin ? 0 : interval;
        for (LineSegment segment : segmentList) {
            double segmentLength = segment.getLength();
            if (progress + segmentLength >= next) {
                double segmentAngle = segment.angle();
                double segmentOffset;
                for (segmentOffset = next - progress; segmentOffset <= segmentLength; segmentOffset += interval, next += interval) {
                    Coordinate c = segment.pointAlong(segmentOffset / segmentLength);
                    intervalList.add(new LineSegment(
                            c.x, c.y, c.x + Math.cos(segmentAngle), c.y + Math.sin(segmentAngle)));
                }
            }
            progress += segmentLength;
        }
        return intervalList;
    }
    
    // NOTE: For each segment p0 is interval coord, with p1 = p0 + direction of segment as unit vector.
    public static List<LineSegment> findIntervals(LineString lineString, boolean includeOrigin, double interval, double smoothing) {
        if (smoothing <= 0 || lineString.getNumPoints() == 2) {
            return findIntervals(lineString, includeOrigin, interval);
        }
        LinkedList<LineSegment> intervalList = new LinkedList<LineSegment>();
        List<LineSegment> segmentList = toLineSegments(lineString);     
        double progress = 0;
        double next = includeOrigin ? 0 : interval;
        final double half = smoothing / 2d;
        int index = 0;
        final int count = segmentList.size();
        for (LineSegment segment : segmentList) {
            final double length = segment.getLength();
            if (progress + length >= next) {
                double offset;
                for (offset = next - progress; offset <= length; offset += interval, next += interval) {
                    Coordinate intervalCenter = segment.pointAlong(offset / length);
                    double low = offset - half; // offset from p0 of current segment for low bound of smoothed segment (may be < 0)
                    double high = offset + half; // offset from p0 of current segment for high bound of smoothed segment (may be > length)
                    double angle;
                    if (low < 0 || high > length) {
                        Coordinate intervalLow = low < 0 && index > 0 ?
                                    // find distance along line string from end of *last* segment (removing distance along current segment);
                                    fromEnd(segmentList.subList(0, index), 0 - low, false) :
                                    // otherwise project along segment
                                    segment.pointAlong(low / length);
                        Coordinate intervalHigh = high > length && index < (count - 1) ?
                                    // find distance along line string from end of *last* segment (removing distance along current segment);
                                    fromStart(segmentList.subList(index + 1, count), high - length, false) :
                                    // otherwise project along segment
                                    segment.pointAlong(high / length);
                        LineSegment smoothSegment = new LineSegment(intervalLow, intervalHigh);
                        angle = smoothSegment.angle();
                    } else {
                        angle = segment.angle();
                    }
                    intervalList.add(new LineSegment(
                        intervalCenter.x, intervalCenter.y, intervalCenter.x + Math.cos(angle), intervalCenter.y + Math.sin(angle)));
                }
            }
            index++;
            progress += length;
        }
        return intervalList;
    }
    
    // Extracts consituent segments from line string
    public static List<LineSegment> toLineSegments(LineString line) {
        int cCount = line.getNumPoints();
        List<LineSegment> segments = new ArrayList<LineSegment>(cCount -1);
        for(int cIndex = 1; cIndex < cCount; ++cIndex) {
            segments.add(new LineSegment(line.getCoordinateN(cIndex -1), line.getCoordinateN(cIndex)));
        }
        return segments;
    }
        
    static Coordinate fromStart(LineString lineString, double distance, boolean clamp) {
        return fromStart(toLineSegments(lineString), distance, clamp);
    }
    
    static Coordinate fromStart(List<LineSegment> lineSegments, double distance, boolean clamp) {
        ListIterator<LineSegment> iterator = lineSegments.listIterator();
        double progress = 0;
        while (iterator.hasNext()) {
            final LineSegment segement = iterator.next();
            final double length = segement.getLength();
            final double offset = distance - progress;
            if (offset <= length) {
                return segement.pointAlong(offset / length);
            }
            progress += length;
        }
        // special case handling, distance is longer than line
        LineSegment last = lineSegments.get(lineSegments.size() - 1);
        // clamp or project?
        if (clamp) {
            // end of last segment in list
            return last.getCoordinate(1);
        } else {
            double overflow = distance - progress;
            // project along last line segment, add 1 since this method
            // uses fraction-of-segment-length as distance from p0 and our
            // overflow is distance from p1
            return last.pointAlong(1d + (overflow / last.getLength()));
        }
    }
    
    static Coordinate fromEnd(LineString lineString, double distance, boolean clamp) {
        return fromEnd(toLineSegments(lineString), distance, clamp);
    }
    
    static Coordinate fromEnd(List<LineSegment> lineSegments, double distance, boolean clamp) {
        ListIterator<LineSegment> iterator = lineSegments.listIterator(lineSegments.size());
        double progress = 0;
        while (iterator.hasPrevious()) {
            final LineSegment segement = iterator.previous();
            final double length = segement.getLength();
            final double offset = distance - progress;
            if (offset <= length) {
                // since method is fraction from p0, but offset is distance from p1.
                return segement.pointAlong(1d - (offset / length));
            }
            progress += length;
        }
        // special case handling, distance is longer than line
        LineSegment first = lineSegments.get(0);
        // clamp or project?
        if (clamp) {
            // start of first segment in list
            return first.getCoordinate(0);
        } else {
            double overflow = distance - progress;
            // project along first line segment, negate since this method
            // uses fraction-of-segment-length as distance from p0 towards p1 and our
            // overflow is distance from p0 (in p1 to p0 direction)
            return first.pointAlong(-(overflow / first.getLength()));
        }
    }
    
    private static double calculateMaxDistance(SimpleFeatureCollection transformedShorelines, SimpleFeatureCollection transformedBaselines) {
        ReferencedEnvelope shorelineBounds = transformedShorelines.getBounds();
        ReferencedEnvelope baselineBounds = transformedBaselines.getBounds();
        
        double[] slUpperCorner1 = shorelineBounds.getUpperCorner().getCoordinate();
        double[] slLowerCorner1 = shorelineBounds.getLowerCorner().getCoordinate();
        double[] slUpperCorner2 = new double[] { slLowerCorner1[0], slUpperCorner1[1] };
        double[] slLowerCorner2 = new double[] { slUpperCorner1[0], slLowerCorner1[1] };
        
        double[] blUpperCorner1 = baselineBounds.getUpperCorner().getCoordinate();
        double[] blLowerCorner1 = baselineBounds.getLowerCorner().getCoordinate();
        double[] blUpperCorner2 = new double[] { blLowerCorner1[0], blUpperCorner1[1] };
        double[] blLowerCorner2 = new double[] { blUpperCorner1[0], blLowerCorner1[1] };
        
        double[][] pointsCompare = { slUpperCorner1, slUpperCorner2, slLowerCorner1,
            slLowerCorner2, blUpperCorner1, blUpperCorner2, blLowerCorner1, blLowerCorner2 };
        
        double maxDist = 0.0d;
        for (int i=0; i<4; i++) {
            Coordinate a = new Coordinate(pointsCompare[i][0], pointsCompare[i][1]);
            for (int j=0; j<4; j++) {
                Coordinate b = new Coordinate(pointsCompare[4+j][0], pointsCompare[4+j][1]);
                double dist = a.distance(b);
                if (dist > maxDist) {
                    maxDist = dist;
                }
            }
        }
        
        return maxDist;
    }
	
	public static ProxyDatumBias getBiasValue(Transect transect, STRtree biasTree, AttributeGetter biasGetter) {
		ProxyDatumBias proxy = null;
		LineString line = transect.getLineString();
		List<ShorelineFeature> possibleIntersects = biasTree.query(line.getEnvelopeInternal());
		for (ShorelineFeature feature : possibleIntersects) {
			LineString segment = feature.segment;
			if (segment.intersects(line)) {
				Point intersection = (Point)segment.intersection(line);
				double slopeVal = feature.interpolate(intersection, AVG_SLOPE_ATTR, biasGetter, Double.NaN);
				double biasVal = feature.interpolate(intersection, BIAS_ATTR, biasGetter, DEFAULT_BIAS);
				double uncybVal = feature.interpolate(intersection, BIAS_UNCY_ATTR, biasGetter, DEFAULT_BIAS_UNCY);
				proxy = new ProxyDatumBias(slopeVal, biasVal, uncybVal);
			}
		}
		return proxy;
	}
}
