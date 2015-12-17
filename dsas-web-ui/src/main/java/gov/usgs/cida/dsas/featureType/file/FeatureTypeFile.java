package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
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
 * @author smlarson
 */
public abstract class FeatureTypeFile {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureTypeFile.class);
	protected File featureTypeExplodedZipFileLocation;
	protected Map<String, File> fileMap;  
	protected GeoserverDAO geoserverHandler = null;
	protected FeatureTypeFileDAO dao = null;
	protected DSASProcess process = null;
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

	public abstract String getEPSGCode() throws IOException, FactoryException;

	public abstract String[] getColumns() throws IOException; // these are the column names found in the DBF file. 

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
	 * @param workspace
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
	public abstract String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException, AttributeNotANumberException;

	/**
	 * Imports the view as a layer in Geoserver
	 *
	 * @param viewName
	 * @param workspace
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
