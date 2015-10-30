package gov.usgs.cida.utilities.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author isuftin
 */
public class ShapefileHelperTest {
	
	public ShapefileHelperTest() {
	}
	
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static File validShapeZip;
	private static File noPRJShapeZip;
	
	
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
	}

	@After
	public void tearDown() {
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	/**
	 * Test of getBoundBoxFromShapefile method, of class ShapefileHelper.
	 */
	@Test
	public void testGetBoundBoxFromShapefileUsingValidZip() throws IOException, TransformException, FactoryException {
		System.out.println("getBoundBoxFromShapefileUsingValidZip");
		BoundingBox result = ShapefileHelper.getBoundingBoxFromShapefile(validShapeZip);
		assertThat("Bounding Box Not Null Test", result, is(notNullValue()));
		
		double expMaxX = 40.478368325842034;
		double expMaxY = -73.97145395241397;
		double expMinX = 39.50032713356323;
		double expMinY = -74.3004691692885;
		assertThat(result.getMaxX(), is(equalTo(expMaxX)));
		assertThat(result.getMaxY(), is(equalTo(expMaxY)));
		assertThat(result.getMinX(), is(equalTo(expMinX)));
		assertThat(result.getMinY(), is(equalTo(expMinY)));
	}
	
	/**
	 * Test of getBoundBoxFromShapefile method, of class ShapefileHelper.
	 */
	@Test(expected = IOException.class)
	public void testGetBoundBoxFromShapefileUsingShapeWithoutPRJ() throws IOException, TransformException, FactoryException {
		System.out.println("getBoundBoxFromShapefileUsingShapeWithoutPRJ");
		ShapefileHelper.getBoundingBoxFromShapefile(noPRJShapeZip);
		assertThat("Test should have thrown IOexception", false, is(true));
	}
	
}
