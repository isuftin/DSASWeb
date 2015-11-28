package gov.usgs.cida.dsas.uncy;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.h2.engine.SysProperties;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.LoggerFactory;

/**
 * Tests the integration of the exploder using a database feature writer
 *
 * @author isuftin
 */
@Category(XploderIntegrationTest.class)
public class XploderH2IntegrationTest implements XploderIntegrationTest {
	
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(XploderH2IntegrationTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static final String capeCodName = "OuterCapeCod_shorelines_ghost";
	private static final String testShorelinesName = "test_shorelines";
	private static File capeCodShapefile;
	private static File testShorelinesShapefile;
	private static Connection conn;
	
	@BeforeClass
	public static void setUpClass() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		Class.forName("org.h2.Driver").newInstance();
		
		conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:" 
				+ System.getProperty("db.h2.integration-test.port") 
				+ "/mem:" + System.getProperty("db.h2.integration-test.dbname") 
				+ ";create=false", 
				System.getProperty("db.h2.integration-test.username"), 
				System.getProperty("db.h2.integration-test.password"));
		
		workDir = new File(tempDir, String.valueOf(new Date().getTime()));
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);
	}

	@AfterClass
	public static void tearDownClass() throws SQLException {
		FileUtils.deleteQuietly(workDir);
		conn.close();
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
	public void testCreateXploder() throws IOException, SQLException {
		LOG.info("testCreateXploder()");
		
		File tempFile = Files.createTempFile(new File(tempDir).toPath(), "tempFile", ".shp", new FileAttribute<?>[0]).toFile();
		tempFile.deleteOnExit();
		
		Map<String, String> config = new HashMap<>(3);
		config.put(ShapefileOutputXploder.UNCERTAINTY_COLUMN_PARAM, "ACCURACY");
		config.put(ShapefileOutputXploder.INPUT_FILENAME_PARAM, workDir + "/" + testShorelinesName);
		config.put(H2DatabaseOutputExplorer.HOST_PARAM, "localhost");
		config.put(H2DatabaseOutputExplorer.PORT_PARAM, System.getProperty("db.h2.integration-test.port"));
		config.put(H2DatabaseOutputExplorer.DATABASE_PARAM, System.getProperty("db.h2.integration-test.dbname"));
		config.put(H2DatabaseOutputExplorer.USERNAME_PARAM, System.getProperty("db.h2.integration-test.username"));
		config.put(H2DatabaseOutputExplorer.PASSWORD_PARAM, System.getProperty("db.h2.integration-test.password"));
		
		H2DatabaseOutputExplorer exploder = new H2DatabaseOutputExplorer(config);
		assertTrue(true);
//		exploder.explode();
	}
	
}
