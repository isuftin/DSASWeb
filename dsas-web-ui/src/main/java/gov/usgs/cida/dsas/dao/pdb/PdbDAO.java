package gov.usgs.cida.dsas.dao.pdb;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
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
import javax.naming.NamingException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class PdbDAO extends FeatureTypeFileDAO {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PdbDAO.class);
	public final static String PDB_VIEW_NAME = "proxy_datum_bias_view";

	public PdbDAO() {
		this.JNDI_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
	}

	@Override
	public String importToDatabase(File shpFile, Map<String, String> columns, String workspace, String EPSGCode) throws SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		LOGGER.info("Attempting to begin Import of PDB to database.");
		String viewName = null;
		updateProcessInformation(String.format("Importing pdb into database %s", shpFile.getName()));
		int MAX_POINTS_AT_ONCE = 500;
		BidiMap bm = new DualHashBidiMap(columns);
		String biasFieldName = (String) bm.getKey(Constants.BIAS_ATTR);  //refer to the shapefile attr (not the geo), dbf file type adds attributes 
		String biasUncyFieldName = (String) bm.getKey(Constants.BIAS_UNCY_ATTR);
		String profileIdFieldName = (String) bm.getKey(Constants.PROFILE_ID_ATTR);
		String segmentIdFieldName = (String) bm.getKey(Constants.SEGMENT_ID_ATTR);

		updateProcessInformation("Importing pdb into database: Reading PDB column names from Dbase file.");

		try (Connection connection = getConnection()) {
			LOGGER.info("Import Pdb into DB: Obtained connection");
			FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(shpFile.toURI().toURL());

			if (!fc.isEmpty()) {
				ReprojectFeatureResults rfc = new ReprojectFeatureResults(fc, DefaultGeographicCRS.WGS84);
				SimpleFeatureIterator iter = rfc.features();
				try {
					connection.setAutoCommit(false);

					ArrayList<Pdb> pdbList = new ArrayList();

					while (iter.hasNext()) {
						SimpleFeature sf = iter.next();

						// get the values from the file and set the Pdbs 
						Pdb pdb = new Pdb();

						int profileId = getIntValue(profileIdFieldName, sf);
						pdb.setProfileId(profileId);

						double bias = getDoubleValue(biasFieldName, sf);
						pdb.setBias(bias);

						double biasUncy = getDoubleValue(biasUncyFieldName, sf);
						pdb.setUncyb(biasUncy);

						BigInteger segmentId = getBigIntValue(segmentIdFieldName, sf);
						pdb.setSegmentId(segmentId);

						Geometry geom = (Geometry) sf.getDefaultGeometry();

						if (geom instanceof Point) {
							Point point = (Point) geom;
							pdb.setX(point.getX());
							pdb.setY(point.getY());
						} else {
							throw new UnsupportedFeatureTypeException("Only Points supported for PDB inserts.");
						}

						pdbList.add(pdb);

						if (pdbList.size() == MAX_POINTS_AT_ONCE) {
							insertPointsIntoPdbTable(connection, pdbList);
							pdbList.clear();
						}
					} // close while

					//insert the remainder of the pdb points into the table
					if (pdbList.size() > 0) {
						insertPointsIntoPdbTable(connection, pdbList);
					}

					connection.commit();
				} catch (NoSuchElementException | SQLException ex) {
					LOGGER.error("Error while attempting insert into PDB table. ", ex);
					connection.rollback();
					throw ex;
				} finally {
					LOGGER.info("Pdb import: closing iterator.");
					iter.close();
				}
			}
		} catch (SchemaException | TransformException | FactoryException ex) {
			LOGGER.error("Error while inserting into PDB table. ", ex);
			throw ex;
		}
		return PDB_VIEW_NAME;
	}

	public int getIntValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			LOGGER.error("Int value is not a number.");
			throw new ClassCastException("This attribute is not an Integer");
		}
	}

	public BigInteger getBigIntValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return BigInteger.valueOf(((Number) value).longValue());
		} else {
			LOGGER.error("BigInt value is not a number.");
			throw new ClassCastException("This attribute is not a Number");
		}
	}

	public double getDoubleValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			LOGGER.error("Double value is not a number.");
			throw new ClassCastException("This attribute is not a floating point value");
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

}
