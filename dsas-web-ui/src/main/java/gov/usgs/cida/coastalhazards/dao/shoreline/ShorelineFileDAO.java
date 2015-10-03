package gov.usgs.cida.coastalhazards.dao.shoreline;

import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.coastalhazards.dao.postgres.PostgresDAO;
import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.utilities.features.Constants;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
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
 * @author isuftin
 */
public abstract class ShorelineFileDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineFileDAO.class);
	public final static int DATABASE_PROJECTION = 4326;
	public final static String[] REQUIRED_FIELD_NAMES = new String[]{Constants.DB_DATE_ATTR, Constants.UNCY_ATTR, Constants.MHW_ATTR};
	public final static String DB_SCHEMA_NAME = PropertyUtil.getProperty(Property.DB_SCHEMA_NAME, "public");
	public final static String[] PROTECTED_WORKSPACES = new String[]{GeoserverDAO.PUBLISHED_WORKSPACE_NAME};
	protected String JNDI_NAME;
	private final PostgresDAO pgDao = new PostgresDAO();

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

	/**
	 * Inserts a shoreline into the shorelines table
	 *
	 * @param connection
	 * @param workspace
	 * @param date
	 * @param mhw
	 * @param source
	 * @param name
	 * @param shorelineType
	 * @param auxillaryName
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	protected long insertToShorelinesTable(Connection connection, String workspace, Date date, boolean mhw, String source, String name, String shorelineType, String auxillaryName) throws NamingException, SQLException {
		return pgDao.insertToShorelinesTable(connection, workspace, new java.sql.Date(date.getTime()), mhw, source, name, shorelineType, auxillaryName);
	}

	protected boolean insertPointIntoShorelinePointsTable(Connection connection, long shorelineId, int segmentId, double x, double y, double uncertainty) throws IOException, SchemaException, TransformException, NoSuchElementException, FactoryException, SQLException {
		return insertPointsIntoShorelinePointsTable(connection, shorelineId, segmentId, new double[][]{new double[]{x, y, uncertainty}});
	}

	/**
	 * Inserts an array of points into the shoreline points table
	 *
	 * @param connection
	 * @param shorelineId
	 * @param segmentId
	 * @param XYuncyArray two-dimensional array of doubles and uncertainty. For
	 * the inner array, the indexes map like so:
	 * <br />
	 * 1: X coordinate
	 * <br />
	 * 2: Y coordinate
	 * <br />
	 * 3: Uncertainty
	 *
	 * @return
	 * @throws SQLException
	 */
	protected boolean insertPointsIntoShorelinePointsTable(Connection connection, long shorelineId, int segmentId, double[][] XYuncyArray) throws SQLException {
		return pgDao.insertPointsIntoShorelinePointsTable(connection, shorelineId, segmentId, XYuncyArray);
	}

	/**
	 * Inserts an attribute into the auxillary table
	 *
	 * @param connection
	 * @param shorelineId
	 * @param name
	 * @param value
	 * @return
	 * @throws SQLException besides the normal reasons, this may be thrown if
	 * the element already exists in the table - for instance if the auxillary
	 * element was repeated earlier in the shoreline file
	 */
	protected int insertAuxillaryAttribute(Connection connection, long shorelineId, String name, String value) throws SQLException {
		return pgDao.insertAuxillaryAttribute(connection, shorelineId, name, value);
	}

	/**
	 * Sets up a view against a given workspace in the shorelines table
	 *
	 * @param connection
	 * @param workspace
	 * @return
	 * @throws SQLException
	 */
	protected String createViewAgainstWorkspace(Connection connection, String workspace) throws SQLException {
		return pgDao.createViewAgainstWorkspace(connection, workspace);
	}

	/**
	 * Removes shorelines using workspace name and a wildcard match on the
	 * shoreline name.
	 *
	 * Will also delete the associated view if there are no more rows with the
	 * workspace name in them in the shorelines table
	 *
	 * @param workspace
	 * @param name does a suffix wild card match (name%) on the shoreline name
	 * for deletion
	 * @return
	 * @throws SQLException
	 */
	public boolean removeShorelines(String workspace, String name) throws SQLException {

		if (Arrays.asList(PROTECTED_WORKSPACES).contains(workspace.trim().toLowerCase())) {
			LOGGER.debug("Attempting to remove protected workspace {}. Denied.", workspace);
			return false;
		}

		return pgDao.removeShorelines(workspace, name);
	}

	public int getShorelinesByWorkspace(String workspace) throws SQLException {
		return pgDao.getShorelinesByWorkspace(workspace);
	}

	/**
	 * Removes a shoreline view from the database
	 *
	 * @param view
	 * @throws SQLException
	 */
	public void removeShorelineView(String view) throws SQLException {
		pgDao.removeShorelineView(view);
	}

	/**
	 * Returns a row count of available points in a given shoreline view
	 *
	 * @param workspace
	 * @return
	 * @throws SQLException
	 */
	public int getShorelineCountInShorelineView(String workspace) throws SQLException {
		return pgDao.getShorelineCountInShorelineView(workspace);
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
	 * gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException
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
