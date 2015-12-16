package gov.usgs.cida.dsas.dao.shoreline;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.uncy.PostGISJNDIOutputXploder;
import gov.usgs.cida.dsas.uncy.Xploder;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Imports a shapefile into the databaseF
 *
 * @author isuftin
 */
public class ShorelineShapefileDAO extends ShorelineFileDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineShapefileDAO.class);

	public ShorelineShapefileDAO() {
		this.JNDI_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
	}

	/**
	 * Initialize (or update) the published workspace by touching the view
	 *
	 * @return
	 * @throws java.sql.SQLException
	 * @see ShorelineFileDao#createViewAgainstWorkspace(java.sql.Connection,
	 * java.lang.String)
	 */
	public String createViewAgainstPublishedWorkspace() throws SQLException {
		try (Connection connection = getConnection()) {
			return createViewAgainstWorkspace(connection, GeoserverDAO.PUBLISHED_WORKSPACE_NAME);
		}
	}

	@Override
	public String importToDatabase(File shpFile, Map<String, String> columns, String workspace, String EPSGCode) throws SQLException, NamingException, NoSuchElementException, ParseException, IOException {
		String viewName;
		BidiMap bm = new DualHashBidiMap(columns);
		String dateFieldName = (String) bm.getKey(Constants.DB_DATE_ATTR);
		String uncertaintyFieldName = (String) bm.getKey(Constants.UNCY_ATTR);
		String mhwFieldName = (String) bm.getKey(Constants.MHW_ATTR);
		String orientation = null; // Not yet sure what to do here
		String baseFileName = FilenameUtils.getBaseName(shpFile.getName());
		String name = baseFileName;
		File parentDirectory = shpFile.getParentFile();
		deleteExistingPointFiles(parentDirectory);

		Map<String, Object> config = new HashMap<>();
		config.put(PostGISJNDIOutputXploder.JNDI_PARAM, String.format("java:comp/env/%s", PropertyUtil.getProperty(Property.JDBC_NAME)));
		config.put(PostGISJNDIOutputXploder.INCOMING_DATEFIELD_NAME_PARAM, dateFieldName);
		config.put(PostGISJNDIOutputXploder.INCOMING_MHW_NAME_PARAM, mhwFieldName);
		config.put(PostGISJNDIOutputXploder.INCOMING_ORIENTATION_NAME_PARAM, orientation);
		config.put(PostGISJNDIOutputXploder.INCOMING_NAME_NAME_PARAM, name);
		config.put(PostGISJNDIOutputXploder.WORKSPACE_PARAM, workspace);
		config.put(Xploder.UNCERTAINTY_COLUMN_PARAM, uncertaintyFieldName);
		config.put(Xploder.INPUT_FILENAME_PARAM, shpFile.getAbsolutePath());

		//TODO- Check if incoming shapefile is not already a points shapefile
		LOGGER.debug("Exploding shapefile at {}", config.get(Xploder.INPUT_FILENAME_PARAM));
		updateProcessInformation("Exploding shapefile");
		int pointCount = 0;
		try (Xploder xploder = new PostGISJNDIOutputXploder(config)) {
			pointCount = xploder.explode();
			LOGGER.debug("Shapefile exploded");
			updateProcessInformation(String.format("Shapefile exploded to %s points", pointCount));
		} catch (Exception ex) {
			LOGGER.warn("There was an issue during exploding the Shapefile to points", ex);
		}
		if (pointCount == 0) {
			throw new SQLException("Could not write shapefile to database");
		}

		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			viewName = createViewAgainstWorkspace(connection, workspace);
			if (StringUtils.isBlank(viewName)) {
				throw new SQLException("Could not create view");
			}
			new PostgresDAO().addViewToMetadataTable(connection, viewName);
			connection.commit();
		}

		return viewName;
	}

	private Collection<File> deleteExistingPointFiles(File directory) {
		Collection<File> existingPointFiles = FileUtils.listFiles(directory, new PrefixFileFilter("*_pts"), null);
		existingPointFiles.stream().forEach((existingPtFile) -> {
			existingPtFile.delete();
		});
		return existingPointFiles;
	}



}
