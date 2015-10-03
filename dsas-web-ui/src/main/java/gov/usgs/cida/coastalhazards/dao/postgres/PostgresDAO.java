package gov.usgs.cida.coastalhazards.dao.postgres;

import gov.usgs.cida.coastalhazards.dao.shoreline.Shoreline;
import gov.usgs.cida.coastalhazards.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
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
 *
 * @author isuftin
 */
public class PostgresDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PostgresDAO.class);
	public static final String METADATA_TABLE_NAME = "gt_pk_metadata_table";
	private final String JNDI_JDBC_NAME;

	public PostgresDAO() {
		this.JNDI_JDBC_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
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
			ps.setDate(1, new Date(date.getTime()));
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

		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, workspace);
				ps.setString(2, name + "%");
				deleteCt = ps.executeUpdate();
			}
		}

		return deleteCt > 0;
	}

	public int getShorelinesByWorkspace(String workspace) throws SQLException {
		String sql = "SELECT COUNT(*) FROM shorelines WHERE workspace=?";
		try (Connection connection = getConnection()) {
			try (final PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, workspace);
				ResultSet rs = ps.executeQuery();
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	/**
	 * Removes a shoreline view from the database
	 *
	 * @param view
	 * @throws SQLException
	 */
	public void removeShorelineView(String view) throws SQLException {
		String sql = "DROP VIEW IF EXISTS \"" + view + "\";";
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
		try (Connection connection = getConnection()) {
			try (final Statement ps = connection.prepareStatement(sql)) {
				ResultSet resultSet = ps.getResultSet();
				return resultSet.getInt(1);
			}
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
		String sql = "INSERT INTO shoreline_auxillary_attrs " + "(shoreline_id, attr_name, value) " + "VALUES (?,?,?)";
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
		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, workspaceName);
				ResultSet resultSet = ps.executeQuery();
				while (resultSet.next()) {
					auxNames.add(resultSet.getString(1));
				}
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
		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, workspaceName);
				ps.setString(2, auxName);
				ResultSet resultSet = ps.executeQuery();
				while (resultSet.next()) {
					auxValues.add(resultSet.getString(1));
				}
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

			try (Connection connection = getConnection()) {
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					ps.setString(1, auxName);
					ps.setString(2, workspaceName);
					int rowsUpdated = ps.executeUpdate();
					return rowsUpdated > 0;
				}
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
		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, workspaceName);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					name = rs.getString(1);
				}
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

		try (Connection connection = getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, viewName);
				ResultSet rs = ps.executeQuery();
				rs.next();
				return rs.getInt(1) != 0;
			}
		}
	}

	public List<Shoreline> getShorelinesFromBoundingBox(String workspace, double[] bbox) throws SQLException {
		String sql = "select distinct shoreline_id, uncy, segment_id, to_char(date, 'YYYY-MM-dd') as date, mhw, workspace, source, auxillary_name, auxillary_value from " + workspace
				+ " where geom && ST_MakeEnvelope(" + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3] + ",4326)"
				+ " order by date desc, shoreline_id";
		List<Shoreline> shorelines = new ArrayList<>();
		try (Connection connection = getConnection()) {
				ResultSet rs = connection.createStatement().executeQuery(sql);
				while (rs.next()) {
					Shoreline shoreline = new Shoreline();
					shoreline.setId(BigInteger.valueOf(rs.getLong(1)));
					shoreline.setUncertainty(rs.getDouble(2));
					shoreline.setSegmentId(BigInteger.valueOf(rs.getLong(3)));
					shoreline.setDate(rs.getString(4));
					shoreline.setMhw(rs.getBoolean(5));
					shoreline.setWorkspace(rs.getString(6));
					shoreline.setSource(rs.getString(7));
					shoreline.setAuxName(rs.getString(8));
					shoreline.setAuxValue(rs.getString(9));
					shorelines.add(shoreline);
				}
		}
		return shorelines;
		
	}

}
