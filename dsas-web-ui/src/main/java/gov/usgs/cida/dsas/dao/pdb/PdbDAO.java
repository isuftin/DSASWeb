package gov.usgs.cida.dsas.dao.pdb;

import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class PdbDAO extends FeatureTypeFileDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineShapefileDAO.class);
	public final static int DATABASE_PROJECTION = 4326;
	public final static String DB_SCHEMA_NAME = PropertyUtil.getProperty(Property.DB_SCHEMA_NAME, "public");
	public final static String[] PROTECTED_WORKSPACES = new String[]{GeoserverDAO.PUBLISHED_WORKSPACE_NAME};
	protected String JNDI_NAME;

	public PdbDAO() {
		this.JNDI_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
	}

	private final PostgresDAO pgDao = new PostgresDAO();

	public String importToDatabase(File shpFile, Map<String, String> columns, String workspace, String EPSGCode) throws SQLException, NamingException, NoSuchElementException, ParseException, IOException {
		String viewName = null;
		updateProcessInformation(String.format("Importing pdb into database %s", shpFile.getName()));
		BidiMap bm = new DualHashBidiMap(columns);
		String biasFieldName = (String) bm.getKey(Constants.BIAS_ATTR);  //refer to the shapefile attr (not the geo), dbf file type adds attributes 
		String biasUncyFieldName = (String) bm.getKey(Constants.BIAS_UNCY_ATTR);
		String profileIdFieldName = (String) bm.getKey(Constants.PROFILE_ID);
		String segmentIdFieldName = (String) bm.getKey(Constants.SEGMENT_ID_ATTR); 
		String baseFileName = FilenameUtils.getBaseName(shpFile.getName());

		String[][] fieldNames = null;
		int MAX_POINTS_AT_ONCE = 500;

		updateProcessInformation("Importing pdb into database: Reading PDB column names from Dbase file.");
		try (IterableShapefileReader isfr = new IterableShapefileReader(new ShpFiles(shpFile))) {
			DbaseFileHeader dbfHeader = isfr.getDbfHeader();
			fieldNames = new String[dbfHeader.getNumFields()][2];
			for (int fIdx = 0; fIdx < fieldNames.length; fIdx++) {
				fieldNames[fIdx][0] = dbfHeader.getFieldName(fIdx);
				fieldNames[fIdx][1] = String.valueOf(dbfHeader.getFieldType(fIdx));
			}
		} catch (Exception ex) {
			LOGGER.debug("Could not open shapefile for reading. Auxillary attributes will not be persisted to the database", ex);
		}
		
		
		try (Connection connection = getConnection()) {

			FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(shpFile.toURI().toURL());

			if (!fc.isEmpty()) {
				ReprojectFeatureResults rfc = new ReprojectFeatureResults(fc, DefaultGeographicCRS.WGS84);
				try (SimpleFeatureIterator iter = rfc.features()) {
					connection.setAutoCommit(false);
					int lastSegmentId = -1;
					boolean isResultSet = false;
					//long proxyDatumBiasId = -1; //#TODO#
					ArrayList<Pdb> pdbList = new ArrayList();
					        
					while (iter.hasNext()) {
						SimpleFeature sf = iter.next();

						// get the values from the file and set the Pdbs with then
						Pdb pdb = new Pdb();

						int segmentId = getIntValue(segmentIdFieldName, sf);  //null check
						//BigInteger segmentId = getBigIntValue(segmentIdFieldName, sf);
						pdb.setSegmentId(segmentId);

						String profileId = (String) sf.getAttribute(profileIdFieldName); //null check ?
						pdb.setProfileId(profileId);

						String bias = (String) sf.getAttribute(biasFieldName); //null check ?
						pdb.setBias(bias);

						String biasUncy = (String) sf.getAttribute(biasUncyFieldName); //null check ?
						pdb.setUncyb(biasUncy);

						pdbList.add(pdb);

						if (pdbList.size() == MAX_POINTS_AT_ONCE) { //review where should this be checked? sl
							isResultSet = insertPointsIntoPdbTable(connection, pdbList);  
							pdbList.clear();
						}
					} // close while
					
					//insert the remainder of the pdb points into the table
					insertPointsIntoPdbTable(connection, pdbList);
					pdbList.clear();
					
					viewName = createViewAgainstWorkspace(connection, workspace);
					if (StringUtils.isBlank(viewName)) {
						throw new SQLException("Could not create view");
					}

					new PostgresDAO().addViewToMetadataTable(connection, viewName);

					connection.commit();
				} catch (NoSuchElementException | SQLException ex) {
					connection.rollback();
					throw ex;
				}
			}
		} catch (SchemaException | TransformException | FactoryException ex) {
			Logger.getLogger(ShorelineShapefileDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return viewName;
	}


	public int getIntValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			throw new ClassCastException("This attribute is not an Integer");
		}
	}

	public BigInteger getBigIntValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return BigInteger.valueOf(((Long) value).intValue());
		} else {
			throw new ClassCastException("This attribute is not a Number");
		}
	}

	/**
	 * Inserts a row into the proxy datum bias table
	 *
	 * @param connection
	 * @param pdbs list of beans
	 *
	 * @return boolean -true if it is a resultSet
	 * @throws SQLException
	 */
	protected boolean insertPointsIntoPdbTable(Connection connection, List<Pdb> pdbs) throws SQLException {
		updateProcessInformation("Inserting points into Pdb table");
		return pgDao.insertPointsIntoPDBTable(connection, pdbs);
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
		updateProcessInformation(String.format("Creating view against workspace: %s", workspace));
		return pgDao.createViewAgainstWorkspace(connection, workspace);
	}
}
