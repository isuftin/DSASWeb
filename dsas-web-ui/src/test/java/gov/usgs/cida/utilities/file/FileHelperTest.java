package gov.usgs.cida.utilities.file;

import java.util.zip.ZipEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FileHelperTest {

	@Test
	public void testEntryIsMacBundle() {
		ZipEntry ze;
		
		ze = new ZipEntry(".hidden_thing");
		assertTrue(ze.getName(),FileHelper.entryIsHidden(ze));
		
		ze = new ZipEntry(".hidden_dir/");
		assertTrue(ze.getName(),FileHelper.entryIsHidden(ze));

		ze = new ZipEntry("SOME_MACOSX_BUNDLE/");
		assertTrue(ze.isDirectory());
		
		assertTrue(ze.getName(),FileHelper.entryIsHidden(ze));
		
		ze = new ZipEntry("Another_MacOSX_Bundle/");
		assertTrue(ze.getName(),FileHelper.entryIsHidden(ze));
		
		ze = new ZipEntry("NOT_MACOSX_BUNDLE");
		assertFalse(ze.getName(),FileHelper.entryIsHidden(ze));
		
		ze = new ZipEntry("unhidden_thing");
		assertFalse(ze.getName(),FileHelper.entryIsHidden(ze));

		ze = new ZipEntry("unhidden_dir/");
		assertFalse(ze.getName(),FileHelper.entryIsHidden(ze));
		
	}

}
