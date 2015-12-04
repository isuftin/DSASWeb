/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.usgs.cida.dsas.dao.pdb;

import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.utilities.features.Constants;
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
	public final static String[] REQUIRED_FIELD_NAMES = new String[]{Constants.DB_DATE_ATTR, Constants.UNCY_ATTR, Constants.BIAS_UNCY_ATTR, Constants.BIAS_ATTR, Constants.PROFILE_ID};
	public final static String DB_SCHEMA_NAME = PropertyUtil.getProperty(Property.DB_SCHEMA_NAME, "public");
	public final static String[] PROTECTED_WORKSPACES = new String[]{GeoserverDAO.PUBLISHED_WORKSPACE_NAME};
	protected String JNDI_NAME;

	public PdbDAO() {
		this.JNDI_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
	}

	private final PostgresDAO pgDao = new PostgresDAO();

	public String importToDatabase(File shpFile, Map<String, String> columns, String workspace, String EPSGCode) throws SQLException, NamingException, NoSuchElementException, ParseException, IOException {
		String viewName = null;
		BidiMap bm = new DualHashBidiMap(columns);
		String biasFieldName = (String) bm.getKey(Constants.BIAS_ATTR);  //refer to the shapefile attr (not the geo), dbf file type adds attributes 
		String biasUncyFieldName = (String) bm.getKey(Constants.BIAS_UNCY_ATTR);
		String profileIdFieldName = (String) bm.getKey(Constants.PROFILE_ID);
		String segmentIdFieldName = (String) bm.getKey(Constants.SEGMENT_ID_ATTR); //segment lookin other class
		// String dateFieldName = (String) bm.getKey(Constants.DB_DATE_ATTR); //we set the date timestop on the update...in the code UTC date.now  ...check the mapping
		String baseFileName = FilenameUtils.getBaseName(shpFile.getName());
		//File parentDirectory = shpFile.getParentFile();

		String[][] fieldNames = null;
		int MAX_POINTS_AT_ONCE = 500;

		// the header and fieldnames below pertain to the file names (not the DB)
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
			//Class<?> dateType = fc.getSchema().getDescriptor(dateFieldName).getType().getBinding();
			List<double[]> xyUncies = new ArrayList<>();

			if (!fc.isEmpty()) {
				ReprojectFeatureResults rfc = new ReprojectFeatureResults(fc, DefaultGeographicCRS.WGS84);
				try (SimpleFeatureIterator iter = rfc.features()) {
					connection.setAutoCommit(false);
					int lastSegmentId = -1;
					boolean isResultSet = false;
					//long proxyDatumBiasId = -1; //#TODO#
					ArrayList<Pdb> pdbList = new ArrayList();
					// need a description of the sf pieces. PDB requires the .shp, .shx, .dbf           
					while (iter.hasNext()) {
						SimpleFeature sf = iter.next();

						// get the values from the file and set the Pdbs with then
						Pdb pdb = new Pdb();

						int segmentId = getIntValue(segmentIdFieldName, sf);
						//BigInteger segmentId = getBigIntValue(segmentIdFieldName, sf);
						pdb.setSegmentId(segmentId);

						String profileId = (String) sf.getAttribute(profileIdFieldName); //null check ?
						pdb.setProfileId(profileId);

						String bias = (String) sf.getAttribute(biasFieldName); //null check ?
						pdb.setBias(profileId);

						String biasUncy = (String) sf.getAttribute(biasUncyFieldName); //null check ?
						pdb.setUncyb(biasUncy);

						pdbList.add(pdb);

						if (pdbList.size() == MAX_POINTS_AT_ONCE) { //review where should this be checked? sl
							isResultSet = insertPointsIntoPdbTable(connection, pdbList);  // ... pick up here Wednesday
							pdbList.clear();
						}
					} // close while

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

	private String getSourceFromFC(SimpleFeature sf) {
		String source = "";
		for (AttributeDescriptor d : sf.getFeatureType().getAttributeDescriptors()) {
			if (Constants.SOURCE_ATTR.equalsIgnoreCase(d.getLocalName()) || (Constants.SOURCE_ABBRV_ATTR.equalsIgnoreCase(d.getLocalName()))) {
				return (String) sf.getAttribute(d.getLocalName());
			}
		}
		return source;
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
// or ...   new BigInteger(String.valueOf(((Number)value)));
			return BigInteger.valueOf(((Number) value).intValue());
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
		return pgDao.createViewAgainstWorkspace(connection, workspace);
	}
}
