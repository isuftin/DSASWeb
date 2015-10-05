package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcess;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import gov.usgs.cida.dsas.util.BaselineDistanceAccumulator;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author jiwalker
 */
public class CreateTransectsAndIntersectionsProcessTest {
    
    private final static double EPS = 1e-15;
    
    private GeometryFactory gf;
    
    @Before
    public void setup() {
        gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
    }
    
    /**
     * Test of execute method, of class CreateTransectsAndIntersectionsProcess.
     */
    @Test
    public void testRotateSegment() throws Exception {
        Coordinate a = new Coordinate(-1, -1);
        Coordinate b = new Coordinate(1, 1);
        LineSegment ls = new LineSegment(a, b);
        double angle = ls.angle();
        double rotated = angle + Angle.PI_OVER_2;
        double rise = 100 * Math.sin(rotated);
        double run = 100 * Math.cos(rotated);
        
        System.out.println("x: " + run + " y: " + rise);
    }
    
    @Test
    public void testPreparedWithin() {
        GeometryFactory factory = new GeometryFactory();
        Point a = factory.createPoint(new Coordinate(1,1));
        Point b = factory.createPoint(new Coordinate(2,2));
        LineString line1 = factory.createLineString(new Coordinate[] {new Coordinate(0,0), new Coordinate(3,3)});
        LineString line2 = factory.createLineString(new Coordinate[] {new Coordinate(1.5,1.5), new Coordinate(3,3)});
        Point[] points = new Point[] { a, b };
        MultiPoint createMultiPoint = factory.createMultiPoint(points);
        PreparedGeometry prep = PreparedGeometryFactory.prepare(createMultiPoint);
        assertTrue(prep.within(line1));
        assertFalse(prep.within(line2));
    }
    
    @Test
    @Ignore
    public void testExecute() throws Exception {
        URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_baseline.shp");
        URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_shorelines.shp");
        FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(), new DummyCatalog());
        generate.execute((SimpleFeatureCollection)shorelinefc, (SimpleFeatureCollection)baselinefc, null, 50.0d, 0d, Boolean.FALSE, null, null, null, null);
    }
    
    /*
     * Ignoring this because it is really just to get the shp for testing
     */
    @Test
    @Ignore
    public void testExecuteAndWriteToFile() throws Exception {
        File shpfile = File.createTempFile("test", ".shp");
        URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_baseline.shp");
        URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_shorelines.shp");
        SimpleFeatureCollection baselinefc = (SimpleFeatureCollection)
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        SimpleFeatureCollection shorelinefc = (SimpleFeatureCollection)
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(shpfile), new DummyCatalog());
        generate.execute((SimpleFeatureCollection)shorelinefc, (SimpleFeatureCollection)baselinefc, null, 50.0d, 0d, Boolean.FALSE, null, null, null, null);
    }
    
        
    @Test
    public void testToLineSegments() {
        LineString lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 1),
            new Coordinate(2, 3),
            new Coordinate(4, 5),
            new Coordinate(6, 7),
            new Coordinate(7, 8),
        });
        List<LineSegment> lineSegments = CreateTransectsAndIntersectionsProcess.toLineSegments(lineString);
        
        assertNotNull(lineSegments);
        assertEquals(lineString.getNumPoints() -1, lineSegments.size());
        
        assertEquals(lineString.getCoordinateN(0), lineSegments.get(0).getCoordinate(0));
        assertEquals(lineString.getCoordinateN(1), lineSegments.get(0).getCoordinate(1));
        
        assertEquals(lineString.getCoordinateN(1), lineSegments.get(1).getCoordinate(0));
        assertEquals(lineString.getCoordinateN(2), lineSegments.get(1).getCoordinate(1));
        
        assertEquals(lineString.getCoordinateN(2), lineSegments.get(2).getCoordinate(0));
        assertEquals(lineString.getCoordinateN(3), lineSegments.get(2).getCoordinate(1));
        
        assertEquals(lineString.getCoordinateN(3), lineSegments.get(3).getCoordinate(0));
        assertEquals(lineString.getCoordinateN(4), lineSegments.get(3).getCoordinate(1));
        
    }
    
    @Test
    public void testFindIntervals_IntervalEqualToCoordSpacing_WithOrigin() {
        
        LineString lineString;
        List<LineSegment> lineSegments;
        
        // just a simple line incrementing by one on y axis
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(0, 1),
            new Coordinate(0, 2),
            new Coordinate(0, 3),
            new Coordinate(0, 4),
        });
        
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, true, 1);
        
        assertNotNull(lineSegments);
        assertEquals(lineString.getNumPoints(), lineSegments.size());
        
        assertEquals(lineString.getCoordinateN(0).x, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(0).y, lineSegments.get(0).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(1).x, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(1).y, lineSegments.get(0).getCoordinate(1).y, EPS);
        
        assertEquals(lineString.getCoordinateN(1).x, lineSegments.get(1).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(1).y, lineSegments.get(1).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(2).x, lineSegments.get(1).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(2).y, lineSegments.get(1).getCoordinate(1).y, EPS);
        
        assertEquals(lineString.getCoordinateN(2).x, lineSegments.get(2).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(2).y, lineSegments.get(2).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(3).x, lineSegments.get(2).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(3).y, lineSegments.get(2).getCoordinate(1).y, EPS);
        
        assertEquals(lineString.getCoordinateN(3).x, lineSegments.get(3).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(3).y, lineSegments.get(3).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(4).x, lineSegments.get(3).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(4).y, lineSegments.get(3).getCoordinate(1).y, EPS);
    }
    
    @Test
    public void testFindIntervals_IntervalEqualToCoordSpacing_WithoutOrigin() {
        
        LineString lineString;
        List<LineSegment> lineSegments;
        
        // just a simple line incrementing by one on y axis
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0),
            new Coordinate(3, 0),
            new Coordinate(4, 0),
        });
        
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, false, 1);
        
        assertNotNull(lineSegments);
        assertEquals(lineString.getNumPoints() - 1, lineSegments.size());
        
        assertEquals(lineString.getCoordinateN(1).x, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(1).y, lineSegments.get(0).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(2).x, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(2).y, lineSegments.get(0).getCoordinate(1).y, EPS);
        
        assertEquals(lineString.getCoordinateN(2).x, lineSegments.get(1).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(2).y, lineSegments.get(1).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(3).x, lineSegments.get(1).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(3).y, lineSegments.get(1).getCoordinate(1).y, EPS);
        
        assertEquals(lineString.getCoordinateN(3).x, lineSegments.get(2).getCoordinate(0).x, EPS);
        assertEquals(lineString.getCoordinateN(3).y, lineSegments.get(2).getCoordinate(0).y, EPS);
        // coincidental, not by algorithm design 
        assertEquals(lineString.getCoordinateN(4).x, lineSegments.get(2).getCoordinate(1).x, EPS);
        assertEquals(lineString.getCoordinateN(4).y, lineSegments.get(2).getCoordinate(1).y, EPS);
    }
        
    
    @Test
    public void testFindIntervals_IntervalLessThanCoordSpacing_WithOrigin() {
    
        LineString lineString;
        List<LineSegment> lineSegments;
        
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(0, 10),
            new Coordinate(0, 20)
        });
        
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, true, 1);
        
        assertNotNull(lineSegments);
        assertEquals(21, lineSegments.size());
        
        assertEquals(0, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(0).y, EPS);
        // (0,0) + (0,1) 
        assertEquals(0, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(1, lineSegments.get(0).getCoordinate(1).y, EPS);
       
        assertEquals(0, lineSegments.get(10).getCoordinate(0).x, EPS);
        assertEquals(10, lineSegments.get(10).getCoordinate(0).y, EPS);
        // (0,10) + (0,1) 
        assertEquals(0, lineSegments.get(10).getCoordinate(1).x, EPS);
        assertEquals(11, lineSegments.get(10).getCoordinate(1).y, EPS);
        
        assertEquals(0, lineSegments.get(20).getCoordinate(0).x, EPS);
        assertEquals(20, lineSegments.get(20).getCoordinate(0).y, EPS);
        // (0,20) + (0,1) 
        assertEquals(0, lineSegments.get(20).getCoordinate(1).x, EPS);
        assertEquals(21, lineSegments.get(20).getCoordinate(1).y, EPS);
    }
    
    @Test
    public void testFindIntervals_IntervalLessThanCoordSpacing_WithoutOrigin() {
    
        LineString lineString;
        List<LineSegment> lineSegments;
        
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(10, 0),
            new Coordinate(20, 0)
        });
                
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, false, 1);
        
        assertNotNull(lineSegments);
        assertEquals(20, lineSegments.size());
        
        assertEquals(1, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(0).y, EPS);
        // (1,0) + (1,0) 
        assertEquals(2, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(1).y, EPS);
       
        assertEquals(11, lineSegments.get(10).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(10).getCoordinate(0).y, EPS);
        // (10,0) + (1,0) 
        assertEquals(12, lineSegments.get(10).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(10).getCoordinate(1).y, EPS);
        
        assertEquals(20, lineSegments.get(19).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(19).getCoordinate(0).y, EPS);
        // (20,0) + (1,0) 
        assertEquals(21, lineSegments.get(19).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(19).getCoordinate(1).y, EPS);   
    }
    
    @Test
    public void testFindIntervals_IntervalGreaterThanCoordSpacing_WithOrigin() {
    
        LineString lineString;
        List<LineSegment> lineSegments;
        
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(0, 1),
            new Coordinate(0, 2),
            new Coordinate(0, 3),
            new Coordinate(0, 4),
            new Coordinate(0, 5),
            new Coordinate(0, 6),
            new Coordinate(0, 7),
            new Coordinate(0, 8),
            new Coordinate(0, 9),
        });
        
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, true, 2.5);
        
        assertNotNull(lineSegments);
        assertEquals(4, lineSegments.size());
        
        assertEquals(0, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(0).y, EPS);
        // (0,0) + (0,1) 
        assertEquals(0, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(1, lineSegments.get(0).getCoordinate(1).y, EPS);
       
        assertEquals(0, lineSegments.get(1).getCoordinate(0).x, EPS);
        assertEquals(2.5, lineSegments.get(1).getCoordinate(0).y, EPS);
        // (0,2.5) + (0,1) 
        assertEquals(0, lineSegments.get(1).getCoordinate(1).x, EPS);
        assertEquals(3.5, lineSegments.get(1).getCoordinate(1).y, EPS);
       
        assertEquals(0, lineSegments.get(2).getCoordinate(0).x, EPS);
        assertEquals(5, lineSegments.get(2).getCoordinate(0).y, EPS);
        // (0,5) + (0,1) 
        assertEquals(0, lineSegments.get(2).getCoordinate(1).x, EPS);
        assertEquals(6, lineSegments.get(2).getCoordinate(1).y, EPS);
       
        assertEquals(0, lineSegments.get(3).getCoordinate(0).x, EPS);
        assertEquals(7.5, lineSegments.get(3).getCoordinate(0).y, EPS);
        // (0,7.5) + (0,1) 
        assertEquals(0, lineSegments.get(3).getCoordinate(1).x, EPS);
        assertEquals(8.5, lineSegments.get(3).getCoordinate(1).y, EPS);
    }
    
    @Test
    public void testFindIntervals_IntervalGreaterThanCoordSpacing_WithoutOrigin() {
    
        LineString lineString;
        List<LineSegment> lineSegments;
        
        lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0),
            new Coordinate(3, 0),
            new Coordinate(4, 0),
            new Coordinate(5, 0),
            new Coordinate(6, 0),
            new Coordinate(7, 0),
            new Coordinate(8, 0),
            new Coordinate(9, 0),
        });        
        
        lineSegments = CreateTransectsAndIntersectionsProcess.findIntervals(lineString, false, 2.5);
        
        assertNotNull(lineSegments);
        assertEquals(3, lineSegments.size());
       
        assertEquals(2.5, lineSegments.get(0).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(0).y, EPS);
        // (2.5,0) + (1,0) 
        assertEquals(3.5, lineSegments.get(0).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(0).getCoordinate(1).y, EPS);
       
        assertEquals(5, lineSegments.get(1).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(1).getCoordinate(0).y, EPS);
        // (5,0) + (1,0) 
        assertEquals(6, lineSegments.get(1).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(1).getCoordinate(1).y, EPS);
       
        assertEquals(7.5, lineSegments.get(2).getCoordinate(0).x, EPS);
        assertEquals(0, lineSegments.get(2).getCoordinate(0).y, EPS);
        // (7.5,0) + (1,0) 
        assertEquals(8.5, lineSegments.get(2).getCoordinate(1).x, EPS);
        assertEquals(0, lineSegments.get(2).getCoordinate(1).y, EPS);   
    }
    
}
