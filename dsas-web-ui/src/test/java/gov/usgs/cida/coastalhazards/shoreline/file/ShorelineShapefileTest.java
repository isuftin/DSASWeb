package gov.usgs.cida.coastalhazards.shoreline.file;

import gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
		ShorelineShapefile.validate(zipFile);
		assertTrue("Validated without exception", true);
		
		zipFile = gaMHWFalseShorelines;
		ShorelineShapefile.validate(zipFile);
		assertTrue("Validated without exception", true);
		
	}

	@Test(expected = ShorelineFileFormatException.class)
	public void testValidateWithInvalidFile() throws Exception {
		System.out.println("testValidateWithInvalidFile");
		File zipFile = noPRJShapeZip;
		ShorelineShapefile.validate(zipFile);
		assertTrue("Validated without exception", true);
	}

}
