package gov.usgs.cida.dsas.pdbc;

import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcess;
import gov.usgs.cida.dsas.wps.CreateTransectsAndIntersectionsProcessTest;
import gov.usgs.cida.dsas.wps.DummyCatalog;
import gov.usgs.cida.dsas.wps.DummyImportProcess;

import java.io.File;
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
public class TestPDBC {

	@Test
	public void testGeorgiaPDBCCorrection() throws Exception {
		File shpfile = File.createTempFile("test", ".shp");
		URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
				.getResource("gov/usgs/cida/coastalhazards/pdbc/GAbase_baseline.shp");
		URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
				.getResource("gov/usgs/cida/coastalhazards/pdbc/GA1971_1973.shp");
		URL biasRefShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
				.getResource("gov/usgs/cida/coastalhazards/Georgia_MHW_false_bias_test/GA_bias_new_bias.shp");
		FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc =
				FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
		FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc =
				FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
		FeatureCollection<SimpleFeatureType, SimpleFeature> biasReffc =
				FeatureCollectionFromShp.getFeatureCollectionFromShp(biasRefShapefile);
		CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(shpfile), new DummyCatalog());
		generate.execute((SimpleFeatureCollection)shorelinefc, (SimpleFeatureCollection)baselinefc, (SimpleFeatureCollection)biasReffc, 50.0d, 0d, Boolean.FALSE, null, null, null, null);
	}
	
}
