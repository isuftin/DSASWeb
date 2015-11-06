/*
 * U.S.Geological Survey Software User Rights Notice
 * 
 * Copied from http://water.usgs.gov/software/help/notice/ on September 7, 2012.  
 * Please check webpage for updates.
 * 
 * Software and related material (data and (or) documentation), contained in or
 * furnished in connection with a software distribution, are made available by the
 * U.S. Geological Survey (USGS) to be used in the public interest and in the 
 * advancement of science. You may, without any fee or cost, use, copy, modify,
 * or distribute this software, and any derivative works thereof, and its supporting
 * documentation, subject to the following restrictions and understandings.
 * 
 * If you distribute copies or modifications of the software and related material,
 * make sure the recipients receive a copy of this notice and receive or can get a
 * copy of the original distribution. If the software and (or) related material
 * are modified and distributed, it must be made clear that the recipients do not
 * have the original and they must be informed of the extent of the modifications.
 * 
 * For example, modified files must include a prominent notice stating the 
 * modifications made, the author of the modifications, and the date the 
 * modifications were made. This restriction is necessary to guard against problems
 * introduced in the software by others, reflecting negatively on the reputation of the USGS.
 * 
 * The software is public property and you therefore have the right to the source code, if desired.
 * 
 * You may charge fees for distribution, warranties, and services provided in connection
 * with the software or derivative works thereof. The name USGS can be used in any
 * advertising or publicity to endorse or promote any products or commercial entity
 * using this software if specific written permission is obtained from the USGS.
 * 
 * The user agrees to appropriately acknowledge the authors and the USGS in publications
 * that result from the use of this software or in products that include this
 * software in whole or in part.
 * 
 * Because the software and related material are free (other than nominal materials
 * and handling fees) and provided "as is," the authors, the USGS, and the 
 * United States Government have made no warranty, express or implied, as to accuracy
 * or completeness and are not obligated to provide the user with any support, consulting,
 * training or assistance of any kind with regard to the use, operation, and performance
 * of this software nor to provide the user with any updates, revisions, new versions or "bug fixes".
 * 
 * The user assumes all risk for any damages whatsoever resulting from loss of use, data,
 * or profits arising in connection with the access, use, quality, or performance of this software.
 */

package gov.usgs.cida.dsas.wps;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.exceptions.InputFileFormatException;
import gov.usgs.cida.dsas.exceptions.UnsupportedCoordinateReferenceSystemException;
import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.dsas.util.CRSUtils;
import gov.usgs.cida.dsas.util.LayerImportUtil;
import gov.usgs.cida.dsas.util.UTMFinder;
import gov.usgs.cida.utilities.features.AttributeGetter;
import gov.usgs.cida.utilities.features.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
@DescribeProcess(
    title = "Create Result Layer From Statistics",
    description = "Clone transect feature collection, append statistics results",
    version = "1.0.0")
public class CreateResultsLayerProcess implements GeoServerProcess {

    private LayerImportUtil importer;
    
    public CreateResultsLayerProcess(ImportProcess importProcess, Catalog catalog) {
        importer = new LayerImportUtil(catalog, importProcess);
    }
    
    @DescribeResult(name = "resultLayer", description = "Layer containing results of shoreline statistics")
    public String execute(@DescribeParameter(name = "results", description = "Block of text with TransectID and stats results", min = 1, max = 1) StringBuffer results,
            @DescribeParameter(name = "transects", description = "Feature collection of transects to join", min = 1, max = 1) FeatureCollection<SimpleFeatureType, SimpleFeature> transects,
            @DescribeParameter(name = "intersects", description = "Feature collection of intersects used to calculate results", min = 0, max = 1) FeatureCollection<SimpleFeatureType, SimpleFeature> intersects,
            @DescribeParameter(name = "workspace", description = "Workspace in which to put results layer", min = 1, max = 1) String workspace,
            @DescribeParameter(name = "store", description = "Store in which to put results", min = 1, max = 1) String store,
            @DescribeParameter(name = "layer", description = "Layer name of results", min = 1, max = 1) String layer) throws Exception {
        
        return new Process(results, transects, intersects, workspace, store, layer).execute();
    }
    
    protected class Process {
        
        /* this is different from the one in Constants, this is a "contract" with the R process on the column name for TransectId*/
        public static final String TRANSECT_ID = "transect_ID";
        
        private final String results;
        private final FeatureCollection<SimpleFeatureType, SimpleFeature> transects;
        private final FeatureCollection<SimpleFeatureType, SimpleFeature> intersects;
        private final String workspace;
        private final String store;
        private final String layer;
        
        protected Process(StringBuffer results,
                FeatureCollection<SimpleFeatureType, SimpleFeature> transects,
                FeatureCollection<SimpleFeatureType, SimpleFeature> intersects,
                String workspace,
                String store,
                String layer) {
            this.results = results.toString();
            this.transects = transects;
            this.intersects = intersects;
            this.workspace = workspace;
            this.store = store;
            this.layer = layer;
        }
        
        protected String execute() {
            importer.checkIfLayerExists(workspace, layer);
            String[] columnHeaders = getColumnHeaders(results);
            Map<Long, Double[]> resultMap = parseTextToMap(results, columnHeaders);
            List<SimpleFeature> joinedFeatures = joinResultsToTransects(columnHeaders, resultMap, transects);
            CoordinateReferenceSystem utmZone = null;
            try {
                utmZone = UTMFinder.findUTMZoneCRSForCentroid((SimpleFeatureCollection)transects);
                
                CoordinateReferenceSystem tCRS = CRSUtils.getCRSFromFeatureCollection(transects);
                CoordinateReferenceSystem iCRS = CRSUtils.getCRSFromFeatureCollection(intersects);
                // NOTE: I tried CRS.equalsIgnoreMetadata(o1, o2) but it did seem to work correctly (GT 8.7)
                // NOTE: I tried AbstractCRS)tCRS).equals((AbstractCRS)iCRS, false) but it did not seem to work correctly (GT 8.7)
                // This is too strict as it won't resolve identical CRS defined by different authorities.  But what to do
                // concerning issues noted above?  An example of a potential issue is a transect derived from a WFS via EPSG code
                // and an intersect file provided vias Base64 encoded shapefile.  The EPSG code and PRJ could reference the 
                // same UTM zone, but the test below would fail.
                if (!tCRS.getName().equals(iCRS.getName())) {
                    throw new IllegalStateException("Transects and Intersects do not share common Coordinate Reference System");
                }
            } catch (FactoryException | TransformException ex) {
                throw new UnsupportedCoordinateReferenceSystemException("Could not find utm zone", ex);
            }
            SimpleFeatureCollection collection = DataUtilities.collection(joinedFeatures);
            String imported = importer.importLayer(collection, workspace, store, layer, utmZone, ProjectionPolicy.REPROJECT_TO_DECLARED);
            return imported;
        }

        protected Map<Long, Double[]> parseTextToMap(String results, String[] headers) {
            String[] lines = results.split("\n");
            Map<Long, Double[]> resultMap = new HashMap<>();
            int transectColumn = -1;
            for (String line : lines) {
                String[] columns = line.split("\t");
                if (transectColumn < 0) {
                    // ignore the first line
                    for (int i=0; i<headers.length; i++) {
                        if (headers[i].equals(TRANSECT_ID)) {
                            transectColumn = i;
                        }
                    }
                    if (transectColumn < 0) {
                        throw new InputFileFormatException("Stats did not contain column named " + TRANSECT_ID);
                    }
                }
                else {
                    Long transectId = null;
                    Double[] values = new Double[columns.length-1];
                    int j = 0;
                    for (int i=0; i<columns.length; i++) {
                        if (i == transectColumn) {
                            String id = columns[i].replaceAll("\"", "");
                            transectId = Long.parseLong(id);
                        }
                        else {
                            try {
                                // may need to remove " here too
                                values[j] = Double.parseDouble(columns[i]);
                            } catch (NumberFormatException e) {
                                values[j] = Constants.SHAPEFILE_NODATA;
                            }
                            j++;
                        }
                    }
                    resultMap.put(transectId, values);
                }
            }
            return resultMap;
        }

        protected List<SimpleFeature> joinResultsToTransects(String[] columnHeaders, Map<Long, Double[]> resultMap, FeatureCollection<SimpleFeatureType, SimpleFeature> transects) {
                     
            SimpleFeatureType transectFeatureType = transects.getSchema();
            List<AttributeDescriptor> descriptors = transectFeatureType.getAttributeDescriptors();
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Results");
            builder.addAll(descriptors);
            for (String header: columnHeaders) {
                if (!header.equals(TRANSECT_ID)) {
                    builder.add(header, Double.class);
                }
            }
            builder.add("NSD", Double.class);
            Map<Integer, List<Point>> transectToIntersectMap = generateTransectToIntersectMap(intersects);

            
            SimpleFeatureType joinedFeatureType = builder.buildFeatureType();
            
            SortedMap<Double, List<Object>> distanceToAttribureMap = new TreeMap<>();
            try (FeatureIterator<SimpleFeature> features = transects.features()) {
                AttributeGetter getter = new AttributeGetter(joinedFeatureType);
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    Object transectIdAsObject = getter.getValue(Constants.TRANSECT_ID_ATTR, feature);
                    long transectId = ((Number)transectIdAsObject).longValue();
                    Double baseDistance = (Double)getter.getValue(Constants.BASELINE_DIST_ATTR, feature);
                    if (baseDistance == null) {
                        throw new UnsupportedFeatureTypeException("Transects must include base_dist attribute");
                    }
                    Double[] values = resultMap.get(transectId);
					if (values != null) {
						List<Object> joinedAttributes = new ArrayList<>(joinedFeatureType.getAttributeCount());
						joinedAttributes.addAll(feature.getAttributes());
						joinedAttributes.addAll(Arrays.asList(values));
						joinedAttributes.add(calculateNSD((Geometry) feature.getDefaultGeometry(), transectToIntersectMap.get(transectIdAsObject)));
						distanceToAttribureMap.put(baseDistance, joinedAttributes);
					}
                }
            }
            int joinedFeatureCount = distanceToAttribureMap.size();
            SequentialFeatureIDGenerator fidGenerator = new SequentialFeatureIDGenerator(joinedFeatureCount);
            List<SimpleFeature> joinedFeatureList = new ArrayList<>(distanceToAttribureMap.size()); 
            for (List<Object> attributes : distanceToAttribureMap.values()) {
                joinedFeatureList.add(SimpleFeatureBuilder.build(
                        joinedFeatureType,
                        attributes,
                        fidGenerator.next()));
            }
            
            return joinedFeatureList;
        }

        private String[] getColumnHeaders(String results) {
            String[] lines = results.split("\n");
            if (lines.length <= 1) {
                throw new InputFileFormatException("Results must have at least 2 rows");
            }
            String[] header = lines[0].split("\t");
            for (int i=0; i<header.length; i++) {
                header[i] = header[i].replaceAll("\"", "");
            }
            return header;
        }
    }
    
    private Map<Integer, List<Point>> generateTransectToIntersectMap(FeatureCollection<?, SimpleFeature> intersects) {
        Map<Integer, List<Point>> transectToIntersectionMap = new HashMap<>();
        FeatureIterator<SimpleFeature> intersectsIterator = null;
		try {
			intersectsIterator = intersects.features();
			while (intersectsIterator.hasNext()) {
				SimpleFeature feature = intersectsIterator.next();
				Object transectIdAsObject = feature.getAttribute(Constants.TRANSECT_ID_ATTR);
				if (transectIdAsObject instanceof Integer) {
					List<Point> pointList = transectToIntersectionMap.get((Integer)transectIdAsObject);
					if (pointList == null) {
						pointList = new ArrayList<>();
						transectToIntersectionMap.put((Integer)transectIdAsObject, pointList);
					}
					Object geometryAsObject = feature.getDefaultGeometry();
					if (geometryAsObject instanceof Point) {
						pointList.add((Point)geometryAsObject);
					} 
				}
			}
		} finally {
			if (null != intersectsIterator) {
				intersectsIterator.close();
			}
		}
        
        return transectToIntersectionMap;
    }
    
    private double calculateNSD(Geometry transect, List<? extends Geometry> intersects) {
        Coordinate b = transect.getGeometryN(0).getCoordinates()[0];
        double nsd = Double.MAX_VALUE;
        for (Geometry intersect : intersects) {
            Coordinate i = intersect.getGeometryN(0).getCoordinates()[0];
            double d = b.distance(i);
            if (d < nsd) {
                nsd = d;
            }
        }
        return nsd;
    }
    
    public static class SequentialFeatureIDGenerator {
        final String base;
        final int digits;
        final int count;
        int index = 0;
        public SequentialFeatureIDGenerator(int featureCount) {
            this.count = featureCount;
            this.base = SimpleFeatureBuilder.createDefaultFeatureId() + "-";
            this.digits = (int)Math.ceil(Math.log10(featureCount));
        }
        public String next() {
            if (index < count) {
                return base + StringUtils.leftPad(Integer.toString(index++), digits, '0');
            }
            throw new NoSuchElementException("FIDs have been exhausted for this generator: " + index++ + " < " + count);
        }
    }
}
