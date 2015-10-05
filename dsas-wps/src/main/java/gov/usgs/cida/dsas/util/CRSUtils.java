package gov.usgs.cida.dsas.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.utilities.features.AttributeGetter;
import gov.usgs.cida.utilities.features.Constants;

import static gov.usgs.cida.utilities.features.Constants.SEGMENT_ID_ATTR;
import static gov.usgs.cida.utilities.features.Constants.SHORELINE_ID_ATTR;

import java.util.LinkedList;
import java.util.List;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jiwalker
 */
public class CRSUtils {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CRSUtils.class);

	private static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));

	public static CoordinateReferenceSystem getCRSFromFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> simpleFeatureCollection) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> shorelineFeatureCollection = simpleFeatureCollection;
		SimpleFeatureType sft = shorelineFeatureCollection.getSchema();
		CoordinateReferenceSystem coordinateReferenceSystem = sft.getCoordinateReferenceSystem();
		return coordinateReferenceSystem;
	}

	/**
	 * Step through feature collection, get default geometries and transform
	 * Then build up a new MultiLine geometry and return
	 *
	 * @param featureCollection
	 * @return
	 */
	public static MultiLineString transformAndGetLinesFromFeatureCollection(
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
			CoordinateReferenceSystem sourceCrs,
			CoordinateReferenceSystem targetCrs) {
		SimpleFeatureCollection transformed = transformFeatureCollection(featureCollection, sourceCrs, targetCrs);
		return getLinesFromFeatureCollection(transformed);

	}

	/**
	 * Returns a SimpleFeatureCollection with transformed default geometry
	 *
	 * @param featureCollection source feature collection (features may be
	 * modified)
	 * @param sourceCrs original coordinate reference system
	 * @param targetCrs new coordinate reference system
	 * @return new SimpleFeatureCollection
	 */
	public static SimpleFeatureCollection transformFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
			CoordinateReferenceSystem sourceCrs,
			CoordinateReferenceSystem targetCrs) {
		List<SimpleFeature> sfList = new LinkedList<SimpleFeature>();
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
		} catch (FactoryException ex) {
			return null; // do something better than this
		}

		FeatureIterator<SimpleFeature> features = null;
		try {
			features = featureCollection.features();
			SimpleFeature feature = null;
			while (features.hasNext()) {
				feature = features.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				Geometry utmGeometry = null;
				try {
					utmGeometry = JTS.transform(geometry, transform);
				} catch (TransformException ex) {
					// TODO handle exceptions
					LOGGER.warn("Unhandled exception in transformFeatureCollection", ex);
				}
				feature.setDefaultGeometry(utmGeometry);
				sfList.add(feature);
			}
		} finally {
			if (null != features) {
				features.close();
			}
		}

		return DataUtilities.collection(sfList);
	}

	/**
	 * Since we are now supporting points, we need to join points together into
	 * lines as well as break multilines into lines
	 *
	 * @param collection feature collection to split up
	 * @return
	 */
	public static MultiLineString getLinesFromFeatureCollection(SimpleFeatureCollection collection) {
		List<LineString> lines = new LinkedList<>();

		FeatureIterator<SimpleFeature> features = null;
		try {
			if (collection == null) {
				throw new IllegalArgumentException("Must include feature collection to convert");
			}
			features = collection.features();

			SimpleFeature feature = null;
			while (features.hasNext()) {
				feature = features.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				Geometries geomType = Geometries.get(geometry);
				switch (geomType) {
					case LINESTRING:
					case MULTILINESTRING:
						List<LineString> separatedLines = breakLinesIntoLineList(feature);
						lines.addAll(separatedLines);
						break;
					case POINT:
					case MULTIPOINT:
						List<LineString> gatheredLines = gatherPointsIntoLineList(feature, features);
						lines.addAll(gatheredLines);
						break;
					case POLYGON:
					case MULTIPOLYGON:
						throw new UnsupportedFeatureTypeException(geomType.getSimpleName() + " features not supported");
					default:
						throw new UnsupportedFeatureTypeException("Only line type supported");
				}
			}
		} finally {
			if (null != features) {
				features.close();
			}
		}

		LineString[] linesArr = new LineString[lines.size()];
		lines.toArray(linesArr);
		return geometryFactory.createMultiLineString(linesArr);
	}

	public static MultiLineString getMultilineFromFeature(SimpleFeature feature) {
		List<LineString> lines = breakLinesIntoLineList(feature);
		LineString[] linesArr = new LineString[lines.size()];
		lines.toArray(linesArr);
		return geometryFactory.createMultiLineString(linesArr);
	}

	private static List<LineString> breakLinesIntoLineList(SimpleFeature feature) {
		List<LineString> lines = new LinkedList<>();
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Geometries geomType = Geometries.get(geometry);
		LineString lineString = null;
		switch (geomType) {
			case LINESTRING:
				lineString = (LineString) geometry;
				lines.add(lineString);
				break;
			case MULTILINESTRING:
				MultiLineString multiLineString = (MultiLineString) geometry;
				for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
					lineString = (LineString) multiLineString.getGeometryN(i);
					lines.add(lineString);
				}
				break;
			default:
				throw new IllegalStateException("Only line types should end up here");
		}
		return lines;
	}

	private static List<LineString> gatherPointsIntoLineList(SimpleFeature start, FeatureIterator<SimpleFeature> rest) {
		List<LineString> lines = new LinkedList<>();

		SimpleFeatureType featureType = start.getFeatureType();
		AttributeGetter getter = new AttributeGetter(featureType);

		SimpleFeature previous = null;
		SimpleFeature current = start;
		List<Coordinate> currentLine = new LinkedList<>();
		while (current != null) {
			Geometry geometry = (Geometry) current.getDefaultGeometry();
			Geometries geomType = Geometries.get(geometry);
			switch (geomType) {
				case LINESTRING:
				case MULTILINESTRING:
					// flush currentLine to list before starting new one
					if (currentLine.size() > 0) {
						lines.add(buildLineString(currentLine));
						currentLine = new LinkedList<>();
					}
					List<LineString> separatedLines = breakLinesIntoLineList(current);
					lines.addAll(separatedLines);
					break;
				case POINT:
					Point p = (Point) geometry;
					if (isNewLineSegment(previous, current, getter)) {
						//only create a line if 2 or more points exist
						if (currentLine.size() > 1) {
							lines.add(buildLineString(currentLine));
						} else {
							//DO nothing right now, signifies a single point segnment
							LOGGER.warn("Single point feature found and is being ignored, segment_id" + getter.getValue(Constants.SEGMENT_ID_ATTR, current));
						}
						currentLine = new LinkedList<>();
					}
					currentLine.add(p.getCoordinate());
					break;
				case MULTIPOINT:
					MultiPoint mp = (MultiPoint) geometry;
					if (isNewLineSegment(previous, current, getter)) {
						lines.add(buildLineString(currentLine));
						currentLine = new LinkedList<>();
					}
					for (int i = 0; i < mp.getNumPoints(); i++) {
						Point pointMember = (Point) mp.getGeometryN(i);
						currentLine.add(pointMember.getCoordinate());
					}
					break;
				case POLYGON:
				case MULTIPOLYGON:
					throw new UnsupportedFeatureTypeException(geomType.getSimpleName() + " features not supported");
				default:
					throw new UnsupportedFeatureTypeException("Only line type supported");
			}
			if (rest.hasNext()) {
				previous = current;
				current = rest.next();
			} else {
				if (currentLine.size() > 0) {
					lines.add(buildLineString(currentLine));
					currentLine = new LinkedList<>();
				}
				current = null;
			}
		}

		return lines;
	}

	private static LineString buildLineString(List<Coordinate> coords) {
		LineString line;
		try {
			CoordinateSequence seq = new CoordinateArraySequence(coords.toArray(new Coordinate[coords.size()]));
			line = new LineString(seq, geometryFactory);
		} catch (Exception e) {
			LOGGER.error("Failed to build line string from list of coordinates", e);
			line = null;
		}

		return line;
	}

	public static boolean isNewLineSegment(SimpleFeature first, SimpleFeature second, AttributeGetter getter) {
		boolean isNewSegment = false;
		try {
			if (first == null || second == null) {
				isNewSegment = true;
			} else if (getter.exists(SHORELINE_ID_ATTR) && getter.exists(SEGMENT_ID_ATTR)) {
				isNewSegment = !((getter.getIntValue(SHORELINE_ID_ATTR, first) == getter.getIntValue(SHORELINE_ID_ATTR, second))
					&& (getter.getIntValue(SEGMENT_ID_ATTR, first) == getter.getIntValue(SEGMENT_ID_ATTR, second)));
			} else if (getter.exists(SEGMENT_ID_ATTR)) {
				isNewSegment = !(getter.getIntValue(SEGMENT_ID_ATTR, first) == getter.getIntValue(SEGMENT_ID_ATTR, second));
			}
		} catch (AttributeNotANumberException e) {
			isNewSegment = true;
		}
		return isNewSegment;
	}
}
