package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.owsutils.commons.shapefile.utils.PointIterator;
import gov.usgs.cida.owsutils.commons.shapefile.utils.ShapeAndAttributes;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader.Row;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an exploder which will output directly to a database.
 *
 * @see
 * <a href="http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html">http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html</a>
 * @author isuftin
 */
public abstract class DatabaseOutputXploder extends Xploder {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseOutputXploder.class);
	public final static String DB_TYPE_PARAM = "dbtype";
	public final static String INCOMING_DATEFIELD_NAME_PARAM = "incomingDateFieldName";
	public final static String INCOMING_MHW_NAME_PARAM = "incomingMHWFieldName";
	public final static String INCOMING_SOURCE_NAME_PARAM = "incomingSourceFieldName";
	public final static String INCOMING_NAME_NAME_PARAM = "incomingNameFieldName";
	public final static String INCOMING_ORIENTATION_NAME_PARAM = "incomingOrientationFieldName";
	public final static String WORKSPACE_PARAM = "workspaceParam";
	public final static String POINTS_TABLE_NAME_PARAM = "ptsTableName";
	public final static String SHORELINE_TABLE_NAME_PARAM = "shorelineTableName";
	public final static String POINTS_TABLE_SHORELINE_ID_NAME_PARAM = "ptsTableShorelineIdName";
	public final static String POINTS_TABLE_SEGMENT_ID_NAME_PARAM = "ptsTableSegmentIdName";
	public final static String SHORELINE_TABLE_DATE_NAME_PARAM = "shorelinesTableDateName";
	public final static String SHORELINE_TABLE_WORKSPACE_NAME_PARAM = "shorelinesTableWorkspaceName";
	public final static String SHORELINE_TABLE_SOURCE_NAME_PARAM = "shorelinesTableSourceName";
	public final static String SHORELINE_TABLE_SHORELINENAME_NAME_PARAM = "shorelinesTableShorelineNameName";
	public final static String SHORELINE_TABLE_SHORELINETYPE_NAME_PARAM = "shorelinesTableShorelineTypeName"; // orientation
	public final static String SHORELINE_TABLE_SHORELINEAUX_NAME_PARAM = "shorelinesTableShorelineAuxName";
	public final static String SHORELINE_TABLE_MHW_NAME_PARAM = "shorelinesTableMhwName";
	public final static String AUX_TABLE_SHORELINE_ID_NAME_PARAM = "auxTableShorelineIdName";
	public final static String AUX_TABLE_ATTR_NAME_NAME_PARAM = "auxTableAttrName";
	public final static String AUX_TABLE_VALUE_NAME_PARAM = "auxTableValueName";
	protected final String SHAPEFILE_DATE_FIELD_NAME;
	protected final String SHAPEFILE_MHW_FIELD_NAME;
	protected final String SHAPEFILE_SOURCE_FIELD_NAME;
	protected final String SHAPEFILE_NAME_FIELD_NAME;
	protected final String SHAPEFILE_ORIENTATION_FIELD_NAME;
	protected final String WORKSPACE_NAME;
	protected String SHORELINES_TABLE_NAME = "shorelines";
	protected String WORKSPACE_TABLE_NAME = "workspace";
	protected String AUX_TABLE_NAME = "shoreline_auxillary_attrs";
	protected String SHORELINES_TABLE_DATE_FIELD_NAME = "date";
	protected String SHORELINES_TABLE_MHW_FIELD_NAME = "mhw";
	protected String SHORELINES_TABLE_WORKSPACE_FIELD_NAME = "workspace";
	protected String SHORELINES_TABLE_SOURCE_FIELD_NAME = "source";
	protected String SHORELINES_TABLE_SHORELINENAME_FIELD_NAME = "shoreline_name";
	protected String SHORELINES_TABLE_SHORELINETYPE_FIELD_NAME = "shoreline_type";
	protected String SHORELINES_TABLE_SHORELINEAUX_FIELD_NAME = "auxillary_name";
	protected String POINTS_TABLE_NAME = "shoreline_points";
	protected String POINT_TABLE_SHORELINED_ID_FIELD_NAME = "shoreline_id";
	protected String POINT_TABLE_SEGMENT_ID_FIELD_NAME = "segment_id";
	protected String AUX_TABLE_SHORELINE_ID_FIELD_NAME = "shoreline_id";
	protected String AUX_TABLE_ATTR_NAME_NAME = "attr_name";
	protected String AUX_TABLE_VALUE_NAME = "value";
	protected SimpleFeatureType pointOutputFeatureType;
	protected SimpleFeatureType shorelineOutputFeatureType;
	protected Connection connection;
	public final String dbType;
	protected final Map<String, Object> dbConfig = new HashMap<>();

	public DatabaseOutputXploder(Map<String, Object> config) throws IOException {
		super(config);

		String[] requiredConfigs = new String[]{
			DB_TYPE_PARAM,
			INCOMING_DATEFIELD_NAME_PARAM,
			WORKSPACE_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for DatabaseOutputExploder must include parameter %s", requiredConfig));
			}
		}

		this.dbType = (String) config.get(DB_TYPE_PARAM);
		this.WORKSPACE_NAME = (String) config.get(WORKSPACE_PARAM);
		this.SHAPEFILE_MHW_FIELD_NAME = (String) config.get(INCOMING_MHW_NAME_PARAM);
		this.SHAPEFILE_SOURCE_FIELD_NAME = (String) config.get(INCOMING_SOURCE_NAME_PARAM);
		this.SHAPEFILE_NAME_FIELD_NAME = (String) config.get(INCOMING_NAME_NAME_PARAM);
		this.SHAPEFILE_ORIENTATION_FIELD_NAME = (String) config.get(INCOMING_ORIENTATION_NAME_PARAM);

		String pointsTableName = (String) config.get(POINTS_TABLE_NAME_PARAM);
		if (StringUtils.isNotBlank(pointsTableName)) {
			this.POINTS_TABLE_NAME = pointsTableName;
		}

		String shorelinesTableName = (String) config.get(SHORELINE_TABLE_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableName)) {
			this.SHORELINES_TABLE_NAME = shorelinesTableName;
		}

		String incomingDateFieldName = (String) config.get(INCOMING_DATEFIELD_NAME_PARAM);
		if (StringUtils.isBlank(incomingDateFieldName)) {
			throw new IllegalArgumentException("Incoming date field name parameter may not be blank or null");
		}
		this.SHAPEFILE_DATE_FIELD_NAME = incomingDateFieldName;

		String pointsShorelineIdName = (String) config.get(POINTS_TABLE_SHORELINE_ID_NAME_PARAM);
		if (StringUtils.isNotBlank(pointsShorelineIdName)) {
			this.POINT_TABLE_SHORELINED_ID_FIELD_NAME = pointsShorelineIdName;
		}

		String pointsSegmentIdFieldName = (String) config.get(POINTS_TABLE_SEGMENT_ID_NAME_PARAM);
		if (StringUtils.isNotBlank(pointsSegmentIdFieldName)) {
			this.POINT_TABLE_SEGMENT_ID_FIELD_NAME = pointsSegmentIdFieldName;
		}

		String shorelinesTableWorkspaceName = (String) config.get(SHORELINE_TABLE_WORKSPACE_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableWorkspaceName)) {
			this.SHORELINES_TABLE_WORKSPACE_FIELD_NAME = shorelinesTableWorkspaceName;
		}

		String shorelinesTableSourceName = (String) config.get(SHORELINE_TABLE_SOURCE_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableSourceName)) {
			this.SHORELINES_TABLE_SOURCE_FIELD_NAME = shorelinesTableSourceName;
		}

		String shorelinesTableShorelineNameName = (String) config.get(SHORELINE_TABLE_SHORELINENAME_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableShorelineNameName)) {
			this.SHORELINES_TABLE_SHORELINENAME_FIELD_NAME = shorelinesTableShorelineNameName;
		}

		String shorelinesTableShorelineTypeName = (String) config.get(SHORELINE_TABLE_SHORELINETYPE_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableShorelineTypeName)) {
			this.SHORELINES_TABLE_SHORELINETYPE_FIELD_NAME = shorelinesTableShorelineTypeName;
		}

		String shorelinesTableDateName = (String) config.get(SHORELINE_TABLE_DATE_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableDateName)) {
			this.SHORELINES_TABLE_DATE_FIELD_NAME = shorelinesTableDateName;
		}

		String shorelinesTableAuxName = (String) config.get(SHORELINE_TABLE_SHORELINEAUX_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableAuxName)) {
			this.SHORELINES_TABLE_SHORELINEAUX_FIELD_NAME = shorelinesTableAuxName;
		}

		String shorelinesTableMhwName = (String) config.get(SHORELINE_TABLE_MHW_NAME_PARAM);
		if (StringUtils.isNotBlank(shorelinesTableMhwName)) {
			this.SHORELINES_TABLE_MHW_FIELD_NAME = shorelinesTableMhwName;
		}

		String auxTableShorelineIdName = (String) config.get(AUX_TABLE_SHORELINE_ID_NAME_PARAM);
		if (StringUtils.isNotBlank(auxTableShorelineIdName)) {
			this.AUX_TABLE_SHORELINE_ID_FIELD_NAME = auxTableShorelineIdName;
		}

		String auxTableAttrName = (String) config.get(AUX_TABLE_ATTR_NAME_NAME_PARAM);
		if (StringUtils.isNotBlank(auxTableAttrName)) {
			this.AUX_TABLE_ATTR_NAME_NAME = auxTableAttrName;
		}

		String auxTableValueName = (String) config.get(AUX_TABLE_VALUE_NAME_PARAM);
		if (StringUtils.isNotBlank(auxTableValueName)) {
			this.AUX_TABLE_VALUE_NAME = auxTableValueName;
		}

	}

	public static Map<String, Object> mergeMaps(Map<String, Object> m1, Map<String, Object> m2) {
		Map<String, Object> mergedMap = new HashMap<>(m1.size() + m2.size());
		mergedMap.putAll(m1);
		mergedMap.putAll(m2);
		return mergedMap;
	}

	protected SimpleFeatureType createShorelineOutputFeatureType() throws IOException {
		SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
		DataStore ds = null;
		try {
			ds = DataStoreFinder.getDataStore(dbConfig);
			SimpleFeatureType shorelineSchema = ds.getSchema(SHORELINES_TABLE_NAME);
			String dateColumnName = null;
			Class<?> dateType = null;
			String meanHighWaterName = null;
			for (AttributeDescriptor attDesc : shorelineSchema.getAttributeDescriptors()) {
				if (Date.class.isAssignableFrom(attDesc.getType().getBinding())) {
					dateColumnName = attDesc.getLocalName();
					dateType = attDesc.getType().getBinding();
				}
				if (Boolean.class.isAssignableFrom(attDesc.getType().getBinding())) {
					meanHighWaterName = attDesc.getLocalName();
				}
			}
			ftb.setName(SHORELINES_TABLE_NAME);
			ftb.add(dateColumnName, dateType);
			ftb.add(meanHighWaterName, Boolean.class);
			ftb.add(SHORELINES_TABLE_WORKSPACE_FIELD_NAME, String.class);
			ftb.add(SHORELINES_TABLE_SOURCE_FIELD_NAME, String.class);
			ftb.add(SHORELINES_TABLE_SHORELINENAME_FIELD_NAME, String.class);
			ftb.add(SHORELINES_TABLE_SHORELINETYPE_FIELD_NAME, String.class);

			return ftb.buildFeatureType();

		} finally {
			if (ds != null) {
				ds.dispose();
			}
		}
	}

	protected SimpleFeatureType createPointOutputFeatureType() throws IOException {
		SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
		DataStore ds = null;
		try {
			ds = getDataStore();
			SimpleFeatureType pointsSchema = ds.getSchema(POINTS_TABLE_NAME);
			String geomColumnName = null;
			String tableUncyColumnName = null;

			// I expect only one geometry column and only one double column in the points
			// table so those should end up being the point and uncy columns
			for (AttributeDescriptor attDesc : pointsSchema.getAttributeDescriptors()) {
				if (Geometry.class.isAssignableFrom(attDesc.getType().getBinding())) {
					geomColumnName = attDesc.getLocalName();
				}
				if (Double.class.isAssignableFrom(attDesc.getType().getBinding())) {
					tableUncyColumnName = attDesc.getLocalName();
				}
			}

			ftb.setName(POINTS_TABLE_NAME);
			ftb.setSRS(CRS.toSRS(DefaultGeographicCRS.WGS84));
			ftb.add(geomColumnName, Point.class, DefaultGeographicCRS.WGS84);
			ftb.add(tableUncyColumnName, Double.class);
			ftb.add(POINT_TABLE_SHORELINED_ID_FIELD_NAME, BigInteger.class);
			ftb.add(POINT_TABLE_SEGMENT_ID_FIELD_NAME, BigInteger.class);
			return ftb.buildFeatureType();

		} finally {
			if (ds != null) {
				ds.dispose();
			}
		}

	}

	@Override
	protected SimpleFeatureType createOutputFeatureType(String outputTypeName) throws IOException {
		DataStore createDataStore = null;
		try {
			createDataStore = getDataStore();
			return createDataStore.getSchema(outputTypeName);
		} finally {
			if (createDataStore != null) {
				createDataStore.dispose();
			}
		}
	}

	@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx, String typeName) throws IOException {
		outputFeatureType = createOutputFeatureType(typeName);
		DataStore ds = getDataStore();
		FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = ds.getFeatureWriterAppend(typeName, tx);

		return featureWriter;
	}

	@Override
	public int explode() throws IOException {
		int ptTotal = 0;
		this.pointOutputFeatureType = createPointOutputFeatureType();
		this.shorelineOutputFeatureType = createShorelineOutputFeatureType();
		try (IterableShapefileReader rdr = initReader();
				Transaction tx = new DefaultTransaction();
				Connection tryWithResourcesConnection = getDataStore().getConnection(tx);
				FeatureWriter<SimpleFeatureType, SimpleFeature> pointFeatureWriter = createFeatureWriter(tx, POINTS_TABLE_NAME);) {

			this.connection = tryWithResourcesConnection;

			DbaseFileHeader dbfHeader = rdr.getDbfHeader();
			int dateFieldIdx = locateField(dbfHeader, SHAPEFILE_DATE_FIELD_NAME);
			int mhwIdx = locateField(dbfHeader, SHAPEFILE_MHW_FIELD_NAME);
			int sourceIdx = locateField(dbfHeader, SHAPEFILE_SOURCE_FIELD_NAME);
			int nameIdx = locateField(dbfHeader, SHAPEFILE_NAME_FIELD_NAME);
			int orientationIdx = locateField(dbfHeader, SHAPEFILE_ORIENTATION_FIELD_NAME);

			String[][] fieldNames = new String[dbfHeader.getNumFields()][2];
			for (int fIdx = 0; fIdx < fieldNames.length; fIdx++) {
				fieldNames[fIdx][0] = dbfHeader.getFieldName(fIdx);
				fieldNames[fIdx][1] = String.valueOf(dbfHeader.getFieldType(fIdx));
			}

			LOGGER.debug("Input files from {}\n{}",
					shapeFiles.getTypeName(),
					String.join(",",
							shapeFiles.getFileNames().values().toArray(new String[shapeFiles.getFileNames().size()])
					)
			);

			int shapeCount = 0;
			long startWriteTime = new java.util.Date().getTime();
			for (ShapeAndAttributes saa : rdr) {
				long startLoopTime = new java.util.Date().getTime();
				java.util.Date date = getDateFromRowObject(saa.row.read(dateFieldIdx), dbfHeader.getFieldClass(dateFieldIdx));
				boolean mhw = mhwIdx == -1 ? false : (boolean) saa.row.read(mhwIdx);
				String source = sourceIdx == -1 ? shapeFiles.getTypeName() : (String) saa.row.read(sourceIdx);
				String name = nameIdx == -1 ? shapeFiles.getTypeName() : (String) saa.row.read(nameIdx);
				String orientation = orientationIdx == -1 ? null : (String) saa.row.read(orientationIdx);

				long shorelineId = writeShoreline(WORKSPACE_NAME, date, mhw, source, name, orientation);
				int ptCt = processShape(saa, ++shapeCount, shorelineId, pointFeatureWriter);

				Map<String, String> auxCols = getAuxillaryColumnsFromRow(saa.row, fieldNames);
				for (Map.Entry<String, String> auxEntry : auxCols.entrySet()) {
					if (StringUtils.isNotBlank(auxEntry.getValue())) {
						insertAuxAttribute(shorelineId, auxEntry.getKey(), auxEntry.getValue());
					}
				}

				LOGGER.debug("Wrote {} points in {}ms for shape {}", ptCt, new java.util.Date().getTime() - startLoopTime, saa.record.toString());
				ptTotal += ptCt;
			}

			tx.commit();
			LOGGER.info("Wrote {} points in {}ms in {} shapes", ptTotal, new java.util.Date().getTime() - startWriteTime, shapeCount);
		} catch (MismatchedDimensionException | TransformException | FactoryException | ParseException | SQLException ex) {
			throw new IOException(ex);
		}
		return ptTotal;
	}

	protected Map<String, String> getAuxillaryColumnsFromRow(Row row, String[][] fieldNames) throws IOException {
		//TODO - Get this from ShorelineFile which is currently not in commons
		String[] AUXILLARY_ATTRIBUTES = new String[]{
			Constants.SURVEY_ID_ATTR,
			Constants.DISTANCE_ATTR,
			Constants.DEFAULT_D_ATTR,
			Constants.NAME_ATTR,
			Constants.BIAS_UNCY_ATTR,
			Constants.MHW_ATTR
		};

		// C (Character)  = String
		// N (Numeric)   = Integer or Long or Double (depends on field's decimal count and fieldLength)
		// F (Floating)  = Double
		// L (Logical)   = Boolean
		// D (Date)		 = java.util.Date (Without time)
		// @ (Timestamp) = java.sql.Timestamp (With time)
		// Unknown		 = String
		Map<String, String> auxillaryAttributes = new HashMap<>();
		for (String attribute : AUXILLARY_ATTRIBUTES) {
			for (int fnIdx = 0; fnIdx < fieldNames.length; fnIdx++) {
				String fieldName = fieldNames[fnIdx][0];
				char fieldType = fieldNames[fnIdx][1].charAt(0);
				String cleanedFieldName = fieldName.trim().replaceAll("_", "");
				String cleanedAttribute = attribute.trim().replaceAll("_", "");
				if (cleanedFieldName.equalsIgnoreCase(cleanedAttribute)) {
					Object attrObj = row.read(fnIdx);
					if (attrObj != null) {
						switch (fieldType) {
							case 'D':
							case '@':
								auxillaryAttributes.put(attribute.toLowerCase(), String.valueOf(((java.util.Date) attrObj).getTime()));
								break;
							case 'L':
							case 'N':
							case 'F':
							case 'C':
							default:
								auxillaryAttributes.put(attribute.toLowerCase(), String.valueOf(attrObj));
						}
					}
				}
			}
		}
		return auxillaryAttributes;
	}

	@Override
	public int processShape(ShapeAndAttributes saa, int segmentId, long shorelineId, FeatureWriter<SimpleFeatureType, SimpleFeature> pointFeatureWriter) throws IOException, MismatchedDimensionException, TransformException, FactoryException {
		Double uncertainty = ((Number) saa.row.read(uncertaintyIdIdx)).doubleValue();

		int ptCt = 0;
		MultiLineString shape = (MultiLineString) saa.record.shape();
		int numGeom = shape.getNumGeometries();
		int MAX_POINTS_SIZE = 500;
		MathTransform mathTransform = CRS.findMathTransform(sourceCRS, outputCRS, true);
		String epsgCode = CRS.lookupIdentifier(pointOutputFeatureType.getCoordinateReferenceSystem(), true);
		epsgCode = epsgCode.substring(5); // I want the code from EPSG:XXXX, not the "EPSG:" part

		for (int segmentIndex = 0; segmentIndex < numGeom; segmentIndex++) {
			try {
				Geometry geometry = JTS.transform(shape.getGeometryN(segmentIndex), mathTransform);
				PointIterator pIterator = new PointIterator(geometry);
				List<double[]> xyUncies = new ArrayList<>(geometry.getNumPoints());

				while (pIterator.hasNext()) {
					Point point = pIterator.next();
					xyUncies.add(new double[]{
						point.getX(),
						point.getY(),
						uncertainty
					});

					if (xyUncies.size() == MAX_POINTS_SIZE) {
						writePoints(
								shorelineId,
								segmentId,
								xyUncies.toArray(new double[MAX_POINTS_SIZE][]),
								epsgCode
						);
						xyUncies.clear();
					}

					ptCt++;
				}

				if (xyUncies.size() > 0) {
					writePoints(
							shorelineId,
							segmentId,
							xyUncies.toArray(new double[xyUncies.size()][]),
							epsgCode
					);
				}
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		}
		return ptCt;
	}

	public int insertAuxAttribute(long shorelineId, String name, String value) throws IOException, SQLException {
		String sql = String.format("INSERT INTO %s  (%s, %s, %s) VALUES (?,?,?)",
				AUX_TABLE_NAME,
				AUX_TABLE_SHORELINE_ID_FIELD_NAME,
				AUX_TABLE_ATTR_NAME_NAME,
				AUX_TABLE_VALUE_NAME);
		try (final PreparedStatement st = connection.prepareStatement(sql)) {
			st.setLong(1, shorelineId);
			st.setString(2, name);
			st.setString(3, value);
			return st.executeUpdate();
		}
	}

	public boolean writePoints(long shorelineId, int segmentId, double[][] XYuncyArray, String epsgCode) throws IOException, SQLException {
		StringBuilder sql = new StringBuilder(String.format("INSERT INTO %s (%s, %s, geom, uncy) VALUES", POINTS_TABLE_NAME, POINT_TABLE_SHORELINED_ID_FIELD_NAME, POINT_TABLE_SEGMENT_ID_FIELD_NAME));

		for (double[] XYUncy : XYuncyArray) {
			sql.append("(").append(shorelineId).append(",").append(segmentId).append(",").append("ST_GeomFromText('POINT(").append(XYUncy[0]).append(" ").append(XYUncy[1]).append(")',")
					.append(epsgCode).append("),").append(XYUncy[2]).append("),");
		}
		sql.deleteCharAt(sql.length() - 1);

		try (final Statement st = connection.createStatement()) {
			return st.execute(sql.toString());
		}
	}

	private java.util.Date getDateFromRowObject(Object date, Class<?> fromType) throws ParseException {
		java.util.Date result = null;

		if (fromType == java.lang.String.class) {
			String dateString = (String) date;
			try {
				result = new SimpleDateFormat("MM/dd/yyyy").parse((String) date);
			} catch (ParseException ex) {
				LOGGER.debug("Could not parse date in format 'MM/dd/yyyy' - Will try non-padded", ex);
			}

			if (null == result) {
				result = new SimpleDateFormat("M/d/yyyy").parse(dateString);
			}
		} else if (fromType == java.util.Date.class) {
			result = (java.util.Date) date;
		}

		return result;
	}

	protected long writeShoreline(String workspace, java.util.Date date, boolean mhw, String source, String name, String orientation) throws IOException, SQLException {
		String sql = MessageFormat.format("INSERT INTO {0}({1},{2}, {3}, {4}, {5}, {6}, {7}) VALUES (?,?,?,?,?,?,?)",
				SHORELINES_TABLE_NAME,
				SHORELINES_TABLE_DATE_FIELD_NAME,
				SHORELINES_TABLE_MHW_FIELD_NAME,
				SHORELINES_TABLE_WORKSPACE_FIELD_NAME,
				SHORELINES_TABLE_SOURCE_FIELD_NAME,
				SHORELINES_TABLE_SHORELINENAME_FIELD_NAME,
				SHORELINES_TABLE_SHORELINETYPE_FIELD_NAME,
				SHORELINES_TABLE_SHORELINEAUX_FIELD_NAME
		);
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setBoolean(2, mhw);
			ps.setString(3, workspace);
			ps.setString(4, source);
			ps.setString(5, name.toLowerCase());
			ps.setString(6, orientation);
			ps.setString(7, null);
			int affectedRows = ps.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Inserting a shoreline row failed. No rows affected");
			}
			try (final ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					return generatedKeys.getLong(1);
				} else {
					throw new SQLException("Inserting a shoreline row failed. No ID obtained");
				}
			}
		}
	}

	protected abstract JDBCDataStore getDataStore() throws IOException;
	
	@Override
	public void close() throws Exception {
		super.close();
		if (this.connection != null && !this.connection.isClosed()) {
			this.connection.close();
		}
	}
}
