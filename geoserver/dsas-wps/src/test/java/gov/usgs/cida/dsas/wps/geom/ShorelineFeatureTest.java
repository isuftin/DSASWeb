package gov.usgs.cida.dsas.wps.geom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import static gov.usgs.cida.dsas.utilities.features.Constants.UNCY_ATTR;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author jiwalker
 */
public class ShorelineFeatureTest {
	
	private static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
	private SimpleFeatureTypeBuilder simpleFeatureTypeBuilder;
	private SimpleFeatureType featureType;
	private SimpleFeatureBuilder simpleFeatureBuilder;
	
	public ShorelineFeatureTest() {
		simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("Test");
		simpleFeatureTypeBuilder.add(UNCY_ATTR, Double.class);
		featureType = simpleFeatureTypeBuilder.buildFeatureType();
		simpleFeatureBuilder = new SimpleFeatureBuilder(featureType);
	}

	/**
	 * Test of interpolate method, of class ShorelineFeature.
	 */
	@Test
	public void testInterpolateMiddle() {
		Point point = geometryFactory.createPoint(new Coordinate(0,0.5));
		String attribute = UNCY_ATTR;
		SimpleFeature f1 = simpleFeatureBuilder.buildFeature("1", new Double [] {1.0});
		SimpleFeature f2 = simpleFeatureBuilder.buildFeature("2", new Double [] {2.0});
		AttributeGetter getter = new AttributeGetter(featureType);
		LineString lineString = new LineString(new CoordinateArraySequence(
				new Coordinate[]{new Coordinate(0,0), new Coordinate(0,1)}), geometryFactory);
		ShorelineFeature instance = new ShorelineFeature(lineString, f1, f2);
		double expResult = 1.5;
		double result = instance.interpolate(point, attribute, getter, 0.0d);
		assertEquals(expResult, result, 0.01);
	}
	
	@Test //(at 0)
	public void testInterpolateZero() {
		Point point = geometryFactory.createPoint(new Coordinate(0,0));
		String attribute = UNCY_ATTR;
		SimpleFeature f1 = simpleFeatureBuilder.buildFeature("1", new Double [] {1.0});
		SimpleFeature f2 = simpleFeatureBuilder.buildFeature("2", new Double [] {2.0});
		AttributeGetter getter = new AttributeGetter(featureType);
		LineString lineString = new LineString(new CoordinateArraySequence(
				new Coordinate[]{new Coordinate(0,0), new Coordinate(0,1)}), geometryFactory);
		ShorelineFeature instance = new ShorelineFeature(lineString, f1, f2);
		double expResult = 1.0;
		double result = instance.interpolate(point, attribute, getter, 0.0d);
		assertEquals(expResult, result, 0.01);
	}
	
	@Test //(at 1)
	public void testInterpolateOne() {
		Point point = geometryFactory.createPoint(new Coordinate(0,1));
		String attribute = UNCY_ATTR;
		SimpleFeature f1 = simpleFeatureBuilder.buildFeature("1", new Double [] {1.0});
		SimpleFeature f2 = simpleFeatureBuilder.buildFeature("2", new Double [] {2.0});
		AttributeGetter getter = new AttributeGetter(featureType);
		LineString lineString = new LineString(new CoordinateArraySequence(
				new Coordinate[]{new Coordinate(0,0), new Coordinate(0,1)}), geometryFactory);
		ShorelineFeature instance = new ShorelineFeature(lineString, f1, f2);
		double expResult = 2.0;
		double result = instance.interpolate(point, attribute, getter, 0.0d);
		assertEquals(expResult, result, 0.01);
	}
	
	@Test //(at 1/4)
	public void testInterpolateQuarter() {
		Point point = geometryFactory.createPoint(new Coordinate(0,0.25));
		String attribute = UNCY_ATTR;
		SimpleFeature f1 = simpleFeatureBuilder.buildFeature("1", new Double [] {1.0});
		SimpleFeature f2 = simpleFeatureBuilder.buildFeature("2", new Double [] {2.0});
		AttributeGetter getter = new AttributeGetter(featureType);
		LineString lineString = new LineString(new CoordinateArraySequence(
				new Coordinate[]{new Coordinate(0,0), new Coordinate(0,1)}), geometryFactory);
		ShorelineFeature instance = new ShorelineFeature(lineString, f1, f2);
		double expResult = 1.25;
		double result = instance.interpolate(point, attribute, getter, 0.0d);
		assertEquals(expResult, result, 0.01);
	}
	
}
