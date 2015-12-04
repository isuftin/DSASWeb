package gov.usgs.cida.dsas.pdb.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.pdb.PdbDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFile;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.service.util.ShapeFileUtil;
import gov.usgs.cida.dsas.shoreline.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class PdbFile extends FeatureTypeFile {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PdbFile.class);
	private static final String[] REQ_FILES = new String[]{SHP, SHX, DBF, PRJ};
	private static final String[] OPTIONAL_FILES = new String[]{SHP_XML};
	private static final String[] FILE_PARTS = new String[]{
		SHP,
		SHX,
		DBF,
		PRJ,
		FBX,
		SBX,
		AIH,
		IXS,
		MXS,
		ATX,
		CST,
		SHP_XML,
		CPG};
		
	public PdbFile(File shapefileLocation, GeoserverDAO gsHandler, PdbDAO dao, DSASProcess process) throws IOException {
		super(shapefileLocation);
		init(gsHandler, dao);
	}
	
	public PdbFile(File shapefileLocation, GeoserverDAO gsHandler, PdbDAO dao) throws IOException {
		this(shapefileLocation, gsHandler, dao, null);
	}
	
	//set up the work structures
	private void init(GeoserverDAO gsHandler, PdbDAO dao) {
		this.geoserverHandler = gsHandler;
		this.dao = dao;
		this.fileMap = new HashMap<>(FILE_PARTS.length);
	}
	
	
	@Override
	public List<File> getRequiredFiles() {
		Collection<File> requiredFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, REQ_FILES, false);
		return new ArrayList<>(requiredFiles);
	}

	@Override
	public List<File> getOptionalFiles() {
		Collection<File> requiredFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, OPTIONAL_FILES, false);
		return new ArrayList<>(requiredFiles);
	}

	@Override
	public boolean validate() throws IOException {
		ShapeFileUtil.isValidShapefile(this.featureTypeExplodedZipFileLocation); 
		return ShapeFileUtil.isValidShapefile(this.featureTypeExplodedZipFileLocation); 
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PdbFile)) {
			return false;
		}
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public void setDSASProcess(DSASProcess process) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		updateProcessInformation("Getting EPSG Code");
		String projection = getEPSGCode(); // TODO Is this reliant on a prj which may not be part of a PdbFile zip?
		File shpFile = fileMap.get(SHP);
		updateProcessInformation("Importing to database");
		return dao.importToDatabase(shpFile, columns, workspace, projection);
	}

	@Override
	public void importToGeoserver(String viewName, String workspace) throws IOException {
		if (!geoserverHandler.createWorkspaceInGeoserver(workspace, null)) {
			throw new IOException("Could not create workspace");
		}

		if (!geoserverHandler.createPGDatastoreInGeoserver(workspace, "pdb", null, PdbDAO.DB_SCHEMA_NAME)) {
			throw new IOException("Could not create data store");
		}

		if (!geoserverHandler.createLayerInGeoserver(workspace, "pdb", viewName)) {
			throw new IOException("Could not create pdb layer");
		}

		if (geoserverHandler.touchWorkspace(workspace)) {
			LOGGER.debug("Geoserver workspace {} updated", workspace);
		} else {
			LOGGER.debug("Geoserver workspace {} could not be updated", workspace);
		}
	}

//	@Override
//	public void close() throws Exception {
//		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//	}
	@Override
	public String setDirectory(File directory) throws IOException {
		String fileToken = super.setDirectory(directory);
		updateFileMapWithDirFile(directory, FILE_PARTS);
		return fileToken;
	}

	public String getEPSGCode() throws IOException, FactoryException {
		return ShapeFileUtil.getEPSGCode(this.fileMap.get(SHP));
	}

	@Override
	public List<String> getColumns() throws IOException {
		return ShapeFileUtil.getDbfColumnNames(this.fileMap.get(SHP));
	}

	@Override
	public Map<String, String> setFileMap() throws IOException {
		Map<ShpFileType, String> geoShapeUrlMap = ShapeFileUtil.getFileMap(featureTypeExplodedZipFileLocation);// this requires the dir to the exploded zip
		Map<String, String> result = new HashMap<>(); // Returns the URLs (in string form) of all the files for the shapefile datastore.
		
		for (ShpFileType key :geoShapeUrlMap.keySet())
		{
			String value = geoShapeUrlMap.get(key);
			result.put((key.extension), value);
		}
		//todo refactor later to return the Map<ShpFileType, String> in FeatureTypeFile....
		
		return result;
	}

}
