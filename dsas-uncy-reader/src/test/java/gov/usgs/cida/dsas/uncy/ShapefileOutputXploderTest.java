package gov.usgs.cida.dsas.uncy;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class ShapefileOutputXploderTest {
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ShapefileOutputXploderTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static final String capeCodName = "OuterCapeCod_shorelines_ghost";
	private static final String testShorelinesName = "test_shorelines";
	private static File capeCodShapefile;
	private static File testShorelinesShapefile;

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
		capeCodShapefile = new File(workDir, capeCodName + ".zip");
		testShorelinesShapefile = new File(workDir, testShorelinesName + ".zip");
		FileHelper.unzipFile(workDir.toString(), capeCodShapefile);
		FileHelper.unzipFile(workDir.toString(), testShorelinesShapefile);
	}
	
	@After
	public void tearDown() {
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	@Test
	public void testExplodeUsingTestShorelines() throws Exception {
		LOG.info("testExplodeUsingTestShorelines()");
		File tempFile = Files.createTempFile(new File(tempDir).toPath(), "tempFile", ".shp", new FileAttribute<?>[0]).toFile();
		tempFile.deleteOnExit();
		Map<String, Object> config = new HashMap<>(3);
		config.put(Xploder.UNCERTAINTY_COLUMN_PARAM, "ACCURACY");
		config.put(Xploder.INPUT_FILENAME_PARAM, testShorelinesShapefile.getAbsolutePath());
		config.put(ShapefileOutputXploder.OUTPUT_FILENAME_PARAM, tempFile.getAbsolutePath());
		
		
		Xploder x = new ShapefileOutputXploder(config);
		int pointsCreated = x.explode();
		assertTrue(pointsCreated > 0);
		assertTrue(tempFile.exists());
		assertEquals(tempFile.length(), 94712l);
	}
	
	@Test
	public void testExplodeUsingCapeCodhorelines() throws Exception {
		LOG.info("testExplodeUsingCapeCodhorelines()");
		
		File tempFile = Files.createTempFile(new File(tempDir).toPath(), "tempFile", ".shp", new FileAttribute<?>[0]).toFile();
		tempFile.deleteOnExit();
		Map<String, Object> config = new HashMap<>(3);
		config.put(ShapefileOutputXploder.UNCERTAINTY_COLUMN_PARAM, "laser_u");
		config.put(ShapefileOutputXploder.INPUT_FILENAME_PARAM, capeCodShapefile.getAbsolutePath());
		config.put(ShapefileOutputXploder.OUTPUT_FILENAME_PARAM, tempFile.getAbsolutePath());
		
		Xploder x = new ShapefileOutputXploder(config);
		int pointsCreated = x.explode();
		assertTrue(tempFile.exists());
		assertTrue(pointsCreated > 0);
		assertEquals(tempFile.length(), 3061368l);
	}

}
