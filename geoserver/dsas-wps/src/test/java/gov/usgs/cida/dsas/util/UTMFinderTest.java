package gov.usgs.cida.dsas.util;

import gov.usgs.cida.dsas.util.UTMFinder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;

/**
 *
 * @author jordan
 */
public class UTMFinderTest {
    
    private SimpleFeatureType testFeatureType;
    private GeometryFactory gf = new GeometryFactory();
    
    @Before
    public void setUp() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Lines");
        builder.add("geom", LineString.class, WGS84);
        testFeatureType = builder.buildFeatureType();
    }
    
    @Test
    public void testWGS84AxisOrderAssumptions() {
        // This is an important test.  Axis ordering for WGS84 is ambigous.  Some implementations of
        // WGS84 present data as (lon,lat) while others are (lat,lon).  This test is to
        // versify our assumption of (lon,lat) for coordinate ordering is not an assumption.
        assertEquals("Verify coordinate value at ordinal 0 is longitude (axis order check)",
                UTMFinder.UTM_GCRS.getAxis(UTMFinder.UTM_GCRS_LON).getDirection(), AxisDirection.EAST);
        assertEquals("Verify coordinate value at ordinal 1 is longitude (axis order check)",
                UTMFinder.UTM_GCRS.getAxis(UTMFinder.UTM_GCRS_LAT).getDirection(), AxisDirection.NORTH);
    }

    /**
     * Test of findUTMZoneForFeatureCollection method, of class UTMFinder.
     */
    @Test
    public void testZone1N() throws Exception {
        
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(createLine(-177, 23, -177, 10));
        fc.add(createLine(-176, 13, -175, 33));
        fc.add(createLine(-177, 9, -176, 22));
        
        
        CoordinateReferenceSystem expResult = CRS.decode("EPSG:32601");
        CoordinateReferenceSystem result = UTMFinder.findUTMZoneCRSForCentroid(fc);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testZone31S() throws Exception {
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(createLine(3, -3, 3, -4));    
        
        CoordinateReferenceSystem expResult = CRS.decode("EPSG:32731");
        CoordinateReferenceSystem result = UTMFinder.findUTMZoneCRSForCentroid(fc);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testIncorrect() throws Exception {
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(createLine(3, -3, 3, -4));    
        
        CoordinateReferenceSystem expResult = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem result = UTMFinder.findUTMZoneCRSForCentroid(fc);
        assertFalse(expResult.equals(result));
    }
    
    private SimpleFeature createLine(double x1, double y1, double x2, double y2) {
        Coordinate[] coords = new Coordinate[] { new Coordinate(x1, y1), new Coordinate(x2, y2) };
        return SimpleFeatureBuilder.build(testFeatureType, new Object[] { gf.createLineString(coords) }, null);
    }
    
    @Test
    public void testUTMZoneCount() throws Exception {
 
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -164.001, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -164.00000, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -163.99999, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -158.00001, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -158.00000, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-170, -157.99999, 40, 50, WGS84)));
        
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(164.00001, 170, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(164.00000, 170, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(163.99999, 170, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(158.00001, 170, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(158.00000, 170, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(157.99999, 170, 40, 50, WGS84)));
        
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-3, 3, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-4, 4, 40, 50, WGS84)));
        assertEquals(2, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-5, 5, 40, 50, WGS84)));
        assertEquals(3, UTMFinder.findUTMZoneCRSCount(new ReferencedEnvelope(-6, 6, 40, 50, WGS84)));
    }
    


}
