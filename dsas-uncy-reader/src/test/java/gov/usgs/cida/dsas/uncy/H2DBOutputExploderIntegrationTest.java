package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.commons.io.FileUtils;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
public class H2DBOutputExploderIntegrationTest implements XploderIntegrationTest {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(H2DBOutputExploderIntegrationTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static final String capeCodName = "OuterCapeCod_shorelines_ghost";
	private static final String testShorelinesName = "test_shorelines";
	private static File capeCodShapefile;
	private static File testShorelinesShapefile;
	private static Liquibase liquibase;
	private static Connection conn;
	private static final Contexts contexts = new Contexts("integration-test");

	@BeforeClass
	public static void setUpClass() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, DatabaseException, LockException, LiquibaseException {
		Class.forName("org.h2.Driver").newInstance();

		conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:"
				+ System.getProperty("db.h2.integration-test.port")
				+ "/mem:" + System.getProperty("db.h2.integration-test.dbname")
				+ ";create=false",//;TRACE_LEVEL_FILE=1;TRACE_LEVEL_SYSTEM_OUT=1",
				System.getProperty("db.h2.integration-test.username"),
				System.getProperty("db.h2.integration-test.password"));

		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
		liquibase = new Liquibase("src/test/resources/liquibase/changelogs/create-table-parent-changeLog.xml", new FileSystemResourceAccessor(), database);
		liquibase.dropAll();
		liquibase.update(contexts, new PrintWriter(System.out));

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
		liquibase.update("integration-test");

		String packagePath = "/";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		capeCodShapefile = new File(workDir, capeCodName + ".zip");
		testShorelinesShapefile = new File(workDir, testShorelinesName + ".zip");
		FileHelper.unzipFile(workDir.toString(), capeCodShapefile);
		FileHelper.unzipFile(workDir.toString(), testShorelinesShapefile);
	}

	@After
	public void tearDown() throws LiquibaseException {
		liquibase.rollback("pre-points-table-created", "integration-test", new PrintWriter(System.out));

		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	@Test
	@Ignore
	public void testCreateXploder() throws IOException, SQLException {
		LOG.info("testCreateXploder()");

		Map<String, String> config = new HashMap<>();
		config.put(ShapefileOutputXploder.UNCERTAINTY_COLUMN_PARAM, "ACCURACY");
		config.put(ShapefileOutputXploder.INPUT_FILENAME_PARAM, workDir + "/" + testShorelinesName);
		config.put(H2DBOutputXploder.HOST_PARAM, "localhost");
		config.put(H2DBOutputXploder.PORT_PARAM, System.getProperty("db.h2.integration-test.port"));
		config.put(H2DBOutputXploder.DATABASE_PARAM, "mem:" + System.getProperty("db.h2.integration-test.dbname"));
		config.put(H2DBOutputXploder.USERNAME_PARAM, System.getProperty("db.h2.integration-test.username"));
		config.put(H2DBOutputXploder.PASSWORD_PARAM, System.getProperty("db.h2.integration-test.password"));

		SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
		ftb.setName("POINTS");
		ftb.setSRS("EPSG:4326");
		ftb.add("GEOM", Point.class, DefaultGeographicCRS.WGS84);
		ftb.add("SHORELINE_ID", BigInteger.class);
		ftb.add("SEGMENT_ID", BigInteger.class);
		ftb.add("UNCY", Double.class);

		H2DBOutputXploder exploder = new H2DBOutputXploder(config);
		exploder.setOutputFeatureType(ftb.buildFeatureType());
		exploder.explode();
	}

}
