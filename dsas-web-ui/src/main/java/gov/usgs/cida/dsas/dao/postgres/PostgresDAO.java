package gov.usgs.cida.dsas.dao.postgres;

import gov.usgs.cida.dsas.dao.pdb.Pdb;
import gov.usgs.cida.dsas.dao.shoreline.Shoreline;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * DAO class to work against the Postgres backing DB
 *
 * @author isuftin
 */
public class PostgresDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PostgresDAO.class);
	public static final String METADATA_TABLE_NAME = "gt_pk_metadata_table";
	private static String JNDI_JDBC_NAME;
	static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

	public PostgresDAO() {
		if (StringUtils.isBlank(JNDI_JDBC_NAME)) {
			JNDI_JDBC_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
		}
	}

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
			DataSource ds = (DataSource) envCtx.lookup(JNDI_JDBC_NAME);
			con = ds.getConnection();
		} catch (SQLException | NamingException ex) {
			LOGGER.error("Could not create database connection", ex);
		}
		return con;
	}

	/**
	 * Get workspace names in the workspaces table that have expired.
	 * 
	 * @param expireSeconds the time, in seconds, past which a workspace may be 
	 * considered expired
	 * @return the expired workspaces
	 * @throws SQLException 
	 */
	public String[] getExpiredWorkspaces(long expireSeconds) throws SQLException {
		List<String> results = new ArrayList<>();
		String sql = String.format("select workspace from workspace where create_time < (now() - '%s seconds'::interval)", expireSeconds);
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql);
				final ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				results.add(rs.getString(1));
			}
		}
		return results.toArray(new String[results.size()]);
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
	public long insertToShorelinesTable(Connection connection, String workspace, Date date, boolean mhw, String source, String name, String shorelineType, String auxillaryName) throws NamingException, SQLException {
		String sql = "INSERT INTO shorelines "
				+ "(date, mhw, workspace, source, shoreline_name, shoreline_type, auxillary_name) "
				+ "VALUES (?,?,?,?,?,?,?)";
		long createdId;
		try (final PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setDate(1, date);
			ps.setBoolean(2, mhw);
			ps.setString(3, workspace);
			ps.setString(4, source);
			ps.setString(5, name.toLowerCase());
			ps.setString(6, shorelineType);
			ps.setString(7, auxillaryName);
			int affectedRows = ps.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Inserting a shoreline row failed. No rows affected");
			}
			try (final ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					createdId = generatedKeys.getLong(1);
				} else {
					throw new SQLException("Inserting a shoreline row failed. No ID obtained");
				}
			}
		}
		return createdId;
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
	public boolean insertPointsIntoShorelinePointsTable(Connection connection, long shorelineId, int segmentId, double[][] XYuncyArray) throws SQLException {

		StringBuilder sql = new StringBuilder("INSERT INTO shoreline_points (shoreline_id, segment_id, geom, uncy) VALUES");
		for (double[] XYUncy : XYuncyArray) {
			sql.append("(").append(shorelineId).append(",").append(segmentId).append(",").append("ST_GeomFromText('POINT(").append(XYUncy[0]).append(" ").append(XYUncy[1]).append(")',").append(ShorelineFileDAO.DATABASE_PROJECTION).append("),").append(XYUncy[2]).append("),");
		}
		sql.deleteCharAt(sql.length() - 1);
		try (final Statement st = connection.createStatement()) {
			return st.execute(sql.toString());
		}
	}

	/**
	 * Inserts an array of points into the pdb table
	 *
	 * @param connection
	 * @param pdbs
	 *
	 * @return
	 * @throws SQLException
	 */
	public boolean insertPointsIntoPDBTable(Connection connection, List<Pdb> pdbs) throws SQLException {
		StringBuilder sql = new StringBuilder("INSERT INTO proxy_datum_bias (profile_id, segment_id, xy, bias, uncyb, last_update) VALUES");
		Timestamp now = getUTCNowAsSQLTimestamp();

		for (Pdb pdb : pdbs) {
			sql.append("(");
			sql.append(pdb.getProfileId());
			sql.append(",");
			sql.append(pdb.getSegmentId());
			sql.append(",");
			sql.append("ST_GeomFromText('POINT(");
			sql.append(pdb.getX()); //  ... ST_GeomFromText('POINT(x y)',4326)
			sql.append(" ");
			sql.append(pdb.getY());
			sql.append(ShorelineFileDAO.DATABASE_PROJECTION);
			sql.append("),");
			sql.append(pdb.getBias());
			sql.append(",");
			sql.append(pdb.getUncyb());
			sql.append(",");
			sql.append(now);
			sql.append(");");
		}

		sql.deleteCharAt(sql.length() - 1);
		LOGGER.debug("Insert points into Proxy_Datum_Bias : " + sql.toString());
		try (final Statement st = connection.createStatement()) {
			return st.execute(sql.toString());
		} //after an insert, look and see if there is a postgres process to recalibrate the index - vacuum process or trigger based on time if its performance is poor #TODO# 
	}

	/**
	 *
	 * @return Timestamp a current UTC sql timestamp
	 */
	public static Timestamp getUTCNowAsSQLTimestamp() {
		Instant now = Instant.now();
		java.sql.Timestamp currentTimestamp = Timestamp.from(now);
		return currentTimestamp;

	}

	/**
	 * Creates a row in the workspace table given the provided workspace id.
	 *
	 * Workspace id must be unique
	 *
	 * @param workspace id of workspace to be created
	 * @return success in creating workspace
	 * @throws SQLException
	 */
	public boolean createWorkspace(String workspace) throws SQLException {
		String sql = "INSERT INTO workspace (workspace) VALUES(?)";
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspace);
			ps.execute();
		}
		return true;
	}

	/**
	 * Removes a workspace from the workspace table.
	 *
	 * This will also remove all associated points. Optionally, also remove the
	 * associated views.
	 *
	 * @param workspace the workspace id to remove
	 * @return true if workspace was deleted, false if not
	 * @throws java.sql.SQLException
	 */
	public boolean removeWorkspace(String workspace) throws SQLException {
		String sql = "DELETE FROM workspace "
				+ "WHERE workspace=?";

		boolean deleted;

		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspace);
			deleted = ps.executeUpdate() > 0;
		}

		if (deleted) {
			LOGGER.debug(String.format("Removed workspace %s from workspace table", workspace));
			String[] views = getWorkspaceAssociatedViews(workspace);
			for (String view : views) {
				removeView(view);
				LOGGER.debug(String.format("Removed view %s", view));
			}
		} else {
			LOGGER.debug(String.format("Could not remove workspace %s from workspace table", workspace));
		}

		return deleted;
	}

	/**
	 * Gets all views associated with a workspace.
	 *
	 * @param workspace the name of the workspace
	 * @return the workspace view names associated with the workspace name
	 * @throws SQLException
	 */
	private String[] getWorkspaceAssociatedViews(String workspace) throws SQLException {
		String statement = "select table_name from INFORMATION_SCHEMA.views "
				+ "where table_name LIKE ?;";
		List<String> viewNames = new ArrayList<>();
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(statement)) {
			ps.setString(1, workspace + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					viewNames.add(rs.getString(1));
				}
			}
		}
		return viewNames.toArray(new String[viewNames.size()]);
	}

	/**
	 * Sets up a view against a given workspace in the shorelines table
	 *
	 * @param connection
	 * @param workspace
	 * @return
	 * @throws SQLException
	 */
	public String createViewAgainstWorkspace(Connection connection, String workspace) throws SQLException {
		String sql = "SELECT * FROM CREATE_WORKSPACE_VIEW(?)";
		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspace);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString(1);
				}
				return null;
			}
		}
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
		String sql = "DELETE FROM shorelines "
				+ "WHERE workspace=? AND shoreline_name LIKE ?";

		int deleteCt;

		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspace);
			ps.setString(2, name + "%");
			deleteCt = ps.executeUpdate();
		}

		return deleteCt > 0;
	}

	public int getShorelinesByWorkspace(String workspace) throws SQLException {
		String sql = "SELECT COUNT(*) FROM shorelines WHERE workspace=?";
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspace);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		}
	}

	/**
	 * Removes a shoreline view from the database
	 *
	 * @param view
	 * @throws SQLException
	 */
	public void removeView(String view) throws SQLException {
		String sql = "DROP VIEW IF EXISTS \"" + view + "\" CASCADE;";
		try (Connection connection = getConnection()) {
			try (final Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		}
	}

	/**
	 * Returns a row count of available points in a given shoreline view
	 *
	 * @param workspace
	 * @return
	 * @throws SQLException
	 */
	public int getShorelineCountInShorelineView(String workspace) throws SQLException {
		String sql = "SELECT COUNT(id) FROM " + workspace + "_shorelines";
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ResultSet resultSet = ps.getResultSet();
			return resultSet.getInt(1);
		}
	}

	/**
	 * Runs vacuum analysis against a sql table.
	 *
	 * This is particularly useful after adding points to a geospatial table
	 * like shoreline_points
	 *
	 * @param tableName
	 * @throws SQLException
	 */
	public void optimizeTable(String tableName) throws SQLException {
		String tName = tableName;
		if (StringUtils.isBlank(tName)) {
			tName = "shoreline_points";
		}
		String sql = "VACUUM analyze " + tName + ";";
		try (Connection connection = getConnection()) {
			connection.createStatement().execute(sql);
		}
	}

	public void optimizeTables() throws SQLException {
		optimizeTable("shoreline_points");
		optimizeTable("shorelines");
		optimizeTable("shoreline_auxillary_attrs");
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
	public int insertAuxillaryAttribute(Connection connection, long shorelineId, String name, String value) throws SQLException {
		String sql = "INSERT INTO shoreline_auxillary_attrs  (shoreline_id, attr_name, value) VALUES (?,?,?)";
		try (final PreparedStatement st = connection.prepareStatement(sql)) {
			st.setLong(1, shorelineId);
			st.setString(2, name);
			st.setString(3, value);
			return st.executeUpdate();
		}
	}

	/**
	 * Gets available auxillary value names for a given workspace
	 *
	 * @param workspaceName
	 * @return
	 * @throws SQLException
	 */
	public String[] getAvailableAuxillaryNamesFromWorkspace(String workspaceName) throws SQLException {
		String sql = "SELECT DISTINCT a.attr_name "
				+ "FROM shoreline_auxillary_attrs a "
				+ "JOIN shorelines s "
				+ "ON a.shoreline_id = s.id "
				+ "WHERE s.workspace = ?";

		List<String> auxNames = new ArrayList<>();
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspaceName);
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next()) {
				auxNames.add(resultSet.getString(1));
			}
		}
		return auxNames.toArray(new String[auxNames.size()]);
	}

	/**
	 * Gets available auxillary value names for a given workspace
	 *
	 * @param workspaceName
	 * @param auxName
	 * @return
	 * @throws SQLException
	 */
	public String[] getAvailableAuxillaryValuesFromWorkspace(String workspaceName, String auxName) throws SQLException {
		String sql = "SELECT DISTINCT a.value "
				+ "FROM shoreline_auxillary_attrs a "
				+ "JOIN shorelines s "
				+ "ON a.shoreline_id = s.id "
				+ "WHERE s.workspace = ? "
				+ "AND a.attr_name = ?";

		List<String> auxValues = new ArrayList<>();
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspaceName);
			ps.setString(2, auxName);
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next()) {
				auxValues.add(resultSet.getString(1));
			}
		}
		return auxValues.toArray(new String[auxValues.size()]);
	}

	/**
	 * Updates the shorelines table's auxillary_name column
	 *
	 * @param workspaceName
	 * @param auxName
	 * @return
	 * @throws SQLException
	 */
	public boolean updateShorelineAuxillaryName(String workspaceName, String auxName) throws SQLException {
		if (StringUtils.isNotBlank(workspaceName) && !"published".equals(workspaceName.trim().toLowerCase())) {
			String sql = "UPDATE shorelines "
					+ "SET auxillary_name =  ? "
					+ "WHERE workspace = ?";

			String[] availableAuxNames = getAvailableAuxillaryNamesFromWorkspace(workspaceName);
			if (StringUtils.isNotBlank(auxName) && !Arrays.asList(availableAuxNames).contains(auxName)) {
				LOGGER.info("workspace {} does not contain auxillary name {}", workspaceName, auxName);
				return false;
			}

			try (Connection connection = getConnection();
					final PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, auxName);
				ps.setString(2, workspaceName);
				int rowsUpdated = ps.executeUpdate();
				return rowsUpdated > 0;
			}
		} else {
			return false;
		}
	}

	/**
	 * Retrieves a map of dates to auxillary values.
	 *
	 * @param workspaceName
	 * @return
	 * @throws SQLException
	 */
	public Map<String, String> getShorelineDateToAuxValueMap(String workspaceName) throws SQLException {
		Map<String, String> d2a = new HashMap<>();
		String sql = "SELECT DISTINCT s.date, a.value "
				+ "FROM shoreline_auxillary_attrs a "
				+ "JOIN shorelines s "
				+ "ON a.shoreline_id = s.id "
				+ "AND a.attr_name = s.auxillary_name "
				+ "WHERE s.workspace = ? "
				+ "ORDER BY s.date";
		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspaceName);
			ResultSet rs = ps.executeQuery();
			SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd");
			while (rs.next()) {
				Date date = rs.getDate(1);
				String value = rs.getString(2);
				String dateStr = df.format(date);
				d2a.put(dateStr, value);
			}
		}
		return d2a;
	}

	/**
	 * Retrieves the current auxillary name for a given workspace.
	 *
	 * @param workspaceName
	 * @return
	 * @throws SQLException
	 */
	public String getCurrentShorelineAuxNameForWorkspace(String workspaceName) throws SQLException {
		String name = "";

		String sql = "SELECT DISTINCT auxillary_name "
				+ "FROM shorelines "
				+ "WHERE workspace = ?";

		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, workspaceName);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				name = rs.getString(1);
			}
		}
		return name;
	}

	/**
	 * Inserts a view into the metadata table
	 *
	 * @param connection
	 * @see
	 * http://docs.geoserver.org/stable/en/user/data/database/primarykey.html#using-the-metadata-table-with-views
	 * @param viewName
	 */
	public void addViewToMetadataTable(Connection connection, String viewName) throws SQLException {
		String sql = "INSERT INTO gt_pk_metadata_table("
				+ "table_schema, table_name, pk_column, pk_column_idx, pk_policy, pk_sequence) "
				+ "VALUES ('public',?,'id',1,'assigned',null)";

		if (!viewExistsInMetadataTable(viewName)) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, viewName);
				ps.executeUpdate();
			}
		}
	}

	public boolean viewExistsInMetadataTable(String viewName) throws SQLException {
		String sql = "SELECT COUNT(*) "
				+ "FROM gt_pk_metadata_table "
				+ "WHERE table_name = ?";

		try (Connection connection = getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, viewName);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1) != 0;
		}
	}

	/**
	 * Get shorelines from database by bounding box. Note: this is only the top
	 * level shoreline metadata, and doesn't include info related to uncertainty
	 * needed for further calculations.
	 *
	 * @param workspace
	 * @param bbox
	 * @return
	 * @throws SQLException
	 */
	public List<Shoreline> getShorelinesFromBoundingBox(String workspace, double[] bbox) throws SQLException {
		String sql = "select distinct shoreline_id, to_char(date, 'YYYY-MM-dd') as date, mhw, workspace, source, auxillary_name, auxillary_value, shoreline_name from " + workspace
				+ " where geom && ST_MakeEnvelope(" + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3] + ",4326)"
				+ " order by date desc, shoreline_id";
		List<Shoreline> shorelines = new ArrayList<>();
		try (Connection connection = getConnection();
				ResultSet rs = connection.createStatement().executeQuery(sql)) {
			while (rs.next()) {
				Shoreline shoreline = new Shoreline();
				shoreline.setId(BigInteger.valueOf(rs.getLong(1)));
				shoreline.setDate(rs.getString(2));
				shoreline.setMhw(rs.getBoolean(3));
				shoreline.setWorkspace(rs.getString(4));
				shoreline.setSource(rs.getString(5));
				shoreline.setAuxName(rs.getString(6));
				shoreline.setAuxValue(rs.getString(7));
				shoreline.setName(rs.getString(8));
				shorelines.add(shoreline);
			}
		}
		return shorelines;
	}

}
