package gov.usgs.cida.dsas.model;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author isuftin
 */
public class ShapeFileTest {

	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static File validShapeZip;
	private static File noPRJShapeZip;
	private static File gaMHWFalseShorelines;

	public ShapeFileTest() {
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
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	@Test(expected = IOException.class)
	public void testCreateShapefileFromZipExpectIOE() throws IOException {
		System.out.println("testCreateShapefileFromZipExpectIOE");
		new ShapeFile(validShapeZip);
	}

	@Test(expected = IOException.class)
	public void testCreateShapefileFromEmptyDirectorypExpectIOE() throws IOException {
		System.out.println("testCreateShapefileFromEmptyDirectorypExpectIOE");
		new ShapeFile(validShapeZip);
	}

	@Test 
	public void testCreateShapefileFromValidShapefile() throws Exception {
		System.out.println("testCreateShapefileFromEmptyDirectorypExpectIOE");
		File tmpDir = null;
		try {
			tmpDir = Files.createTempDirectory(UUID.randomUUID().toString(), new FileAttribute[0]).toFile();
			FileHelper.unzipFile(tmpDir.getAbsolutePath(), validShapeZip);
			tmpDir.deleteOnExit();
			try (ShapeFile instance = new ShapeFile(tmpDir)) {
				assertNotNull(instance);
			}
		} finally {
			FileUtils.deleteQuietly(tmpDir);
		}
	}
        
}
