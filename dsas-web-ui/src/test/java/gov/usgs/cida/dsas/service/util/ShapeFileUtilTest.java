package gov.usgs.cida.dsas.service.util;

import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.owsutils.commons.io.exception.ShapefileFormatException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class ShapeFileUtilTest {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapeFileUtilTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");

	private static File workDir;
	private static File validShapeZip;
	private static File noPRJShapeZip;
	private static File invalidShapeZip;

	@BeforeClass
	public static void setUpClass() throws IOException {
		workDir = new File(tempDir, String.valueOf(new Date().getTime()));
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);

	}

	@Before
	public void setUp() throws URISyntaxException, IOException {
		String packagePath = "/shapefiles";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		validShapeZip = new File(workDir, "valid_shapezip.zip");
		noPRJShapeZip = new File(workDir, "no_prj_shapefile.zip");
		invalidShapeZip = new File(workDir, "invalidShape.zip");

	}

	@Test
	public void testValidShapeZipFile() throws Exception {
		LOGGER.info("testForValidShapeFile");

		assertNotNull(validShapeZip);
		LOGGER.info("Zip files absolute path: " + validShapeZip.getAbsolutePath());

		assertTrue(FileHelper.isZipFile(validShapeZip));

		// more robust check to see if it is really a zip
		ZipFile zip = new ZipFile(validShapeZip);
		LOGGER.info("Zip size is: " + zip.size());
		Enumeration e = zip.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			LOGGER.info("Zip file entry name: " + entry.getName());
		}
		assertEquals(6, zip.size());
	}

	@Test
	public void testInvalidShapeZipFile() throws Exception {
		LOGGER.info("testForInvalidShapeFile");

		assertNotNull(noPRJShapeZip); //InvalidShapeZip

		LOGGER.info("Zip files absolute path: " + noPRJShapeZip.getAbsolutePath());

		assertTrue(FileHelper.isZipFile(noPRJShapeZip));

		// more robust check to see if it is really a zip
		ZipFile zip = new ZipFile(noPRJShapeZip);

		LOGGER.info("Zip size is: " + zip.size());
		Enumeration e = zip.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			LOGGER.info("Zip file entry name: " + entry.getName());
		}
		try {
			FileHelper.validateShapefileZip(noPRJShapeZip);  // this should throw as its not valid - missing shp file as planned
		} catch (ShapefileFormatException sfe) {
			assertEquals(sfe.getMessage(), "Shapefile archive is not valid");
		}
	}

	@Test
	public void testOtherInvalidShapeZipFile() throws Exception {
		LOGGER.info("testForInvalidShapeFile");
		String packagePath = "/";
		File path = new File(getClass().getResource(packagePath).toURI());
		LOGGER.info("The zip files source directory:" + path.getPath());

		assertNotNull(invalidShapeZip);
		LOGGER.info("Zip files absolute path: " + invalidShapeZip.getAbsolutePath());

		assertTrue(FileHelper.isZipFile(invalidShapeZip));

		// more robust check to see if it is really a zip
		ZipFile zip = new ZipFile(invalidShapeZip);
		LOGGER.info("Zip size is: " + zip.size());
		Enumeration e = zip.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			LOGGER.info("Zip file entry name: " + entry.getName());
		}
		try {
			FileHelper.validateShapefileZip(invalidShapeZip);  // this should throw as its not valid - missing shp file as planned
		} catch (ShapefileFormatException sfe) {
			assertEquals(sfe.getMessage(), "Shapefile archive needs to contain at least one shapefile");
		}
	}

	@AfterClass
	public static void tearDownClass() {
		FileUtils.deleteQuietly(workDir);

	}

	@After
	public void tearDown() {
		FileUtils.listFiles(workDir, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
	}

}
