package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 * @author isuftin
 */
public abstract class FeatureTypeFile {// implements AutoCloseable {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureTypeFile.class);
	protected File featureTypeExplodedZipFileLocation;
//	protected File baseDirectory;
//	protected File uploadDirectory;
//	protected File workDirectory;
	protected Map<String, File> fileMap;  // TODO shape files can use a util to get back the filemap, Lidar requires its own impl
	protected GeoserverDAO geoserverHandler = null;
	protected FeatureTypeFileDAO dao = null;
	protected DSASProcess process = null;
//	protected String token;
	protected FeatureType type = null;
	protected static final String SHP = "shp";
	protected static final String SHX = "shx";
	protected static final String DBF = "dbf";
	protected static final String PRJ = "prj";
	protected static final String FBX = "fbx";
	protected static final String SBX = "sbx";
	protected static final String AIH = "aih";
	protected static final String IXS = "ixs";
	protected static final String MXS = "mxs";
	protected static final String ATX = "atx";
	protected static final String SHP_XML = "shp.xml";
	protected static final String CPG = "cpg";
	protected static final String CST = "cst";
	protected static final String CSV = "csv";

	
//	public FeatureTypeFile(File featureTypeExplodedZipFileLocation) throws IOException {  //the location to the  exploded zip
//		this.featureTypeExplodedZipFileLocation = featureTypeExplodedZipFileLocation;
//		if (this.featureTypeExplodedZipFileLocation.isFile()) {
//			this.featureTypeExplodedZipFileLocation = this.featureTypeExplodedZipFileLocation.getParentFile();
//		}
//		this.baseDirectory = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, System.getProperty("java.io.tmpdir")));
//		this.uploadDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
//		this.workDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));

//	}

//	/**
//	 *
//	 * @param zipFile The intact zipFile
//	 * @return the directory location to the exploded zip with its contents all
//	 * renamed to the dir+shp.shp format
//	 * @throws IOException
//	 */
//	public File saveZipFile(File zipFile) throws IOException {
//		File workLocation = createWorkLocationForZip(zipFile);
//		FileHelper.unzipFile(workLocation.getAbsolutePath(), zipFile);
//		FileHelper.renameDirectoryContents(workLocation);
//		return workLocation;
//	}
//
//	/**
//	 * Moves a zip file into the applications work directory and returns the
//	 * parent directory containing the unzipped collection of files
//	 *
//	 * @param zipFile
//	 * @return
//	 */
//	protected File createWorkLocationForZip(File zipFile) throws IOException {
//		String featureTypeFileName = FilenameUtils.getBaseName(zipFile.getName());
//		File fileWorkDirectory = new File(this.workDirectory, featureTypeFileName);
//		if (fileWorkDirectory.exists()) {
//			try {
//				FileUtils.cleanDirectory(fileWorkDirectory);
//			} catch (IOException ex) {
//				LOGGER.debug("Could not clean work directory at " + fileWorkDirectory.getAbsolutePath(), ex);
//			}
//		}
//		FileUtils.forceMkdir(fileWorkDirectory);
//		return fileWorkDirectory;
//	}

	protected void updateFileMapWithDirFile(File directory, String[] parts) {
		Collection<File> fileList = FileUtils.listFiles(directory, parts, false);
		Iterator<File> listIter = fileList.iterator();
		while (listIter.hasNext()) {
			File file = listIter.next();
			String filename = file.getName();
			for (String part : parts) {
				if (filename.contains(part)) {
					this.fileMap.put(part, file);
				}
			}
		}
	}

	/**
	 * Deletes own files in the file system and removes parent directory
	 *
	 * @return whether parent directory has been removed
	 */
	public boolean clear() {
		boolean success = true;
		Iterator<File> iterator = this.fileMap.values().iterator();
		while (iterator.hasNext()) {
			File parentDirectory = iterator.next().getParentFile();
			success = FileUtils.deleteQuietly(parentDirectory);
		}
		if (success) {
			this.fileMap.clear();
		}
		return success;
	}

	/*
	 * Checks if underlying files exist in the file system
	 *
	 * @return
	 */
	public boolean exists() {
		for (File file : this.fileMap.values()) {
			if (file == null || !file.exists()) {
				return false;
			}
		}
		return true;
	}

	
	public abstract List<File> getRequiredFiles();

	public abstract List<File> getOptionalFiles();

	//public abstract boolean validate() throws IOException;
//	public abstract Map<String, String> setFileMap() throws IOException; // contains the unzipped files, the key is the type ie file ext
	public abstract String getEPSGCode() throws IOException, FactoryException;

	// public abstract List<String> getColumns() throws IOException; // these are the column names found in the DBF file. Use the Utils in the sub-classes : ShapeFileUtil, LidarFileUtils..
	public abstract String[] getColumns() throws IOException; // these are the column names found in the DBF file. Use the Utils in the sub-classes : ShapeFileUtil, LidarFileUtils..

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	public void setDSASProcess(DSASProcess process) {
		this.process = process;
		if (this.dao != null) {
			this.dao.setDSASProcess(process);
		}
	}

	protected void updateProcessInformation(String string) {
		if (this.process != null) {
			this.process.addProcessInformation(string);
		}
	}

	/**
	 * Imports the shoreline file into the database
	 *
	 * @param columns
	 * @return
	 * @throws ShorelineFileFormatException
	 * @throws SQLException
	 * @throws NamingException
	 * @throws NoSuchElementException
	 * @throws ParseException
	 * @throws IOException
	 * @throws SchemaException
	 * @throws TransformException
	 * @throws FactoryException
	 */
	public abstract String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException;

	/**
	 * Imports the view as a layer in Geoserver
	 *
	 * @param viewName
	 * @throws IOException
	 */
	public abstract void importToGeoserver(String viewName, String workspace) throws IOException;

	/*
	 * Returns the type of FeatureType. Pdb, ShorelineShape, ShorlineLidar etc 
	 *
	 * @return FeatureType
	 */
	protected FeatureType getType() {
		return this.type;

	}

	/*
	 * Returns the type of FeatureType this zip is
	 *
	 * @param FeatureType 
	 */
	protected void setType(FeatureType type) {
		this.type = type;

	}
	
		
}
