package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.featureTypeFile.exception.FeatureTypeFileException;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class FeatureTypeFileFactoryTest {

	public FeatureTypeFileFactoryTest() {
	}
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureTypeFileFactoryTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");

	private static File workDir; // place to copy zip to
	private static File validShapeZip;
	private static File noPRJShapeZip;
	private static File invalidShapeZip;
	private static File validPdb;
	private static File validLidar;

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
		validPdb = new File(workDir, "GA_bias.zip");
		validLidar = new File(workDir, "LIDAR_GA_shorelines.zip");
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

	/**
	 * Test of createFeatureTypeFile method, of class FeatureTypeFileFactory.
	 */
	@Test
	@Ignore
	public void testCreateFeatureTypePDBFile() throws Exception {
		System.out.println("createFeatureTypePDBFile");
		//copy the validPdb zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//FileUtils.copyFileToDirectory(validPdb, UPLOAD_DIRECTORY);  //-->this copies the name of the zip as it was
		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(validPdb);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. 

		System.out.println("zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		FileInputStream pdbInputStream = FileUtils.openInputStream(validPdb);
		FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
		FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.PDB);

		// -- test getColumns from the dbf file
		String[] columns = result.getColumns();
		assertNotNull(columns);

		for (String name : columns) {
			System.out.println("Pdb column name is:" + name);
		}
		// -- test epsg
		String epsg = result.getEPSGCode();
		assertNotNull(epsg);
		System.out.println("Pdb EPSG code: " + epsg);

		// -- test type
		System.out.println("Feature type for PDB: " + result.getType().toString());

		//-- test get Required files
		List<File> reqFiles = result.getRequiredFiles();
		File[] files = reqFiles.toArray(new File[reqFiles.size()]);
		for (File aFile : files) {
			System.out.println("Pdb required file is:" + aFile.getName());
		}

		//-- test get Required files
		List<File> optFiles = result.getOptionalFiles();
		File[] optfiles = optFiles.toArray(new File[optFiles.size()]);
		for (File aFile : optfiles) {
			System.out.println("Pdb optional file is:" + aFile.getName());
		}

		// -- test internal map of file parts - its set in the init after constructor executes
		boolean doesFileMapExist = result.exists();
		assertTrue(doesFileMapExist);
		System.out.println("Filemap has contents");
		Collection<File> fileList = result.fileMap.values();
		Iterator<File> listIter = fileList.iterator();
		while (listIter.hasNext()) {
			File file = listIter.next();
			String filename = file.getName();
			System.out.println("Filemap filename:" + filename);
		}

		//-- test DB
		/// String viewname = result.importToDatabase(columns, epsg);  //need sample of columns that comes from the request
		// -- test Geo
		///result.importToGeoserver(viewname, workspace);
		// -- test DSASProcess
		result.updateProcessInformation("DSAS update process test message");
		// test token file with a FeatureTypeFile 
		String fileToken = TokenFeatureTypeFileExchanger.getToken(result);
		System.out.println("Token is: " + fileToken);
		FeatureTypeFile featureTypeFile = TokenFeatureTypeFileExchanger.getFeatureTypeFile(fileToken);
		System.out.println("File type retrieved is: " + featureTypeFile.getType());

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	@Test
	@Ignore
	public void testInvalidPdb() throws IOException, FeatureTypeFileException {
		// check the case statement logic - that the other/default case is executed
		System.out.println("testInvalidPdb");

		//copy the validLidar zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(validLidar);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));

		System.out.println("zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		try {
			FileInputStream pdbInputStream = FileUtils.openInputStream(validLidar);
			FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
			FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.PDB);
			assertNotNull(result);
		} catch (FeatureTypeFileException ex) {
			assertEquals(ex.getMessage(), "File has failed Pdb validation.");
			System.out.println(ex.getMessage());
		}

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	// ----------------------------------------------------------------
	/**
	 * Test of createFeatureTypeFile method, of class FeatureTypeFileFactory.
	 */
	@Test
	@Ignore
	public void testCreateFeatureTypeShorelineShapeFile() throws Exception {
		System.out.println("testCreateFeatureTypeShorelineShapeFile");
		//copy the validPdb zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		//UPLOAD_DIRECTORY = new File(tempDir, String.valueOf(new Date().getTime())); // tempDir grants java access rites to the dir structure under temp
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//FileUtils.copyFileToDirectory(validPdb, UPLOAD_DIRECTORY);  //-->this copies the name of the zip as it was
		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(validShapeZip);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. Is this what we want??

		System.out.println("Shape zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		FileInputStream pdbInputStream = FileUtils.openInputStream(validShapeZip);
		FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
		FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.SHORELINE);

		// -- test getColumns from the dbf file
		//List<String> columns = result.getColumns();
		String[] columns = result.getColumns();
		assertNotNull(columns);

		//String[] names = columns.toArray(new String[columns.size()]);
		for (String name : columns) {
			System.out.println("Shape column name is:" + name);
		}
		// -- test epsg
		String epsg = result.getEPSGCode();
		assertNotNull(epsg);
		System.out.println("Shape EPSG code: " + epsg);

		// -- test type
		System.out.println("Feature type for Shape: " + result.getType().toString());

		//-- test get Required files
		List<File> reqFiles = result.getRequiredFiles();
		File[] files = reqFiles.toArray(new File[reqFiles.size()]);
		for (File aFile : files) {
			System.out.println("Shape required file is:" + aFile.getName());
		}

		//-- test get Required files
		List<File> optFiles = result.getOptionalFiles();
		File[] optfiles = optFiles.toArray(new File[optFiles.size()]);
		for (File aFile : optfiles) {
			System.out.println("Shape optional file is:" + aFile.getName());
		}

		// -- test internal map of file parts - its set in the init after constructor executes
		boolean doesFileMapExist = result.exists();
		assertTrue(doesFileMapExist);
		System.out.println("Shape Filemap has contents");
		Collection<File> fileList = result.fileMap.values();
		Iterator<File> listIter = fileList.iterator();
		while (listIter.hasNext()) {
			File file = listIter.next();
			String filename = file.getName();
			System.out.println("Shape Filemap filename:" + filename);
		}

		//-- test DB
		/// String viewname = result.importToDatabase(columns, epsg);  //need sample of columns that comes from the request
		// -- test Geo
		///result.importToGeoserver(viewname, workspace);
		// -- test DSASProcess
		result.updateProcessInformation("DSAS update process test message");
//		System.out.println("process ID: " + result.process.getProcessId()); //dont know if this correct

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	// expect FeatureTypeFileException("Unable to create instantiate FeatureTypeFile with zip.")
	@Test //(expected = FeatureTypeFileException.class)
	@Ignore
	public void testInvalidShorelineShapefile() throws IOException, FeatureTypeFileException {
		// check the case statement logic - that the other/default case is executed
		System.out.println("testInvalidShorelineShapefile");

		//copy the validLidar zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(noPRJShapeZip);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. Is this what we want??

		System.out.println("zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		try {
			FileInputStream pdbInputStream = FileUtils.openInputStream(noPRJShapeZip);
			FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
			FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.SHORELINE);
			assertNotNull(result);
		} catch (FeatureTypeFileException ex) {
			assertEquals(ex.getMessage(), "Unable to create FeatureTypeFile with zip.");
			System.out.println(ex.getMessage());
		}

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	// -----------------------------------------------------
	@Test
	@Ignore
	public void testCreateFeatureTypeShorelineLidarFile() throws Exception {
		System.out.println("testCreateFeatureTypeShorelineLidarFile");
		//copy the validPdb zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
//		File WORK_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));
//		System.out.println("BASE dir:" + BASE_DIRECTORY.toString() );
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());
//		System.out.println("WORK_DIRECTORY: " + WORK_DIRECTORY.toString());

		// make the upload directory
		//UPLOAD_DIRECTORY = new File(tempDir, String.valueOf(new Date().getTime())); // tempDir grants java access rites to the dir structure under temp
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//FileUtils.copyFileToDirectory(validPdb, UPLOAD_DIRECTORY);  //-->this copies the name of the zip as it was
		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(validLidar);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. Is this what we want??

		System.out.println("Lidar zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		FileInputStream pdbInputStream = FileUtils.openInputStream(validLidar);
		FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
		FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.SHORELINE);

		// -- test getColumns from the dbf file
		//List<String> columns = result.getColumns();
		String[] columns = result.getColumns();
		assertNotNull(columns);

		//String[] names = columns.toArray(new String[columns.size()]);
		for (String name : columns) {
			System.out.println("Lidar column name is:" + name);
		}
		// -- test epsg
		String epsg = result.getEPSGCode();
		assertNotNull(epsg);
		System.out.println("Lidar EPSG code: " + epsg);

		// -- test type
		System.out.println("Feature type for Lidar: " + result.getType().toString());

		//-- test get Required files
		List<File> reqFiles = result.getRequiredFiles();
		File[] files = reqFiles.toArray(new File[reqFiles.size()]);
		for (File aFile : files) {
			System.out.println("Lidar required file is:" + aFile.getName());
		}

		//-- test get Required files
		List<File> optFiles = result.getOptionalFiles();
		File[] optfiles = optFiles.toArray(new File[optFiles.size()]);
		for (File aFile : optfiles) {
			System.out.println("Lidar optional file is:" + aFile.getName());
		}
		if (optFiles.isEmpty()) {
			System.out.println("Lidar does not have any optional files");
		}

		// -- test internal map of file parts - its set in the init after constructor executes
		boolean doesFileMapExist = result.exists();
		assertTrue(doesFileMapExist);
		System.out.println("Lidar Filemap has contents");
		Collection<File> fileList = result.fileMap.values();
		Iterator<File> listIter = fileList.iterator();
		while (listIter.hasNext()) {
			File file = listIter.next();
			String filename = file.getName();
			System.out.println("Lidar Filemap filename:" + filename);
		}

		//-- test DB
		/// String viewname = result.importToDatabase(columns, epsg);  //need sample of columns that comes from the request
		// -- test Geo
		///result.importToGeoserver(viewname, workspace);
		// -- test DSASProcess
		result.updateProcessInformation("DSAS update process test message");
//		System.out.println("process ID: " + result.process.getProcessId()); //dont know if this correct

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	// expect FeatureTypeFileException("Unable to create instantiate FeatureTypeFile with zip.")
	@Test //(expected = FeatureTypeFileException.class)
	@Ignore
	public void testInvalidShorelineLidarfile() throws IOException, FeatureTypeFileException {
		// check the case statement logic - that the other/default case is executed
		System.out.println("testInvalidShorelineLidarfile");

		//copy the validLidar zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(noPRJShapeZip);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. Is this what we want??

		System.out.println("zip file should be in location: " + UPLOAD_DIRECTORY.getPath());

		try {
			FileInputStream pdbInputStream = FileUtils.openInputStream(noPRJShapeZip);
			FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
			FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.SHORELINE);
			assertNotNull(result);
		} catch (FeatureTypeFileException ex) {
			assertEquals(ex.getMessage(), "Unable to create FeatureTypeFile with zip.");
			System.out.println(ex.getMessage());
		}

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

	// -----------------------------------------------------
	@Test
	public void testAutoNumericNamedZip() throws IOException, FeatureTypeFileException {
		System.out.println("testAutoNumericNamedZip");
		// This test was created to determine if starting the name of the zip with a numeric would cause issues. 
		// In the previous code, a 'clean' was done on the zip that added an underscore to the zip file name if it began with a number.
		//copy the validPdb zip to the directory found in your application.properties
		// --->   /var/folders/hi/deleteme.test.upload/
		File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
		File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		System.out.println("UPLOAD_DIRECTORY :" + UPLOAD_DIRECTORY.toPath());

		// make the upload directory
		//UPLOAD_DIRECTORY = new File(tempDir, String.valueOf(new Date().getTime())); // tempDir grants java access rites to the dir structure under temp
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
		FileUtils.forceMkdir(UPLOAD_DIRECTORY);

		//FileUtils.copyFileToDirectory(validPdb, UPLOAD_DIRECTORY);  //-->this copies the name of the zip as it was
		//copy the valide pdb zip into the upload location to begin the test
		FileInputStream InStream = FileUtils.openInputStream(validPdb);
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(InStream, new FileOutputStream(copiedZip));  //---> this makes the name numeric and is currently in the code base. Is this what we want??

		System.out.println("zip file should be in location: " + UPLOAD_DIRECTORY.getPath());
		System.out.println("zip file name: " + copiedZip.getName());

		FileInputStream pdbInputStream = FileUtils.openInputStream(validPdb);
		FeatureTypeFileFactory instance = new FeatureTypeFileFactory();
		FeatureTypeFile result = instance.createFeatureTypeFile(pdbInputStream, FeatureType.PDB);
		//----
		//ShpFiles shpFile = new ShpFiles(result.fileMap.get("shp"));
		File shpFile = result.fileMap.get("shp");
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(shpFile.toURI().toURL());

		// clean up
		FileUtils.listFiles(UPLOAD_DIRECTORY, null, true).stream().forEach((file) -> {
			FileUtils.deleteQuietly(file);
		});
		FileUtils.deleteQuietly(UPLOAD_DIRECTORY);
	}

}
