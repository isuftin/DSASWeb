package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.dsas.wps.NormalizeLayerColumnNamesProcess;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.dsas.util.LayerImportUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

public class NormalizeLayerColumnNamesProcessTest extends WPSTestSupport {

	private static File outTest = null;

	public static final String WORKSPACE_NAME = "gs";
	public static final String LAYER_NAME = "myLayerName";
	public static final String STORE_NAME = "myStoreName";

	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static File mixedCaseShapefile;
	private static File mixedCaseColumnNamesZeroLengthPRJ;

	@BeforeClass
	public static void setupAll() throws IOException {
		workDir = new File(tempDir, String.valueOf(new Date().getTime()));
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);

		// leaks files, need to delete all the files associated with .shp
		outTest = File.createTempFile("test", ".shp");
		outTest.deleteOnExit();
	}

	@AfterClass
	public static void tearDownClass() {
		FileUtils.deleteQuietly(workDir);
	}
	private ImportProcess dummyImportProcess;
	private AutoImportProcess autoImportProcess;
	private LayerImportUtil importer;

	@Before
	public void setupTest() throws URISyntaxException, IOException {
		String packagePath = "gov/usgs/cida/coastalhazards/mixedCaseColumnNames/";
		FileUtils.copyDirectory(new File(getClass().getResource("/").toURI()), workDir);
		mixedCaseShapefile = new File(workDir, packagePath + "mixedCaseColumnNames.shp");
		mixedCaseColumnNamesZeroLengthPRJ = new File(workDir, packagePath + "mixedCaseColumnNamesZeroLengthPRJ.shp");
		dummyImportProcess = new DummyImportProcess(outTest);
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
	public void testExecute() throws Exception {
		System.out.println("testExecute");

		//inspiration: http://www.torres.at/geoserver-create-datastore-programmatically/
		SimpleFeatureCollection mixedCaseFeatureCollection = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(mixedCaseShapefile.toURI().toURL());
		String response = importer.importLayer(mixedCaseFeatureCollection, WORKSPACE_NAME, STORE_NAME, LAYER_NAME, mixedCaseFeatureCollection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem(), ProjectionPolicy.REPROJECT_TO_DECLARED);
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, catalog);
		String normalizeResult = normalizeLayerColumnNamesProcess.execute(response);
		System.out.println("The following renaming was attempted (oldName|newName):");
		System.out.println(normalizeResult);
		validateColumnNames(outTest);
	}

	// The exception here is actually thrown by the AutoImportProcess, but I also have a check for this in the NormalizeLayerColumnProcess
	@Test(expected = org.geotools.process.ProcessException.class)
	public void testExecuteWithZeroLengthPRJ() throws Exception {
		System.out.println("testExecuteWithZeroLengthPRJ");
		SimpleFeatureCollection mixedCaseFeatureCollection = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(mixedCaseColumnNamesZeroLengthPRJ.toURI().toURL());
		String response = importer.importLayer(mixedCaseFeatureCollection, WORKSPACE_NAME, STORE_NAME, LAYER_NAME, mixedCaseFeatureCollection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem(), ProjectionPolicy.REPROJECT_TO_DECLARED);
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, catalog);
		normalizeLayerColumnNamesProcess.execute(response);
	}

	@Test(expected = org.geotools.process.ProcessException.class)
	public void testExecuteWithMissingLayer() throws Exception {
		System.out.println("testExecuteWithMissingLayer");
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, catalog);
		normalizeLayerColumnNamesProcess.execute("test:test");
	}

	@Test(expected = java.lang.NullPointerException.class)
	public void testExecuteWithNullCatalog() throws Exception {
		System.out.println("testExecuteWithNullCatalog");
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, null);
		normalizeLayerColumnNamesProcess.execute("test:test");
	}

	@Test(expected = org.geotools.process.ProcessException.class)
	public void testExecuteWithBlankPrefixedLayerName() throws Exception {
		System.out.println("testExecuteWithBlankPrefixedLayerName");
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, catalog);
		normalizeLayerColumnNamesProcess.execute("");
	}

	@Test(expected = org.geotools.process.ProcessException.class)
	public void testExecuteWithNullPrefixedLayerName() throws Exception {
		System.out.println("testExecuteWithNullPrefixedLayerName");
		NormalizeLayerColumnNamesProcess normalizeLayerColumnNamesProcess = new NormalizeLayerColumnNamesProcess(dummyImportProcess, catalog);
		normalizeLayerColumnNamesProcess.execute(null);
	}

	private void validateColumnNames(File shapefile) throws MalformedURLException, IOException {
		SimpleFeatureCollection resultsFC = (SimpleFeatureCollection) FeatureCollectionFromShp.getFeatureCollectionFromShp(shapefile.toURI().toURL());

		SimpleFeatureType resultsFeatureType = resultsFC.getSchema();
		List<AttributeDescriptor> attributeDescriptors = resultsFeatureType.getAttributeDescriptors();
		assertFalse(attributeDescriptors.isEmpty());
		for (AttributeDescriptor ad : attributeDescriptors) {
			Name attributeName = ad.getName();
			String actualName = attributeName.toString();
			if (!NormalizeLayerColumnNamesProcess.COLUMN_NAMES_TO_IGNORE.contains(actualName)) {
				String upperCasedName = actualName.toUpperCase(Locale.ENGLISH);
				assertEquals(actualName, upperCasedName);
			}
		}
	}
}
