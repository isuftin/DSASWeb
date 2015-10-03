package gov.usgs.cida.coastalhazards.service.util;

import gov.usgs.cida.coastalhazards.shoreline.exception.LidarFileFormatException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;

public class LidarFileUtils {

	/**
	 * A lidar file is a csv with 5 columns
	 *
	 * returns true if a structurally correct lidar zip file is found
	 *
	 * @throws IOException
	 */
	public static boolean isLidar(File shorelineFile) throws IOException {
		boolean isLidar = false;

		isLidar = shorelineFile.getAbsolutePath().endsWith(".csv");

		return isLidar;
	}

	/**
	 * A lidar file has csv files, a prj file, and NO shp files.
	 *
	 * returns true if a structurally correct lidar zip file is found
	 *
	 * @param lidarZipFile
	 * @throws
	 * gov.usgs.cida.coastalhazards.shoreline.exception.LidarFileFormatException
	 * @throws IOException
	 */
	public static void validateLidarFileZip(File lidarZipFile) throws LidarFileFormatException, IOException {
		File temporaryDirectory = new File(FileHelper.getTempDirectory(), UUID.randomUUID().toString() + "-deleteme");
		try {
			if (!temporaryDirectory.mkdirs()) {
				throw new IOException("Could not create temporary directory (" + temporaryDirectory.getCanonicalPath() + ") for processing");
			}

			ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(lidarZipFile)));
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				String currentExtension = entry.getName();
				// We want to skip past directories, hidden files and metadata files (MACOSX ZIPPING FIX)
				if (!entry.isDirectory()
						&& !currentExtension.startsWith(".")
						&& !currentExtension.contains(File.separator + ".")) {
					File currentFile = new File(temporaryDirectory, currentExtension);

					FileOutputStream fos = null;
					try {
						currentFile.createNewFile();
						fos = new FileOutputStream(currentFile);
						IOUtils.copy(zipInputStream, fos);
					} catch (IOException ioe) {
						// This usually occurs because this file is inside of another dir
						// so skip this file. Shapefiles inside with arbitrary directory 
						// depth should first be preprocessed to be single-depth since 
						// GS will not accept it otherwise
					} finally {
						IOUtils.closeQuietly(fos);
					}
				}
				System.gc();
			}
			IOUtils.closeQuietly(zipInputStream);

			File[] csvfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{"csv"}), false).toArray(new File[0]);
			if (csvfiles.length == 0 || csvfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one csv file");
			}
			File[] prjfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{"prj"}), false).toArray(new File[0]);
			if (prjfiles.length == 0 || prjfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one prj file");
			}
			File[] shpfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{"shp"}), false).toArray(new File[0]);
			if (shpfiles.length != 0) {
				throw new LidarFileFormatException("Lidar archive cannot contain an shp file");
			}
		} finally {
			FileHelper.forceDelete(temporaryDirectory);
		}
	}

	public static void validateHeaderRow(String[] headerRow) throws LidarFileFormatException {
		if (headerRow.length != 5) {
			throw new LidarFileFormatException("Lidar csv file has wrong number of header columns");
		}
		if (!headerRow[0].equalsIgnoreCase("segment_id")) {
			throw new LidarFileFormatException("Lidar csv does not have segment_id as first column");
		}
		if (!headerRow[1].equalsIgnoreCase("x")) {
			throw new LidarFileFormatException("Lidar csv does not have x as second column");
		}
		if (!headerRow[2].equalsIgnoreCase("y")) {
			throw new LidarFileFormatException("Lidar csv does not have y as third column");
		}
		if (!headerRow[3].equalsIgnoreCase("uncy_")) {
			throw new LidarFileFormatException("Lidar csv does not have uncy_ as fourth column");
		}
		if (!headerRow[4].equalsIgnoreCase("Date_")) {
			throw new LidarFileFormatException("Lidar csv does not have Date_ as fifth column");
		}
	}

	public static void validateDataRow(String[] dataRow) throws LidarFileFormatException {
		if (dataRow.length != 5) {
			throw new LidarFileFormatException("Lidar csv file has wrong number of columns in one of the rows");
		}
	}
}
