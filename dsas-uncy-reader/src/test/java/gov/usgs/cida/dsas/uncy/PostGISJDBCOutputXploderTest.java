package gov.usgs.cida.dsas.uncy;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.LoggerFactory;

/**
 * Tests the integration of the exploder using a database feature writer
 *
 * @author isuftin
 */
@Category(XploderIntegrationTest.class)
public class PostGISJDBCOutputXploderTest implements XploderIntegrationTest {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PostGISJDBCOutputXploderTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static final String capeCodName = "OuterCapeCod_shorelines_ghost";
	private static final String testShorelinesName = "test_shorelines";
	private static File capeCodShapefile;
	private static File testShorelinesShapefile;
	private static Connection conn;
	private static Map<String, Object> config = new HashMap<>();

	@BeforeClass
	public static void setUpClass() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, DatabaseException, LockException, LiquibaseException {
		Class.forName("org.postgresql.Driver");

		config.put(PostGISJDBCOutputXploder.HOST_PARAM, "localhost");
		config.put(PostGISJDBCOutputXploder.PORT_PARAM, 5432);
		config.put(PostGISJDBCOutputXploder.DATABASE_PARAM, "dsas");
		config.put(PostGISJDBCOutputXploder.USERNAME_PARAM, "");
		config.put(PostGISJDBCOutputXploder.PASSWORD_PARAM, "");
		config.put(PostGISJDBCOutputXploder.SCHEMA_PARAM, "public");

		conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s",
				config.get(PostGISJDBCOutputXploder.HOST_PARAM),
				config.get(PostGISJDBCOutputXploder.PORT_PARAM),
				config.get(PostGISJDBCOutputXploder.DATABASE_PARAM)),
				(String) config.get(PostGISJDBCOutputXploder.USERNAME_PARAM),
				(String) config.get(PostGISJDBCOutputXploder.PASSWORD_PARAM));

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
	public void setUp() throws URISyntaxException, IOException, LiquibaseException {
		String packagePath = "/";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		capeCodShapefile = new File(workDir, capeCodName + ".zip");
		testShorelinesShapefile = new File(workDir, testShorelinesName + ".zip");
		FileHelper.unzipFile(workDir.toString(), capeCodShapefile);
		FileHelper.unzipFile(workDir.toString(), testShorelinesShapefile);
	}

	@After
	public void tearDown() throws LiquibaseException {
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	@Test
	@Ignore
	public void testXploderWithTestShapefile() throws IOException, SQLException, Exception {
		LOG.info("testXploderWithTestShapefile()");

		Map<String, Object> testConfig = new HashMap<>();
		config.put(Xploder.UNCERTAINTY_COLUMN_PARAM, "ACCURACY");
		config.put(Xploder.INPUT_FILENAME_PARAM, testShorelinesShapefile.getAbsolutePath());
		config.put(DatabaseOutputXploder.INCOMING_DATEFIELD_NAME_PARAM, "DATE_");
		config.put(DatabaseOutputXploder.WORKSPACE_PARAM, "mb8d512c803ee41539d3edef1d803e360");

		Map<String, Object> mergedMap = new HashMap<>(testConfig);
		mergedMap.putAll(config);

		try (Xploder exploder = new PostGISJDBCOutputXploder(mergedMap)) {
			exploder.explode();
		}
	}

	@Test
	@Ignore
	public void testXploderWithCCShapefile() throws IOException, SQLException, Exception {
		LOG.info("testXploderWithCCShapefile()");

		Map<String, Object> testConfig = new HashMap<>();
		config.put(Xploder.UNCERTAINTY_COLUMN_PARAM, "laser_u");
		config.put(Xploder.INPUT_FILENAME_PARAM, capeCodShapefile.getAbsolutePath());
		config.put(DatabaseOutputXploder.INCOMING_DATEFIELD_NAME_PARAM, "Date_");
		config.put(DatabaseOutputXploder.WORKSPACE_PARAM, "mb8d512c803ee41539d3edef1d803e360");

		Map<String, Object> mergedMap = new HashMap<>(testConfig);
		mergedMap.putAll(config);

		try (Xploder exploder = new PostGISJDBCOutputXploder(mergedMap)) {
			exploder.explode();
		}
	}

}
