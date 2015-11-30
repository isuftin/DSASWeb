package gov.usgs.cida.dsas.util;

import gov.usgs.cida.dsas.util.CRSUtils;
import com.vividsolutions.jts.geom.MultiLineString;
import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcessTest;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.IOException;
import java.net.URL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class CRSUtilsTest {

    /**
     * Test of getLinesFromFeatureCollection method, of class CRSUtils.
     * need to fill this stuff out
     */
    @Test
    @Ignore
    public void testGetLinesFromFeatureCollection() throws IOException {
        URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_baseline.shp");
        FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
                FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
        MultiLineString expResult = null;
        MultiLineString result = CRSUtils.getLinesFromFeatureCollection((SimpleFeatureCollection)baselinefc);
        assertEquals(expResult, result);
    }
}
