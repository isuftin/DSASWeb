package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.service.util.ShapeFileUtilTest;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.feature.SchemaException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class FeatureTypeFileTest {
	
	public FeatureTypeFileTest() {
	}
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureTypeFileTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");

	private static File workDir; // place to copy zip to
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
	 * Test of createWorkLocationForZip method, of class FeatureTypeFile. moved to the factory
	 */
	@Test
	@Ignore
	public void testCreateWorkLocationForZip() throws Exception {
		System.out.println("createWorkLocationForZip");
		File zipFile = null;
		FeatureTypeFile instance = null;
		File expResult = null;
//		File result = instance.createWorkLocationForZip(zipFile);
//		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of updateFileMapWithDirFile method, of class FeatureTypeFile.
	 */
	@Test
	public void testUpdateFileMapWithDirFile() throws IOException {
		System.out.println("updateFileMapWithDirFile");
		File zipFile = null;
		assertNotNull(validShapeZip);
		if (validShapeZip.exists())
		zipFile = gov.usgs.cida.owsutils.commons.io.FileHelper.flattenZipFile(validShapeZip);		
	
		// saveZipFile ....
		File workLocation = createWorkLocationForZip(zipFile);
		System.out.println("Work location created for zip: " + workLocation);
		FileHelper.unzipFile(workLocation.getAbsolutePath(), zipFile);
		FileHelper.renameDirectoryContents(workLocation);
		// result setDirectory
		//first does super
		if (!workLocation.exists()) {
			throw new FileNotFoundException();
		}

		if (!workLocation.isDirectory()) {
			throw new IOException("File at " + workLocation.getAbsolutePath() + " is not a directory");
		}
		
	}

	/**
	 * Test of saveZipFile method, of class FeatureTypeFile.
	 */
	@Test
	public void testSaveZipFile() throws Exception {
		System.out.println("saveZipFile");
		File zipFile = null;
		assertNotNull(validShapeZip);
		if (validShapeZip.exists())
		zipFile = gov.usgs.cida.owsutils.commons.io.FileHelper.flattenZipFile(validShapeZip);
		
		//File result = instance.saveZipFile(validShapeZip);
	
		// saveZipFile ....
		File workLocation = createWorkLocationForZip(zipFile);
		System.out.println("Work location created for zip: " + workLocation);
		FileHelper.unzipFile(workLocation.getAbsolutePath(), zipFile);
		// show the names of the contents pre-rename ...
		Collection<File> files = FileHelper.getFileCollection(workLocation.getAbsolutePath(), false);
		
		System.out.println("---------------------------------------------------------------------------------------------");
		for (File file : files)
		{
			//System.out.println("Files absolute name: " + file.getName());
			System.out.println("Files absolute path: " + file.getAbsolutePath());
		}
		// rename the contents with the name of the dir + the ext as name
		FileHelper.renameDirectoryContents(workLocation);
		Collection<File> files2 = FileHelper.getFileCollection(workLocation.getAbsolutePath(), false);
		for (File file : files2)
		{
			//System.out.println("POST RENAME: Files absolute name: " + file.getName());
			System.out.println("POST RENAME: Files absolute path: " + file.getAbsolutePath());
		}
		System.out.println("---------------------------------------------------------------------------------------------");
		System.out.println("saveZip sends back the worklocation: ie the exploded zip directory and the contents renamed:" + workLocation.getAbsolutePath());
		
	}

		protected File createWorkLocationForZip(File zipFile) throws IOException {
		String featureTypeFileName = FilenameUtils.getBaseName(zipFile.getName());
		System.out.println("BaseName of Zip file is: " + featureTypeFileName);
		File fileWorkDirectory = new File(workDir, featureTypeFileName);
		if (fileWorkDirectory.exists()) {
			try {
				FileUtils.cleanDirectory(fileWorkDirectory);
			} catch (IOException ex) {
				System.out.println("Could not clean work directory at " + fileWorkDirectory.getAbsolutePath());
			}
		}
		FileUtils.forceMkdir(fileWorkDirectory);
		return fileWorkDirectory;
	}

	/**
	 * Test of setDirectory method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testSetDirectory() throws Exception {
		System.out.println("setDirectory");
		File directory = null;
		FeatureTypeFile instance = null;
		String expResult = "";
	//	String result = instance.setDirectory(directory);
	//	assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getDirectory method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testGetDirectory() {
		System.out.println("getDirectory");
		String token = "";
		FeatureTypeFile instance = null;
		File expResult = null;
	//	File result = instance.getDirectory(token);
	//	assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of deleteDirectory method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testDeleteDirectory() {
		System.out.println("deleteDirectory");
		FeatureTypeFile instance = null;
		boolean expResult = false;
//		boolean result = instance.deleteDirectory();
//		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of clear method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testClear() {
		System.out.println("clear");
		FeatureTypeFile instance = null;
		boolean expResult = false;
		boolean result = instance.clear();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of exists method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testExists() {
		System.out.println("exists");
		FeatureTypeFile instance = null;
		boolean expResult = false;
		boolean result = instance.exists();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getRequiredFiles method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testGetRequiredFiles() {
		System.out.println("getRequiredFiles");
		FeatureTypeFile instance = null;
		List<File> expResult = null;
		List<File> result = instance.getRequiredFiles();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getOptionalFiles method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testGetOptionalFiles() {
		System.out.println("getOptionalFiles");
		FeatureTypeFile instance = null;
		List<File> expResult = null;
		List<File> result = instance.getOptionalFiles();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of validate method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testValidate() throws Exception {
		System.out.println("validate");
		FeatureTypeFile instance = null;
		boolean expResult = false;
//		boolean result = instance.validate();
//		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of setFileMap method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testSetFileMap() throws Exception {
		System.out.println("setFileMap");
		FeatureTypeFile instance = null;
		Map<String, String> expResult = null;
	//	Map<String, String> result = instance.setFileMap();
	//	assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getEPSGCode method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testGetEPSGCode() throws Exception {
		System.out.println("getEPSGCode");
		FeatureTypeFile instance = null;
		String expResult = "";
		String result = instance.getEPSGCode();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getColumns method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testGetColumns() throws Exception {
		System.out.println("getColumns");
		FeatureTypeFile instance = null;
		List<String> expResult = null;
		String[] result = instance.getColumns();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of hashCode method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testHashCode() {
		System.out.println("hashCode");
		FeatureTypeFile instance = null;
		int expResult = 0;
		int result = instance.hashCode();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of equals method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testEquals() {
		System.out.println("equals");
		Object obj = null;
		FeatureTypeFile instance = null;
		boolean expResult = false;
		boolean result = instance.equals(obj);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of setDSASProcess method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testSetDSASProcess() {
		System.out.println("setDSASProcess");
		DSASProcess process = null;
		FeatureTypeFile instance = null;
		instance.setDSASProcess(process);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of updateProcessInformation method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testUpdateProcessInformation() {
		System.out.println("updateProcessInformation");
		String string = "";
		FeatureTypeFile instance = null;
		instance.updateProcessInformation(string);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of importToDatabase method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testImportToDatabase() throws Exception {
		System.out.println("importToDatabase");
		Map<String, String> columns = null;
		String workspace = "";
		FeatureTypeFile instance = null;
		String expResult = "";
		String result = instance.importToDatabase(columns, workspace);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of importToGeoserver method, of class FeatureTypeFile.
	 */
	@Test
	@Ignore
	public void testImportToGeoserver() throws Exception {
		System.out.println("importToGeoserver");
		String viewName = "";
		String workspace = "";
		FeatureTypeFile instance = null;
		instance.importToGeoserver(viewName, workspace);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}
	
	/**
	 * Test getting a simple property string.
	 */
	@Test
	public void testGetSimpleTestProperties() throws Exception {
		System.out.println("testGetSimpleTestProperties");
		String value = PropertyUtil.getProperty("debug");
		System.out.println("Value of property debug is: " + value);
	}
	
		/**
	 * Add enums in package gov.usgs.cida.dsas.utilities.properties; 
	 */
	@Test
	public void testGetTestProperties() throws Exception {
		System.out.println("testGetTestProperties");
		String value = PropertyUtil.getProperty(Property.DEBUG);
		System.out.println("Value of property debug is: " + value);
	}
	
	
	public class FeatureTypeFileImpl extends FeatureTypeFile {

		public FeatureTypeFileImpl() throws Exception {
			//super(null);
		}

		public List<File> getRequiredFiles() {
			return null;
		}

		public List<File> getOptionalFiles() {
			return null;
		}

		public boolean validate() throws IOException {
			return false;
		}

		public Map<String, String> setFileMap() throws IOException {
			return null;
		}

		public String getEPSGCode() throws IOException, FactoryException {
			return "";
		}

		public String[] getColumns() throws IOException {
			return null;
		}

		public int hashCode() {
			return 0;
		}

		public boolean equals(Object obj) {
			return false;
		}

		public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
			return "";
		}

		public void importToGeoserver(String viewName, String workspace) throws IOException {
		}
	}
	
}
