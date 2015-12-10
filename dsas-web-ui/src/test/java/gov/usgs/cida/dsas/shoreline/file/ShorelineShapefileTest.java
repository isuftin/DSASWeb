package gov.usgs.cida.dsas.shoreline.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author isuftin
 */
public class ShorelineShapefileTest {

	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static File validShapeZip;
	private static File noPRJShapeZip;
	private static File gaMHWFalseShorelines;

	public ShorelineShapefileTest() {
	}

	@BeforeClass
	public static void setUpClass() throws IOException {
		workDir = new File(tempDir, String.valueOf(new Date().getTime()));
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);
	}

	@AfterClass
	public static void tearDownClass() {
		FileUtils.deleteQuietly(workDir);
	}

	@Before
	public void setUp() throws URISyntaxException, IOException {
		String packagePath = "/";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		validShapeZip = new File(workDir, "valid_shapezip.zip");
		noPRJShapeZip = new File(workDir, "no_prj_shapefile.zip");
		gaMHWFalseShorelines = new File(workDir, "Georgia_MHW_false_shorelines.zip");
	}

	@After
	public void tearDown() {
		for (File file : FileUtils.listFiles(workDir, null, true)) {
			FileUtils.deleteQuietly(file);
		}
	}

	@Test
	public void testValidate() throws Exception {
		System.out.println("testValidate");
		File zipFile = validShapeZip;
		File tempD = null;
		File tempShapeFile = null;

		tempD = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempD.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(validShapeZip, tempShapeFile);
		FileHelper.unzipFile(tempD.getAbsolutePath(), validShapeZip);
		
		ShorelineShapefile.validate(tempD);
		assertTrue("Validated without exception", true);
		
		File tempDir = null;
		File tempShapeFile2 = null;

		tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempD.deleteOnExit();
		tempShapeFile2 = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile2.deleteOnExit();
		FileUtils.copyFile(gaMHWFalseShorelines, tempShapeFile2);
		FileHelper.unzipFile(tempDir.getAbsolutePath(), gaMHWFalseShorelines);
		
		ShorelineShapefile.validate(tempDir);
		assertTrue("Validated without exception", true);
		
	}

	@Test(expected = ShorelineFileFormatException.class)
	public void testValidateWithInvalidFile() throws Exception {
		System.out.println("testValidateWithInvalidFile");
	
		File tempDirect = null;
		File tempShapeFile = null;

		tempDirect = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDirect.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(noPRJShapeZip, tempShapeFile);
		FileHelper.unzipFile(tempDirect.getAbsolutePath(), noPRJShapeZip);
		
		ShorelineShapefile.validate(tempDirect);
		//assertTrue("Validated without exception", true);
	}

}
