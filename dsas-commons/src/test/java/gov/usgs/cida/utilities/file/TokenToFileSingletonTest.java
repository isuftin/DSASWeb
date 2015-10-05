package gov.usgs.cida.utilities.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class TokenToFileSingletonTest {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenToFileSingletonTest.class);
	private static final String tempDir = System.getProperty("java.io.tmpdir");
	private static File workDir;
	private static File file;

	public TokenToFileSingletonTest() {
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
	public void setUp() throws URISyntaxException, IOException {
		String packagePath = "/";
		FileUtils.copyDirectory(new File(getClass().getResource(packagePath).toURI()), workDir);
		file = new File(workDir, "handpts.zip");
	}

	@After
	public void tearDown() {
		for (File file : FileUtils.listFiles(workDir, null, true)) {
			FileUtils.deleteQuietly(file);
		}
	}

	@Test
	public void testAddFile() {
		LOGGER.info("addFile");

		String result = TokenToFileSingleton.addFile(file);
		assertNotNull("Token should not be null", result);
		assertNotEquals("Token should not be blank", result, "");
	}

	@Test
	public void testAddFileASecondTime() {
		LOGGER.info("testAddFileASecondTime");

		String result = TokenToFileSingleton.addFile(file);
		assertNotNull("Token should not be null", result);
		assertNotEquals("Token should not be blank", result, "");

		String secondResult = TokenToFileSingleton.addFile(file);
		assertEquals("Second result should be the same as the first", secondResult, result);
	}

	@Test
	public void testRemoveTokenAndDeleteBackingFile() {
		LOGGER.info("testRemoveTokenAndDeleteBackingFile");

		String result = TokenToFileSingleton.addFile(file);
		assertNotNull("Token should not be null", result);
		assertNotEquals("Token should not be blank", result, "");

		TokenToFileSingleton.removeToken(result, true);
		assertFalse("File should be gone", file.exists());
	}

	@Test
	public void testRemoveTokenAndDontDeleteBackingFile() {
		LOGGER.info("testRemoveTokenAndDontDeleteBackingFile");

		String result = TokenToFileSingleton.addFile(file);
		assertNotNull("Token should not be null", result);
		assertNotEquals("Token should not be blank", result, "");

		TokenToFileSingleton.removeToken(result, false);
		assertTrue("File should still exist", file.exists());
	}

	@Test
	public void testGetFile() {
		LOGGER.info("testGetFile");
		
		String token = TokenToFileSingleton.addFile(file);
		assertNotNull("Token should not be null", token);
		assertNotEquals("Token should not be blank", token, "");
		
		File retrievedFile = TokenToFileSingleton.getFile(token);
		assertNotNull("Retrieved file object should not be null", retrievedFile);
		assertTrue("Retrieved file object should exist", retrievedFile.exists());
		assertEquals("Retrieved file object should be the same as original file", retrievedFile.getAbsolutePath(), file.getAbsolutePath());
		
		retrievedFile.delete();
		retrievedFile = TokenToFileSingleton.getFile(token);
		assertFalse("Retrieved file object should no longer exist", retrievedFile.exists());
		
	}
	
}
