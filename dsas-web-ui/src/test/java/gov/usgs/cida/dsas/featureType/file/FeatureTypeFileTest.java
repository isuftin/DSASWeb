package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class FeatureTypeFileTest {
	
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
	 * Test getting a simple property string.
	 */
	@Test
	@Ignore
	public void testGetSimpleTestProperties() throws Exception {
		System.out.println("testGetSimpleTestProperties");
		String value = PropertyUtil.getProperty("debug");
		System.out.println("Value of property debug is: " + value);
	}
	
		/**
	 * Add enums in package gov.usgs.cida.dsas.utilities.properties; 
	 */
	@Test
	@Ignore
	public void testGetTestProperties() throws Exception {
		System.out.println("testGetTestProperties");
		String value = PropertyUtil.getProperty(Property.DEBUG);
		System.out.println("Value of property debug is: " + value);
	}
	
}
