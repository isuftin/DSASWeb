package gov.usgs.cida.dsas.service.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.util.Assert;
import gov.usgs.cida.dsas.shapefile.ShapeFile;
import gov.usgs.cida.dsas.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.owsutils.commons.io.exception.ShapefileFormatException;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.data.shapefile.files.ShpFileType;

import org.junit.After;
import org.junit.AfterClass;
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
		String packagePath = "/";
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

	@Test
	public void testGetDbfColumnNames() throws IOException {
		File tempDir = null;
		File tempShapeFile = null;

		tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(validShapeZip, tempShapeFile);
		FileHelper.unzipFile(tempDir.getAbsolutePath(), tempShapeFile);

		//ShapeFile instance = new ShapeFile(tempDir.listFiles()[0]);
		List<String> columns = ShapeFileUtil.getDbfColumnNames(tempShapeFile);
		System.out.println("File passed into ShapefileUtil as dir:" + tempDir);
		//List<String> columns = instance.getDbfColumnNames();
		for (String column : columns) {
			System.out.println("Column Name:" + column);
		}
	}

	@Test
	public void testGetEPSGCode() throws IOException {
		File tempD = null;
		File tempShapeFile = null;

		tempD = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempD.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(validShapeZip, tempShapeFile);
		FileHelper.unzipFile(tempD.getAbsolutePath(), validShapeZip);

		String epsg = ShapeFileUtil.getEPSGCode(tempShapeFile);
		assertNotNull(epsg);

		System.out.println(epsg);
	}

	// mimicking what is in the ShapefileResource 
	@Test
	public void testGetTokenToFileToNames() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		File tempDir1 = null;
		File tempShapeFile = null;

		tempDir1 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDir1.deleteOnExit();
		tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
		tempShapeFile.deleteOnExit();
		FileUtils.copyFile(validShapeZip, tempShapeFile);
		FileHelper.unzipFile(tempDir1.getAbsolutePath(), tempShapeFile);

		String token = TokenFileExchanger.getToken(tempShapeFile); //full name of zip  full/path/to/file/filename.ext
		File shapeZip = TokenFileExchanger.getFile(token);

		//	List<String> nameList = ShapefileUtil.getDbfColumnNames(shapeZip);
		//	List<String> nameList = ShapefileUtil.getDbfColumnNames(shapeZip.getParentFile());
		List<String> nameList = ShapeFileUtil.getDbfColumnNames(shapeZip);

		String[] names = nameList.toArray(new String[nameList.size()]);
		assertNotNull(names);

		String jsonNames = gson.toJson(names, String[].class);
		System.out.println("jason Names: " + jsonNames);

	}

	@Test // ShapefileResource
	public void testShapeFileCreateFromFileInputStream() throws IOException {
		File tempDirectory = null;
		File tempDirectory2 = null;
		//File tempShapeFile = null;
		// get an input stream from the valid shape zip
		FileInputStream fileInputStream =FileUtils.openInputStream(validShapeZip);
		
		tempDirectory = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDirectory.deleteOnExit();
		
		// create the zip file with the valide shape files inputstream and flatten it
		File shapeZip = Files.createTempFile(tempDirectory.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(fileInputStream, new FileOutputStream(shapeZip));
		FileHelper.flattenZipFile(shapeZip);
		
		// list what is in the files directory...expect the new zip, full file name.
		Collection<File> fileColl = FileUtils.listFiles(tempDirectory, null, false);
		Iterator it = fileColl.iterator();
		while (it.hasNext())
		{
			System.out.println("Files in tempDirectory after copy (expect ZIP):" + ((File)it.next()).getName());
		}
		
		//output the parts in the copy of the shapefile
		System.out.println("Copied zip's path :" + shapeZip.getParentFile().getPath()); // use this to pass into the ShapeFileUtil after getting the file from the TokenFileExchanger
		System.out.println("tempDirectory path:" + tempDirectory.getPath());
		Assert.equals(shapeZip.getParentFile().getPath(), tempDirectory.getPath());
		
		//create another temp dir to unzip the copied files contents
		tempDirectory2 = Files.createTempDirectory("temp-shapefile-dir").toFile();
		tempDirectory2.deleteOnExit();
		
		FileHelper.unzipFile(tempDirectory2.getAbsolutePath(), shapeZip);
		// output the contents to make sure the dbf is there
		List<String> files= FileHelper.getFileList(tempDirectory2.toString(), false);
		assertNotNull(files);
		Iterator<String> iter = files.iterator();
		while (iter.hasNext())
		{
			System.out.println("copied zip file contents:" + iter.next());
		}
		
		// using any of the ShapeFileUtil methods to test getParentFile()
		List<String> nameList = ShapeFileUtil.getDbfColumnNames(shapeZip);

		String[] names = nameList.toArray(new String[nameList.size()]);
		assertNotNull(names);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonNames = gson.toJson(names, String[].class);
		System.out.println("jason Names: " + jsonNames);
	}

	@Test
	public void testGetFileNamesWithValidZip() throws IOException
	{
		Map<ShpFileType, String> map= ShapeFileUtil.getFileNames(validShapeZip);
		
		String stringUrlToShapeFile = map.get(ShpFileType.SHP);
		String stringUrlToDbfFile = map.get(ShpFileType.DBF);
		String stringUrlToShxFile = map.get(ShpFileType.SHX); //shp index
		
		assertNotNull(stringUrlToShapeFile);
		System.out.println("URL to shape file: " + stringUrlToShapeFile);
		
		assertNotNull(stringUrlToDbfFile);
		System.out.println("URL to dbf file: " + stringUrlToDbfFile);
		
		assertNotNull(stringUrlToShxFile);
		System.out.println("URL to shx file: " + stringUrlToShxFile);
	}
	
	@Test (expected = FileNotFoundException.class)
	public void testGetFileNamesWithInvalidZip() throws IOException
	{
		Map<ShpFileType, String> map= ShapeFileUtil.getFileNames(invalidShapeZip);
		
	}
	
	@Test (expected = FileNotFoundException.class)
	public void testIsValidWithInvalidZip() throws IOException
	{
		boolean result = ShapeFileUtil.isValidShapeZip(invalidShapeZip);
	}
	
		
	@Test 
	public void testValidWithInvalidZip() throws IOException
	{
		boolean result = ShapeFileUtil.isValidShapeZip(validShapeZip);
		assertTrue(result);
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
