package gov.usgs.cida.coastalhazards.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShapefileImportServiceTest {

	private static final String PTS_SUFFIX = "_pts";
	private File baseFile;
	private File otherFile;
	private File uploadDestinationDirectory;
	
	private void writeFile(File x) throws Exception {
		FileOutputStream fos = new FileOutputStream(x);
		try {
			fos.write("Hello\n".getBytes());
		} finally {
			fos.close();
		}
	}
	
	@Before
	public void setup() throws Exception {
        String destinationDirectoryChild = UUID.randomUUID().toString();
        File uploadDirectory = Files.createTempDirectory(getClass().getName()).toFile();
        uploadDestinationDirectory = new File(uploadDirectory, destinationDirectoryChild);
        FileUtils.forceMkdir(uploadDestinationDirectory);

        baseFile = new File(uploadDestinationDirectory, "test.zip");
        writeFile(baseFile);
        
        otherFile = new File(uploadDestinationDirectory, "test" + PTS_SUFFIX +".zip");
        writeFile(otherFile);
        		
	}
	
	@Test
	public void testZiplist() throws Exception {
        
        FileFilter zipFileFilter = new WildcardFileFilter("*.zip");

        // Upload directory might contain both input shapefile and shapefile_pts
        File[] zipFiles = uploadDestinationDirectory.listFiles(zipFileFilter);
        File ptsFile = null;
        File shapeFile = null;
        for (File file : zipFiles) {
        	if (file.getName().endsWith(PTS_SUFFIX + ".zip")) {
        		ptsFile = file;
        	} else {
        		shapeFile = file;
        	}
        }

        assertNotNull("base file", shapeFile);
        assertEquals(baseFile, shapeFile);
        assertNotNull("pts file", ptsFile);
        assertEquals(otherFile, ptsFile);
	}
	
	@After
	public void teardown() throws Exception {
		if (uploadDestinationDirectory != null) {
			File base = uploadDestinationDirectory.getAbsoluteFile().getParentFile();
			FileUtils.forceDelete(base);
		}
	}

}
