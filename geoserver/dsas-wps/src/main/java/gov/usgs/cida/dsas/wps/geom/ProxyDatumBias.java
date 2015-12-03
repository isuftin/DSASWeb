package gov.usgs.cida.dsas.wps.geom;

import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import gov.usgs.cida.dsas.utilities.features.Constants;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class ProxyDatumBias {
	
	// Average slope preserved for later use
	private double avgSlope;
	private double bias;
	private double uncyb;

	public ProxyDatumBias(double averageSlope, double bias, double biasUncertainty) {
		this.avgSlope = averageSlope;
		this.bias = bias;
		this.uncyb = biasUncertainty;
	}

	public double getAvgSlope() {
		return avgSlope;
	}

	public double getBias() {
		return bias;
	}

	public double getUncyb() {
		return uncyb;
	}
	
	public static ProxyDatumBias fromFeature(SimpleFeature feature) {
		SimpleFeatureType featureType = feature.getFeatureType();
		AttributeGetter getter = new AttributeGetter(featureType);
		double slopeVal;
		double biasVal;
		double uncybVal;
		try {
			slopeVal = getter.getDoubleValue(Constants.AVG_SLOPE_ATTR, feature);
		} catch (AttributeNotANumberException e) {
			slopeVal = Double.NaN;
		}
		try {
			biasVal = getter.getDoubleValue(Constants.BIAS_ATTR, feature);
		} catch (AttributeNotANumberException e) {
			biasVal = Double.NaN;
		}
		try {
			uncybVal = getter.getDoubleValue(Constants.BIAS_UNCY_ATTR, feature);
		} catch (AttributeNotANumberException e) {
			uncybVal = Double.NaN;
		}
		
		return new ProxyDatumBias(slopeVal, biasVal, uncybVal);
	}

}
