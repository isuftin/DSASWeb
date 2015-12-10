package gov.usgs.cida.dsas.service.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.commons.io.FileUtils;

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
public class TokenFileExchangerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenFileExchangerTest.class);
    private static final String tempDir = System.getProperty("java.io.tmpdir");
    
    private static File workDir;
    private static File fileshp;


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

        // Create the four required files for the shape zip file. All have the same file name with different suffixes.
        fileshp = new File(workDir, "testFile.shp");  // .shp
        fileshp.createNewFile();

    }


    @Test
        public void testGetTokenForFile() throws Exception {
        LOGGER.info("getTokenForFile");

        assertNotNull(fileshp);
        assertNotEquals(fileshp, "");
        LOGGER.info("files absolute path:  " + fileshp.getAbsolutePath());
		LOGGER.info("Files path:           " + fileshp.getPath());
		LOGGER.info("Files canonical path: " + fileshp.getCanonicalPath());
		LOGGER.info("Files Parent -        " + fileshp.getParent());
		
        String token = TokenFileExchanger.getToken(fileshp);
        assertNotNull(token);

        LOGGER.info("Token string is: " + token);

    }

    @Test
        public void testGetFileForToken() throws Exception {
        LOGGER.info("getFileForToken");

        // add the file first so that it can be retreived with the token in the second step of the test
        String token = TokenFileExchanger.getToken(fileshp);
        assertNotNull(token);
        assertNotEquals(fileshp, "");

        // retrieve the file
        File retrievedFile = TokenFileExchanger.getFile(token);
        assertNotNull(retrievedFile);
        assertEquals(retrievedFile.getAbsolutePath(), fileshp.getAbsolutePath());
    }

    @Test
        public void testRemoveTokenAndDeleteFile() {
        LOGGER.info("testRemoveTokenAndDeleteFile");

        String token = null;
		try {
			token = TokenFileExchanger.getToken(fileshp);
			assertNotNull(fileshp);
		} catch (FileNotFoundException ex) {
			LOGGER.info("Unable to get token.", ex);
		}
        assertNotNull("Token should not be null", token);
        assertNotEquals("Token should not be blank", token, "");

        TokenFileExchanger.removeToken(token);
        assertFalse("File should be gone", fileshp.exists());
    }

    @AfterClass
        public static void tearDownClass() {
          FileUtils.deleteQuietly(workDir);
          
    }

    @After
        public void tearDown() {

        for (File file : FileUtils.listFiles(workDir, null, true)) {
            FileUtils.deleteQuietly(file);
        }
    }

}
