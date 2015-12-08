package gov.usgs.cida.dsas.service.util;

import gov.usgs.cida.dsas.featureTypeFile.exception.LidarFileFormatException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class LidarFileUtilsTest {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapeFileUtilTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");

	private static File workDir;
	private static File validLidarZip;
	private static File invalidLidarZip;
	static final String PRJ = "prj";
	static final String CSV = "csv";

	public LidarFileUtilsTest() {
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
	public void setUp() throws IOException, URISyntaxException {
		String packagePath = "/";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		validLidarZip = new File(workDir, "LIDAR_GA_shorelines.zip");
		invalidLidarZip = new File(workDir, "invalidLidar.zip");

	}

	@After
	public void tearDown() {
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

	/**
	 * Test of isLidar method, of class LidarFileUtils.
	 */
	@Test
	public void testIsLidar() throws Exception {
		System.out.println("isLidar");
		// this looks for the csv within the zip

		File shorelineFile = null;
		boolean expResult = true;

		File csvFile = null;

		File tempDir1 = null;
		File tempLidarZipFile = null;

		// open the zip and get the prj file
		tempDir1 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir1.deleteOnExit();
		tempLidarZipFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempLidarZipFile.deleteOnExit();

		FileUtils.copyFile(validLidarZip, tempLidarZipFile);
		FileHelper.unzipFile(tempDir1.getAbsolutePath(), tempLidarZipFile);
		String[] csvType = new String[]{CSV};
		Collection<File> csvFiles = FileUtils.listFiles(tempDir1, csvType, true);
		csvFile = csvFiles.iterator().next();

		boolean result = LidarFileUtils.isLidar(csvFile);
		assertEquals(expResult, result);

	}

	/**
	 * Test of validateLidarFileZip method, of class LidarFileUtils.
	 */
	@Test
	public void testValidateLidarFileZip() throws Exception {
		System.out.println("validateLidarFileZip");
		File tempDir1 = null;
		File tempShapeFile = null;

		tempDir1 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir1.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(validLidarZip, tempShapeFile);

		boolean expResult = true;
		boolean result = LidarFileUtils.validateLidarFileZip(tempShapeFile);

		assertEquals(expResult, result);
	}

	/**
	 * Test of validateHeaderRow method, of class LidarFileUtils. Included below
	 */
	public void testValidateHeaderRow() throws Exception {
		System.out.println("validateHeaderRow");
		String[] headerRow = null;
		LidarFileUtils.validateHeaderRow(headerRow);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of validateDataRow method, of class LidarFileUtils.
	 */
	@Test
	public void testValidateHeadernDataRow() throws Exception {
		System.out.println("testValidateHeadernDataRow");
		String[] dataRow = null;
		//LidarFileUtils.validateDataRow(dataRow);
		//this requires the csv file so unzip the contents and get the CSV file
		File csvFile = null;

		File tempDir1 = null;
		File tempLidarZipFile = null;

		// open the zip and get the prj file
		tempDir1 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir1.deleteOnExit();
		tempLidarZipFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempLidarZipFile.deleteOnExit();

		FileUtils.copyFile(validLidarZip, tempLidarZipFile);
		FileHelper.unzipFile(tempDir1.getAbsolutePath(), tempLidarZipFile);
		String[] csvType = new String[]{CSV};
		Collection<File> csvFiles = FileUtils.listFiles(tempDir1, csvType, true);
		csvFile = csvFiles.iterator().next();
		
		assertTrue(LidarFileUtils.isLidar(csvFile));

		try (
				BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			String line;
			int row = 0;
			long prevShorelineId = -1;
			int prevSegmentId = -1;
			HashMap<String, Long> shorelineDateToIdMap = new HashMap<>();

			while ((line = br.readLine()) != null) {
				row++;

				// use comma as separator
				String[] point = line.split(",");

				//validation
				try {
					if (row == 1) {
						LidarFileUtils.validateHeaderRow(point);
						continue;
					} else {
						LidarFileUtils.validateDataRow(point);
					}
				} catch (LidarFileFormatException ex) {
					throw new ShorelineFileFormatException(ex.getMessage());
				}
			}

		}
	}

	/**
	 * Test of getEPSGCode method, of class LidarFileUtils.
	 */
	@Test
	public void testGetEPSGCode() throws Exception {
		System.out.println("getEPSGCode");
		File prjFile = null;
		String expResult = "";

		File tempDir1 = null;
		File tempLidarZipFile = null;

		// open the zip and get the prj file
		tempDir1 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir1.deleteOnExit();
		tempLidarZipFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempLidarZipFile.deleteOnExit();

		FileUtils.copyFile(validLidarZip, tempLidarZipFile);
		FileHelper.unzipFile(tempDir1.getAbsolutePath(), tempLidarZipFile);
		String[] PrjType = new String[]{PRJ};
		Collection<File> prjFiles = FileUtils.listFiles(tempDir1, PrjType, true);
		prjFile = prjFiles.iterator().next();

		String result = LidarFileUtils.getEPSGCode(prjFile);
		System.out.println("EPSG Code  " + result);
		assertNotNull(result);

		assertTrue(!StringUtils.isBlank(result) || !StringUtils.isEmpty(result));

		//assertEquals(expResult, result);
		// TODO the ProjectionUtils method is rather slow ...opens bytearray etc
	}

}
