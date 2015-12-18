package gov.usgs.cida.dsas.shoreline.file;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.utilities.features.Constants;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFile;
/**
 *
 * @author isuftin
 */
public abstract class ShorelineFile extends FeatureTypeFile implements IShorelineFile {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineFile.class);
	protected File baseDirectory;
	protected File uploadDirectory;
	protected File workDirectory;
	public static final String[] AUXILLARY_ATTRIBUTES = new String[]{
		Constants.SURVEY_ID_ATTR,
		Constants.DISTANCE_ATTR,
		Constants.DEFAULT_D_ATTR,
		Constants.NAME_ATTR,
		Constants.BIAS_UNCY_ATTR,
		Constants.MHW_ATTR
	};

	public static enum ShorelineType {

		LIDAR, SHAPEFILE, OTHER
	};

	@Override
	public String importToDatabase(HttpServletRequest request, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException{
		String columnsString = request.getParameter("columns");
		Map<String, String> columns = new HashMap<>();
		if (StringUtils.isNotBlank(columnsString)) {
			columns = new Gson().fromJson(columnsString, Map.class);
		}
		
		String result = importToDatabase(columns, workspace);
		
		new PostgresDAO().optimizeTables();
		
		return result;
	}

	@Override 
	public abstract String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException;

	@Override
	public void importToGeoserver(String viewname, String workspace) throws IOException {
		if (!geoserverHandler.createWorkspaceInGeoserver(workspace, null)) {
			throw new IOException("Could not create workspace");
		}

		if (!geoserverHandler.createPGDatastoreInGeoserver(workspace, "shoreline", null, ShorelineFileDAO.DB_SCHEMA_NAME)) {
			throw new IOException("Could not create data store");
		}

		if (!geoserverHandler.createLayerInGeoserver(workspace, "shoreline", viewname)) {
			throw new IOException("Could not create shoreline layer");
		}

		if (geoserverHandler.touchWorkspace(workspace)) {
			LOGGER.debug("Geoserver workspace {} updated", workspace);
		} else {
			LOGGER.debug("Geoserver workspace {} could not be updated", workspace);
		}
	}

	@Override
	public boolean exists() {
		for (File file : this.fileMap.values()) {
			if (file == null || !file.exists()) {
				return false;
			}
		}
		return true;
	}

	@Override
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

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();
	
	@Override
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

}
