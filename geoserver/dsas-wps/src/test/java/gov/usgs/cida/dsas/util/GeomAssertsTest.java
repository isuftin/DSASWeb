package gov.usgs.cida.dsas.util;

import gov.usgs.cida.dsas.util.CRSUtils;
import gov.usgs.cida.dsas.util.GeomAsserts;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcessTest;
import gov.usgs.cida.dsas.exceptions.PoorlyDefinedBaselineException;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.IOException;
import java.net.URL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class GeomAssertsTest {

    /**
     * Test of assertBaselinesDoNotCrossShorelines method, of class GeomAsserts.
     */
    @Test
    public void testAssertBaselinesDoNotCrossShorelines_doesntcross() throws IOException {
        URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_baseline.shp");
        URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_shorelines.shp");
        FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        MultiLineString shorelineGeom = CRSUtils.getLinesFromFeatureCollection((SimpleFeatureCollection)shorelinefc);
        PreparedGeometry shorelines = PreparedGeometryFactory.prepare(shorelineGeom);
        MultiLineString baselines = CRSUtils.getLinesFromFeatureCollection((SimpleFeatureCollection)baselinefc);;
        GeomAsserts.assertBaselinesDoNotCrossShorelines(shorelines, baselines);
    }
    
    @Test(expected=PoorlyDefinedBaselineException.class)
    public void testAssertBaselinesDoNotCrossShorelines_doescross() throws IOException {
        URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_cross.shp");
        URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_shorelines.shp");
        FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
        MultiLineString shorelineGeom = CRSUtils.getLinesFromFeatureCollection((SimpleFeatureCollection)shorelinefc);
        PreparedGeometry shorelines = PreparedGeometryFactory.prepare(shorelineGeom);
        MultiLineString baselines = CRSUtils.getLinesFromFeatureCollection((SimpleFeatureCollection)baselinefc);;
        GeomAsserts.assertBaselinesDoNotCrossShorelines(shorelines, baselines);
    }
}
