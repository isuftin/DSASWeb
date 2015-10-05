package gov.usgs.cida.coastalhazards.uncy;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class XploderTest {

	private static final Logger LOG = Logger.getLogger(XploderTest.class.getName());
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
		for (File file : FileUtils.listFiles(workDir, null, true)) {
			FileUtils.deleteQuietly(file);
		}
	}

	@Test
	public void testExplodeUsingTestShorelines() throws Exception {
		LOG.info("testExplodeUsingTestShorelines()");
		Xploder x = new Xploder("ACCURACY");
		File result = x.explode(workDir + "/" + testShorelinesName);
		assertTrue("survived", true);
		assertTrue(result.exists());
		assertEquals(result.length(), 94712l);
	}
	
	@Test
	@Ignore
	public void testExplodeUsingCapeCodhorelines() throws Exception {
		LOG.info("testExplodeUsingCapeCodhorelines()");
		Xploder x = new Xploder("laser_u");
		File result = x.explode(workDir + "/" + capeCodName);
		assertTrue("survived", true);
		assertTrue(result.exists());
		assertEquals(result.length(), 3061368l);
	}

}
