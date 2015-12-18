package gov.usgs.cida.dsas.service.util;

import gov.usgs.cida.dsas.featureTypeFile.exception.ShapefileException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
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
	//static final String DBF = "dbf";
	//static final String SHP = "shp";

	private static File getDbfFile(File unzippedShapefileLocation) throws FileNotFoundException, IOException {

		String[] DbfType = new String[]{ShpFileType.DBF.extension};
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
	 * @param validShapeDir a valid shape zip file
	 * @return A list of attribute names found in the DBF file
	 * @throws java.io.IOException
	 */
	public static List<String> getDbfColumnNames(File validShapeDir) throws IOException {
		List<String> names = null;
		DbaseFileReader dbReader = null;
		try {
			dbReader = new DbaseFileReader(FileUtils.openInputStream(getDbfFile(validShapeDir)).getChannel(), false, Charset.forName("UTF-8"));
			int n = dbReader.getHeader().getNumFields();
			names = new ArrayList<>(n);

			for (int i = 0; i < n; i++) {
				names.add(dbReader.getHeader().getFieldName(i));
			}
		
		} finally {
			if (dbReader != null){
				dbReader.close();
			}
		}
		return names;
	}

	/**
	 *
	 * @param validShapeDir the shape file zip Only for files that have a shp
	 * file ie not Lidar
	 * @return A list of attribute names found in the DBF file
	 * @throws java.io.IOException
	 */
	public static String getEPSGCode(File validShapeDir) throws IOException {

		String eCode = null;
		DataStore ds = null;
		SimpleFeatureSource featureSource = null;
		try{
		ds = getDatastore(validShapeDir);
		featureSource = ds.getFeatureSource(ds.getTypeNames()[0]);
		eCode = featureSource.getBounds().getCoordinateReferenceSystem().getName().toString();
		
		} finally {
			if (ds != null){
				ds.dispose();
			}
		}

		return eCode;
	}

	public static boolean isValidShapefile(File candidateShapeDir) throws ShapefileException {
		boolean result = true;

		String[] ShpType = new String[]{ShpFileType.SHP.extension};  // can be any file that is expected to be part of the shape file
		//find the shp file
		Collection<File> files = FileUtils.listFiles(candidateShapeDir, ShpType, false);
		if (files.isEmpty()) {
			throw new ShapefileException("Not a valid shape file. SHP file missing.");
		}
		File foundShpFile = files.iterator().next();
		ShpFiles sFile = null;

		try {
			// create the geotools shape file by passing in the found shp file
			sFile = new ShpFiles(foundShpFile);
		} catch (MalformedURLException ex) {
			Logger.getLogger(ShapeFileUtil.class.getName()).log(Level.SEVERE, null, ex);
			throw new ShapefileException("Validate of shape file requires dir path to unzipped location. Zip sent instead: " + candidateShapeDir.toString());
		}
		boolean booShp = sFile.exists(ShpFileType.SHP);
		boolean booDbf = sFile.exists(ShpFileType.DBF);
		boolean booShx = sFile.exists(ShpFileType.SHX);
		boolean booPrj = sFile.exists(ShpFileType.PRJ);

		if (!booShp || !booDbf || !booShx || !booPrj) {
			result = false;
			throw new ShapefileException("Invalid shape file zip does not have required file. Types required: shp, dbf, shx, prj " + booShp + ", " + booDbf + ", " + booShx + ", " + booPrj);
		}

		return result;
	}

	public static Map<ShpFileType, String> getFileMap(File validShapeDir) throws IOException { // exploded dir

		String[] DbfType = new String[]{ShpFileType.DBF.extension};  // can be any file that is expected to be part of the shape file
		File foundDbfFile = null;
		//find the shp file
		Collection<File> files = FileUtils.listFiles(validShapeDir, DbfType, false);
		if (null != files && !(files.isEmpty())) {
			foundDbfFile = files.iterator().next();
		}

		// create the geotools shape file by passing in the found shp file
		ShpFiles sFile = new ShpFiles(foundDbfFile);

		return sFile.getFileNames();
	}


}
