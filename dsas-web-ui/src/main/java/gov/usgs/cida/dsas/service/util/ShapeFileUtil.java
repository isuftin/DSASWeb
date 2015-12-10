package gov.usgs.cida.dsas.service.util;

import gov.usgs.cida.dsas.featureTypeFile.exception.FeatureTypeFileException;
import gov.usgs.cida.dsas.featureTypeFile.exception.PdbFileFormatException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShapefileException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;

import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class ShapeFileUtil {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapeFileUtil.class);
	static final String DBF = "dbf";
	static final String SHP = "shp";

	private static File getDbfFile(File unzippedShapefileLocation) throws FileNotFoundException, IOException {

		String[] DbfType = new String[]{DBF};
		Collection<File> dbfFiles = FileUtils.listFiles(unzippedShapefileLocation, DbfType, true);
		if (1 != dbfFiles.size() | dbfFiles.isEmpty()) {
			throw new FileNotFoundException("Unable to get dbf file. Missing from location:" + unzippedShapefileLocation + " or more than one found. Size: " + dbfFiles.size());
		}
		File dbfFile = dbfFiles.iterator().next();
		if (null == dbfFile | !dbfFile.canRead() | !dbfFile.exists() | !dbfFile.isFile()) {
			throw new IOException(MessageFormat.format("Unable to read dbfFile found in zip.", dbfFile));
		}

		return dbfFile;
	}

	/**
	 * Must remember to close the data store if using this method. Convenience
	 * method below- close
	 *
	 * @param File the shape file's location found in the unzip directory ie
	 * pass in the directory of where the unzipped files are located
	 * @return DataStore - the shape file's data store
	 */
	private static DataStore getDatastore(File unZippedShapefileLocation) throws MalformedURLException, IOException {

		Map<String, Object> map = new HashMap<>(1);
		map.put("url", unZippedShapefileLocation.toURI().toURL());
		DataStore ds = DataStoreFinder.getDataStore(map);
		return ds;
	}

	/**
	 * Pass a valid shape zip into the method.
	 *
	 * @param validShapeZip a valid shape zip file
	 * @return A list of attribute names found in the DBF file
	 */
//	public static List<String> getDbfColumnNames(File validShapeZip) throws IOException {
//		List<String> names = new ArrayList<String>();
//		File tempDirectory = Files.createTempDirectory("temp-shapefile-dir").toFile();
//		tempDirectory.deleteOnExit();
//		FileHelper.unzipFile(tempDirectory.toString(), validShapeZip);
//
//		DbaseFileReader dbReader = new DbaseFileReader(FileUtils.openInputStream(getDbfFile(tempDirectory)).getChannel(), false, Charset.forName("UTF-8"));
//		int n = dbReader.getHeader().getNumFields();
//		for (int i = 0; i < n; i++) {
//			names.add(dbReader.getHeader().getFieldName(i));
//		}
//		closeReader(dbReader);
//
//		return names;
//	}
	/**
	 * Pass a valid shape zip into the method.
	 *
	 * @param validShapeZip a valid shape zip file
	 * @return A list of attribute names found in the DBF file
	 */
	public static List<String> getDbfColumnNames(File validShapeDir) throws IOException {
		List<String> names = new ArrayList<String>();

		DbaseFileReader dbReader = new DbaseFileReader(FileUtils.openInputStream(getDbfFile(validShapeDir)).getChannel(), false, Charset.forName("UTF-8"));
		int n = dbReader.getHeader().getNumFields();
		for (int i = 0; i < n; i++) {
			names.add(dbReader.getHeader().getFieldName(i));
		}
		closeReader(dbReader);

		return names;
	}
	/**
	 *
	 * @param validShapeZip the shape file zip Only for files that have a shp
	 * file ie not Lidar
	 * @return A list of attribute names found in the DBF file
	 */
//	public static String getEPSGCode(File validShapeZip) throws IOException {
//		// To control the directory structure, make a copy of the file and unzip its contents into a known directory
//		File tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();  //#TODO# will need to get file location from props
//		tempDir.deleteOnExit();
//		File tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
//		tempShapeFile.deleteOnExit();
//		FileUtils.copyFile(validShapeZip, tempShapeFile);
//		FileHelper.unzipFile(tempDir.getAbsolutePath(), validShapeZip);
//
//		String eCode = null;
//		SimpleFeatureSource featureSource = null;
//		DataStore ds = getDatastore(tempDir);
//		featureSource = ds.getFeatureSource(ds.getTypeNames()[0]);
//		eCode = featureSource.getBounds().getCoordinateReferenceSystem().getName().toString();
//		closeDataStore(ds);
//
//		return eCode;
//	}
	/**
	 *
	 * @param validShapeZip the shape file zip Only for files that have a shp
	 * file ie not Lidar
	 * @return A list of attribute names found in the DBF file
	 */
	public static String getEPSGCode(File validShapeDir) throws IOException { 

		String eCode = null;
		SimpleFeatureSource featureSource = null;
		DataStore ds = getDatastore(validShapeDir);
		featureSource = ds.getFeatureSource(ds.getTypeNames()[0]);
		eCode = featureSource.getBounds().getCoordinateReferenceSystem().getName().toString();
		closeDataStore(ds);

		return eCode;
	}
//	public static boolean isValidShapeZip(File candidateShapeZip) throws IOException
//	{
//		boolean result = true;
//				// To control the directory structure, make a copy of the file and unzip its contents into a known directory
//		File tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();  //#TODO# will need to get file location from props
//		tempDir.deleteOnExit();
//		File tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
//		tempShapeFile.deleteOnExit();
//		FileUtils.copyFile(candidateShapeZip, tempShapeFile);
//		FileHelper.unzipFile(tempDir.getAbsolutePath(), candidateShapeZip);
//		
//		String[] DbfType = new String[]{DBF};  // can be any file that is expected to be part of the shape file
//		//find the shp file
//		Collection<File> files = FileUtils.listFiles(tempDir, DbfType, false);
//		File foundDbfFile = files.iterator().next();
//		
//		// create the geotools shape file by passing in the found shp file
//		ShpFiles sFile = new ShpFiles(foundDbfFile);
//		boolean booShp = sFile.exists(ShpFileType.SHP);
//		boolean booDbf = sFile.exists(ShpFileType.DBF);
//		boolean booShx = sFile.exists(ShpFileType.SHX);
//		
//		if (!booShp || !booDbf || !booShx)
//		{
//			result = false;
//			throw new FileNotFoundException("Invalid shape file zip does not have required file types: shp, dbf, shx " + booShp + ", " + booDbf + ", " + booShx);
//		}
//		// TODO add test for max file size ...
//		return result;
//	}
	
		public static boolean isValidShapefile(File candidateShapeDir) throws ShapefileException
	{
		boolean result = true;

		String[] ShpType = new String[]{SHP};  // can be any file that is expected to be part of the shape file
		//find the shp file
		Collection<File> files = FileUtils.listFiles(candidateShapeDir, ShpType, false);
		if (files.isEmpty())
			throw new ShapefileException("Not a valid shape file. SHP file missing.");
		File foundDbfFile = files.iterator().next();
		ShpFiles sFile = null;
		
		try {
			// create the geotools shape file by passing in the found shp file
			sFile = new ShpFiles(foundDbfFile);
		} catch (MalformedURLException ex) {
			Logger.getLogger(ShapeFileUtil.class.getName()).log(Level.SEVERE, null, ex);
			throw new ShapefileException("Validate of shape file requires dir path to unzipped location. Zip sent instead: " + candidateShapeDir.toString());
		}
		boolean booShp = sFile.exists(ShpFileType.SHP);
		boolean booDbf = sFile.exists(ShpFileType.DBF);
		boolean booShx = sFile.exists(ShpFileType.SHX);
		boolean booPrj = sFile.exists(ShpFileType.PRJ);
		
		if (!booShp || !booDbf || !booShx )
		{
			result = false;
			throw new ShapefileException("Invalid shape file zip does not have required file. Types required: shp, dbf, shx, prj " + booShp + ", " + booDbf + ", " + booShx + ", " + booPrj);
		}
		
		return result;
	}
	/**
	 *
	 * @param validShapeZip the shape file dir. Only for files that have a shp
	 * file ie not Lidar
	 * @return A list of attribute names found in the DBF file
	 */
//	public static Map<ShpFileType, String> getFileMap(File validShapeZip) throws IOException {
//		// To control the directory structure, make a copy of the file and unzip its contents into a known directory
//		File tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();  //#TODO# will need to get file location from props
//		tempDir.deleteOnExit();
//		File tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
//		tempShapeFile.deleteOnExit();
//		FileUtils.copyFile(validShapeZip, tempShapeFile);
//		FileHelper.unzipFile(tempDir.getAbsolutePath(), validShapeZip);
//		
//		String[] DbfType = new String[]{DBF};  // can be any file that is expected to be part of the shape file
//		//find the shp file
//		Collection<File> files = FileUtils.listFiles(tempDir, DbfType, false);
//		File foundDbfFile = files.iterator().next();
//		
//		// create the geotools shape file by passing in the found shp file
//		ShpFiles sFile = new ShpFiles(foundDbfFile);
//		boolean booShp = sFile.exists(ShpFileType.SHP);
//		boolean booDbf = sFile.exists(ShpFileType.DBF);
//		boolean booShx = sFile.exists(ShpFileType.SHX);
//		
//		if (!booShp || !booDbf || !booShx)
//			throw new FileNotFoundException("Invalid shape file zip does not have required file types: shp, dbf, shx " + booShp + ", " + booDbf + ", " + booShx);
//
//
//		return sFile.getFileNames();
//	}
	
	public static Map<ShpFileType, String> getFileMap(File validShapeDir) throws IOException { // exploded dir

		String[] DbfType = new String[]{DBF};  // can be any file that is expected to be part of the shape file
		File foundDbfFile = null;
		//find the shp file
		Collection<File> files = FileUtils.listFiles(validShapeDir, DbfType, false);
		if(null != files && !(files.isEmpty()))
		{
			foundDbfFile = files.iterator().next();
		}
		
		// create the geotools shape file by passing in the found shp file
		ShpFiles sFile = new ShpFiles(foundDbfFile);
		boolean booShp = sFile.exists(ShpFileType.SHP);
		boolean booDbf = sFile.exists(ShpFileType.DBF);
		boolean booShx = sFile.exists(ShpFileType.SHX);
		
		if (!booShp || !booDbf || !booShx)
			throw new FileNotFoundException("Invalid shape file zip does not have required file types: shp, dbf, shx " + booShp + ", " + booDbf + ", " + booShx);

		return sFile.getFileNames();
	}
	
	public static void closeReader(DbaseFileReader dbfReader) throws IOException {
		if (dbfReader != null) {
			dbfReader.close();
		}
	}

	public static void closeDataStore(DataStore ds) {
		if (ds != null) {
			ds.dispose();
		}
	}

}
