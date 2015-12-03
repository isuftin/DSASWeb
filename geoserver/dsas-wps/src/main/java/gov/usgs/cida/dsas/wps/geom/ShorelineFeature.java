package gov.usgs.cida.dsas.wps.geom;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import org.opengis.feature.simple.SimpleFeature;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class ShorelineFeature {

	public LineString segment;
	public SimpleFeature feature1;
	public SimpleFeature feature2;

	public ShorelineFeature(LineString segment, SimpleFeature feature1, SimpleFeature feature2) {
		this.segment = segment;
		this.feature1 = feature1;
		this.feature2 = feature2;
	}
	
	public double interpolate(Point point, String attribute, AttributeGetter getter, double defaultVal) {
		double interpolatedValue;
		try {
			if (feature2 == null) {
				interpolatedValue = getter.getDoubleValue(attribute, feature1);
			} else {
				double d1 = point.distance(segment.getStartPoint());
				double d2 = point.distance(segment.getEndPoint());
				double length = segment.getLength();
				double val1 = getter.getDoubleValue(attribute, feature1);
				double val2 = getter.getDoubleValue(attribute, feature2);

				interpolatedValue = (d1/length*val2) + (d2/length*val1);
			}
		} catch (AttributeNotANumberException e) {
			interpolatedValue = defaultVal;
		}
		return interpolatedValue;
	}
}
