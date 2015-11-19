package gov.usgs.cida.dsas.hawaii;

import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcess;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import gov.usgs.cida.dsas.wps.DummyCatalog;
import gov.usgs.cida.dsas.wps.DummyImportProcess;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.File;
import java.net.URL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author jiwalker
 */
public class TestFailingHawaiiTransects {
	
	private SimpleFeatureCollection biasfc = FeatureCollectionFromShp.getEmptyFeatureCollection();
    
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
        URL baselineShapefile = TestFailingHawaiiTransects.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/hawaii/KauaiE_baseline.shp");
        URL shorelineShapefile = TestFailingHawaiiTransects.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/hawaii/KauaiE_shorelines.shp");
        FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        
        CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(), new DummyCatalog());
        generate.execute((SimpleFeatureCollection)shorelinefc, (SimpleFeatureCollection)baselinefc, biasfc, 100.0d, 0d, null, Boolean.FALSE, null, null, null, null);
    }
    
    /*
     * Ignoring this because it is really just to get the shp for testing
     */
    @Test
    @Ignore
    public void testExecuteAndWriteToFile() throws Exception {
        File shpfile = File.createTempFile("test", ".shp");
        URL baselineShapefile = TestFailingHawaiiTransects.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/hawaii/KauaiE_baseline.shp");
        URL shorelineShapefile = TestFailingHawaiiTransects.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/hawaii/KauaiE_shorelines.shp");
        SimpleFeatureCollection baselinefc = (SimpleFeatureCollection)
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        SimpleFeatureCollection shorelinefc = (SimpleFeatureCollection)
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(shpfile), new DummyCatalog());
        generate.execute(shorelinefc, baselinefc, biasfc, 100.0d, 0d, null, Boolean.FALSE, null, null, null, null);
    }
}
