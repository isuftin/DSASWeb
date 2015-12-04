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
package gov.usgs.cida.dsas.wps.geom;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;


import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.dsas.exceptions.PoorlyDefinedBaselineException;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import gov.usgs.cida.dsas.utilities.features.Constants;
import static gov.usgs.cida.dsas.utilities.features.Constants.BIAS_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BIAS_UNCY_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.COMBINED_UNCY_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.DATE_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.DB_DATE_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.DEFAULT_BIAS;
import static gov.usgs.cida.dsas.utilities.features.Constants.DEFAULT_BIAS_UNCY;
import static gov.usgs.cida.dsas.utilities.features.Constants.DEFAULT_GEOM_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.DISTANCE_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.MHW_ATTR;
import gov.usgs.cida.dsas.utilities.features.Constants.Orientation;
import static gov.usgs.cida.dsas.utilities.features.Constants.TRANSECT_ID_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.UNCY_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.UNSHIFTED_DISTANCE_ATTR;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class Intersection {

	private Point point;
	private double distance;
	private int transectId;
	private DateTime date;
	private double uncy;
	private ProxyDatumBias pdb;
	private AttributeGetter attGet;
	private boolean isMeanHighWater = Constants.DEFAULT_MHW_VALUE;
	private static DateTimeFormatter inputFormat;
	private static DateTimeFormatter outputFormat;

	static {
		try {
			inputFormat = new DateTimeFormatterBuilder()
					.appendMonthOfYear(2)
					.appendLiteral('/')
					.appendDayOfMonth(2)
					.appendLiteral('/')
					.appendYear(4, 4)
					.toFormatter();

			outputFormat = new DateTimeFormatterBuilder()
					.appendYear(4, 4)
					.appendLiteral('-')
					.appendMonthOfYear(2)
					.appendLiteral('-')
					.appendDayOfMonth(2)
					.toFormatter();
		} catch (Exception ex) {
			// log severe
		}
	}

	/**
	 * Stores Intersections from feature for delivery to R
	 *
	 * @param point
	 * @param dist distance from reference (negative for seaward baselines)
	 * @param shoreline
	 * @param uncy
	 * @param bias
	 * @param transectId
	 * @param intersectionGetter
	 * @param shorelineGetter
	 */
	public Intersection(Point point, double dist, SimpleFeature shoreline, double uncy,
			int transectId, AttributeGetter intersectionGetter, AttributeGetter shorelineGetter) {
		this.point = point;
		this.distance = dist;
		this.transectId = transectId;
		this.attGet = intersectionGetter;
		//this has to support either shapefile or DB load
		if (shorelineGetter.getValue(DATE_ATTR, shoreline) != null) {
			this.date = parseDate(shorelineGetter.getValue(DATE_ATTR, shoreline));
		} else if (shorelineGetter.getValue(DB_DATE_ATTR, shoreline) != null) {
			this.date = parseDate(shorelineGetter.getValue(DB_DATE_ATTR, shoreline));
		}
		this.uncy = uncy;
		this.isMeanHighWater = shorelineGetter.getBooleanFromMhwAttribute(shoreline);
	}

	/**
	 * Get an intersection object from Intersection Feature Type
	 *
	 * @param intersectionFeature
	 * @param getter
	 */
	public Intersection(SimpleFeature intersectionFeature, AttributeGetter getter) {
		this.point = (Point) intersectionFeature.getDefaultGeometry();
		this.attGet = getter;
		this.transectId = (Integer) attGet.getValue(TRANSECT_ID_ATTR, intersectionFeature);
		this.distance = (Double) attGet.getValue(DISTANCE_ATTR, intersectionFeature);
		this.isMeanHighWater = attGet.getBooleanFromMhwAttribute(intersectionFeature);
		this.date = parseDate(attGet.getValue(DATE_ATTR, intersectionFeature));
		this.uncy = parseUncertainty(attGet.getValue(UNCY_ATTR, intersectionFeature));

		double biasVal;
		double uncybVal;

		try {
			biasVal = attGet.getDoubleValue(BIAS_ATTR, intersectionFeature);
		} catch (AttributeNotANumberException e) {
			biasVal = DEFAULT_BIAS;
		}

		try {
			uncybVal = attGet.getDoubleValue(BIAS_UNCY_ATTR, intersectionFeature);
		} catch (AttributeNotANumberException e) {
			uncybVal = DEFAULT_BIAS_UNCY;
		}

		this.pdb = new ProxyDatumBias(Double.NaN, biasVal, uncybVal);
	}

	public DateTime getDate() {
		return this.date;
	}

	public double getUncertainty() {
		return this.uncy;
	}

	public double getBias() {
		return this.pdb.getBias();
	}

	public void setBias(ProxyDatumBias inBias) {
		if (inBias != null && !isMeanHighWater) {
			this.pdb = inBias;
		} else {
			this.pdb = new ProxyDatumBias(Double.NaN, DEFAULT_BIAS, DEFAULT_BIAS_UNCY);
		}
	}

	public double getBiasUncertainty() {
		return this.pdb.getUncyb();
	}
	
	public double getShiftedDistance() {
		double shifted = distance;
		if (this.pdb != null) {
			shifted -= getBias();
		}
		return shifted;
	}
	
	public double getUnshiftedDistance() {
		return distance;
	}
	
	public double getCombinedUncy() {
		double combinedUncy = uncy;
		if (this.pdb != null) {
			double uncyb = getBiasUncertainty();
			combinedUncy = Math.sqrt(Math.pow(uncy, 2) + Math.pow(uncyb, 2));
		}
		return combinedUncy;
	}

	public static SimpleFeatureType buildSimpleFeatureType(SimpleFeatureCollection collection, CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

		builder.setName("Intersections");
		builder.add(DEFAULT_GEOM_ATTR, Point.class, crs);
		builder.add(TRANSECT_ID_ATTR, Integer.class);
		builder.add(DISTANCE_ATTR, Double.class);
		builder.add(UNSHIFTED_DISTANCE_ATTR, Double.class);
		builder.add(MHW_ATTR, Boolean.class);
		builder.add(DATE_ATTR, Date.class);
		builder.add(UNCY_ATTR, Double.class);
		builder.add(BIAS_ATTR, Double.class);
		builder.add(BIAS_UNCY_ATTR, Double.class);
		builder.add(COMBINED_UNCY_ATTR, Double.class);

		return builder.buildFeatureType();
	}

	public SimpleFeature createFeature(SimpleFeatureType type) {
		List<AttributeType> types = type.getTypes();
		Object[] featureObjectArr = new Object[types.size()];
		for (int i = 0; i < featureObjectArr.length; i++) {
			AttributeType attrType = types.get(i);
			if (attrType instanceof GeometryType) {
				featureObjectArr[i] = point;
			} else if (attGet.matches(attrType.getName(), TRANSECT_ID_ATTR)) {
				featureObjectArr[i] = (long) transectId;
			} else if (attGet.matches(attrType.getName(), DISTANCE_ATTR)) {
				featureObjectArr[i] = getShiftedDistance();
			} else if (attGet.matches(attrType.getName(), UNSHIFTED_DISTANCE_ATTR)) {
				featureObjectArr[i] = getUnshiftedDistance();
			} else if (attGet.matches(attrType.getName(), MHW_ATTR)) {
				featureObjectArr[i] = isMeanHighWater;
			} else if (attGet.matches(attrType.getName(), DATE_ATTR) || attGet.matches(attrType.getName(), DB_DATE_ATTR)) {
				featureObjectArr[i] = date.toDate();
			} else if (attGet.matches(attrType.getName(), UNCY_ATTR)) {
				featureObjectArr[i] = uncy;
			} else if (attGet.matches(attrType.getName(), BIAS_ATTR)) {
				featureObjectArr[i] = pdb.getBias();
			} else if (attGet.matches(attrType.getName(), BIAS_UNCY_ATTR)) {
				featureObjectArr[i] = pdb.getUncyb();
			} else if (attGet.matches(attrType.getName(), COMBINED_UNCY_ATTR)) {
				featureObjectArr[i] = getCombinedUncy();
			}
		}
		return SimpleFeatureBuilder.build(type, featureObjectArr, null);
	}

	private static DateTime parseDate(Object date) {
		if (date instanceof Date) {
			return new DateTime((Date) date);
		} else if (date instanceof String) {
			DateTime datetime = inputFormat.parseDateTime((String) date);
			return datetime;
		} else {
			throw new UnsupportedFeatureTypeException("Not sure what to do with date");
		}
	}

	public static double parseUncertainty(Object uncy) {
		if (uncy instanceof Number) {
			return ((Number) uncy).doubleValue();
		} else {
			throw new UnsupportedFeatureTypeException("Uncertainty should be a number");
		}
	}

	public int getTransectId() {
		return transectId;
	}

	/**
	 * Returns the desired intersection
	 *
	 * @param a first intersection
	 * @param b second intersection
	 * @param closest return the closest intersection (false for farthest)
	 * @return Intersection
	 */
	public static Intersection compare(Intersection a, Intersection b, boolean closest) {
		boolean aFarther = ((Math.abs(a.distance) - Math.abs(b.distance)) > 0);
		if (closest) {
			return (aFarther) ? b : a;
		} else {
			return (aFarther) ? a : b;
		}
	}

	public static double absoluteFarthest(double min, Collection<Intersection> intersections) {
		double maxVal = min;
		for (Intersection intersection : intersections) {
			double absDist = Math.abs(intersection.getShiftedDistance());
			if (absDist > maxVal) {
				maxVal = absDist;
			}
		}
		return maxVal;
	}

	public static Map<DateTime, Intersection> calculateIntersections(Transect transect, STRtree strTree, boolean useFarthest, AttributeGetter intersectionGetter) {
		Map<DateTime, Intersection> allIntersections = new HashMap<>();
		LineString line = transect.getLineString();
		AttributeGetter shorelineGetter = null;

		List<ShorelineFeature> possibleIntersects = strTree.query(line.getEnvelopeInternal());

		for (ShorelineFeature shoreline : possibleIntersects) {
			if (shorelineGetter == null) {
				// featuretype should be the same across all features
				shorelineGetter = new AttributeGetter(shoreline.feature1.getFeatureType());
			}
			LineString segment = shoreline.segment;
			if (segment.intersects(line)) {
				// must be a point
				Point crossPoint = (Point) segment.intersection(line);
				Orientation orientation = transect.getOrientation();

				int sign = orientation.getSign();
				if (sign == 0) {
					throw new PoorlyDefinedBaselineException("Baseline must define orientation");
				}

				double distance = sign
						* transect.getOriginCoord()
						.distance(crossPoint.getCoordinate());
				// use feature1 to get the date and MHW attribute (can't change within shoreline)
				double interpolatedUncy = shoreline.interpolate(crossPoint, UNCY_ATTR, shorelineGetter, 0.0d);
				Intersection intersection = new Intersection(crossPoint, distance, shoreline.feature1,
						interpolatedUncy, transect.getId(), intersectionGetter, shorelineGetter);
				DateTime date = intersection.getDate();
				if (allIntersections.containsKey(date)) {  // use closest/farthest intersection
					Intersection thatIntersection = allIntersections.get(date);
					Intersection closest = Intersection.compare(intersection, thatIntersection, !useFarthest);
					allIntersections.put(date, closest);
				} else {
					allIntersections.put(date, intersection);
				}
			}
		}
		return allIntersections;
	}

	/**
	 * Map is mutated to include new intersections in section of transect called
	 * here "subTransect"
	 *
	 * @param intersectionsSoFar
	 * @param origin
	 * @param subTransect
	 * @param strTree
	 * @param useFarthest
	 * @param getter
	 */
	public static void updateIntersectionsWithSubTransect(Map<DateTime, Intersection> intersectionsSoFar, Point origin,
			Transect subTransect, STRtree strTree, boolean useFarthest, AttributeGetter getter) {
		Map<DateTime, Intersection> intersectionSubset = calculateIntersections(subTransect, strTree, useFarthest, getter);
		for (DateTime date : intersectionSubset.keySet()) {
			Intersection intersection = intersectionSubset.get(date);

			int sign = subTransect.getOrientation().getSign();
			if (subTransect.getOrientation().getSign() == 0) {
				throw new PoorlyDefinedBaselineException("Baseline must define orientation");
			}

			intersection.distance = sign * intersection.point.distance(origin);
			if (intersectionsSoFar.containsKey(date)) {
				boolean isFarther = Math.abs(intersection.distance) > Math.abs(intersectionsSoFar.get(date).distance);
				// only true  && true
				// or   false && false
				if (useFarthest == isFarther) {
					intersectionsSoFar.put(date, intersection);
				}
			} else {
				intersectionsSoFar.put(date, intersection);
			}
		}
	}

	@Override
	public String toString() {
		String time = outputFormat.print(getDate());
		double shiftedDist = getShiftedDistance();
		double uncertainty = getCombinedUncy();
		double bias = getBias();
		double biasUncertainty = getBiasUncertainty();
		String str = time + "\t" + shiftedDist + "\t" + uncertainty + "\t" + bias + "\t" + biasUncertainty;
		return str;
	}

	public boolean isMeanHighWater() {
		return isMeanHighWater;
	}
}
