package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.File;
import java.net.URL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;

import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author jiwalker
 */
public class CreateTransectsAndIntersectionsProcessTest {

	@Test
	@Ignore
	public void testExecute() throws Exception {
		URL baselineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
				.getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_baseline.shp");
		URL shorelineShapefile = CreateTransectsAndIntersectionsProcessTest.class.getClassLoader()
				.getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_shorelines.shp");
		FeatureCollection<SimpleFeatureType, SimpleFeature> baselinefc
				= FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
		FeatureCollection<SimpleFeatureType, SimpleFeature> shorelinefc
				= FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
		CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(), new DummyCatalog());
		generate.execute((SimpleFeatureCollection) shorelinefc, (SimpleFeatureCollection) baselinefc, null, 50.0d, 0d, null, Boolean.FALSE, null, null, null, null);
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
		SimpleFeatureCollection baselinefc = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(baselineShapefile);
		SimpleFeatureCollection shorelinefc = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(shorelineShapefile);
		CreateTransectsAndIntersectionsProcess generate = new CreateTransectsAndIntersectionsProcess(new DummyImportProcess(shpfile), new DummyCatalog());
		generate.execute((SimpleFeatureCollection) shorelinefc, (SimpleFeatureCollection) baselinefc, null, 50.0d, 0d, null, Boolean.FALSE, null, null, null, null);
	}

}
