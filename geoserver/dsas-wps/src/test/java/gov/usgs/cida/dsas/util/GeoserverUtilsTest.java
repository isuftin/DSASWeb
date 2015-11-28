package gov.usgs.cida.dsas.util;

import gov.usgs.cida.dsas.util.LayerImportUtil;
import gov.usgs.cida.dsas.util.GeoserverUtils;
import gov.usgs.cida.dsas.wps.AutoImportProcess;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author isuftin
 */
public class GeoserverUtilsTest extends WPSTestSupport {

	private AutoImportProcess autoImportProcess;
	private LayerImportUtil importer;
	public static final String WORKSPACE_NAME = "gs";
	public static final String LAYER_NAME = "mixedCaseColumnNames";
	public static final String STORE_NAME = "myStoreName";
	private static File workDir;
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File mixedCaseShapefile;

	@BeforeClass
	public static void setupAll() throws IOException {
		workDir = new File(tempDir, String.valueOf(new Date().getTime()));
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);
	}

	@AfterClass
	public static void tearDownClass() {
		FileUtils.deleteQuietly(workDir);
	}
	
	@Before
	public void setupTest() throws URISyntaxException, IOException {
		String packagePath = "gov/usgs/cida/coastalhazards/mixedCaseColumnNames/";
		FileUtils.copyDirectory(new File(getClass().getResource("/").toURI()), workDir);
		mixedCaseShapefile = new File(workDir, packagePath + "mixedCaseColumnNames.shp");
		autoImportProcess = new AutoImportProcess(catalog);
		importer = new LayerImportUtil(catalog, autoImportProcess);
	}

	@After
	public void tearDown() {
		for (File file : FileUtils.listFiles(workDir, null, true)) {
			FileUtils.deleteQuietly(file);
		}
	}

	@Test
	public void testReplaceLayerWhenImportFails() throws IOException, URISyntaxException {
		System.out.println("testReplaceLayerWhenImportFails");
		
		SimpleFeatureCollection mixedCaseFeatureCollection = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(mixedCaseShapefile.toURI().toURL());
		String layer = importer.importLayer(mixedCaseFeatureCollection, WORKSPACE_NAME, STORE_NAME, LAYER_NAME, mixedCaseFeatureCollection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem(), ProjectionPolicy.REPROJECT_TO_DECLARED);
		layer = layer.split(":")[1];
		
		ImportProcess importProc = new MisbehavingImportProcess(catalog);
		GeoserverUtils instance = new GeoserverUtils(catalog);
		WorkspaceInfo workspaceInfo = instance.getWorkspaceByName(WORKSPACE_NAME);
		DataStoreInfo datastoreInfo = instance.getDataStoreByName(WORKSPACE_NAME, STORE_NAME);
		
		Collection<File> initialFiles = FileUtils.listFiles(new File(new File(mixedCaseShapefile.toURI()).getParent()), new PrefixFileFilter(layer), null);
		int initialFileCount = initialFiles.size();
		try {
			instance.replaceLayer(mixedCaseFeatureCollection, layer, datastoreInfo, workspaceInfo, importProc);
			fail("Expected exception not generated");
		} catch (Exception e) {
			assertEquals(e.getClass(), org.geotools.process.ProcessException.class);
			
			// Make sure that the files that were moved are replaced
			Collection<File> postErrorFiles = FileUtils.listFiles(new File(new File(mixedCaseShapefile.toURI()).getParent()), new PrefixFileFilter(layer), null);
			assertEquals(initialFileCount, postErrorFiles.size());
		}
	}

	private class MisbehavingImportProcess extends ImportProcess {

		public MisbehavingImportProcess(Catalog catalog) {
			super(catalog);
		}

		@Override
		public String execute(SimpleFeatureCollection features, GridCoverage2D coverage, String workspace, String store, String name, CoordinateReferenceSystem srs, ProjectionPolicy srsHandling, String styleName) throws ProcessException {
			throw new ProcessException("I've been a bad, bad import process");
		}
	}

}
