package gov.usgs.cida.dsas.dao;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public abstract class FeatureTypeFileDAO {

	public static final int DATABASE_PROJECTION = 4326;
	public static final String DB_SCHEMA_NAME = PropertyUtil.getProperty(Property.DB_SCHEMA_NAME, "public");
	public static final String[] PROTECTED_WORKSPACES = new String[]{GeoserverDAO.PUBLISHED_WORKSPACE_NAME};
	protected String JNDI_NAME;
	protected final PostgresDAO pgDao = new PostgresDAO();
	protected DSASProcess process = null;
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureTypeFileDAO.class);

	/**
	 * Retrieves a connection from the database
	 *
	 * @return
	 */
	protected Connection getConnection() {
		Connection con = null;
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			DataSource ds = (DataSource) envCtx.lookup(JNDI_NAME);
			con = ds.getConnection();
		} catch (SQLException | NamingException ex) {
			LOGGER.error("Could not create database connection", ex);
		}
		return con;
	}
	
	public void setDSASProcess(DSASProcess process) {
		this.process = process;
	}

	protected void updateProcessInformation(String string) {
		if (this.process != null) {
			this.process.addProcessInformation(string);
		}
	}
	/**
	 * Imports the shoreline file into the database. Returns the name of the
	 * view that holds this shoreline
	 *
	 * @param shorelineFile File that will be used to import into the database
	 * @param columns mapping of file columns to database required columns
	 * @param workspace the unique name of workspace to create or append to
	 * @param EPSGCode the projection code for this shoreline file
	 * @return
	 * @throws
	 * gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException
	 * @throws java.sql.SQLException
	 * @throws javax.naming.NamingException
	 * @throws java.text.ParseException
	 * @throws java.io.IOException
	 * @throws org.geotools.feature.SchemaException
	 * @throws org.opengis.referencing.FactoryException
	 * @throws org.opengis.referencing.operation.TransformException
	 */
	public abstract String importToDatabase(File shorelineFile, Map<String, String> columns, String workspace, String EPSGCode) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException;

}
