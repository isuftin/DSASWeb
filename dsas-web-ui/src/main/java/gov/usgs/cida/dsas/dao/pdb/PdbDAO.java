package gov.usgs.cida.dsas.dao.pdb;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.dao.FeatureTypeFileDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
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
import org.apache.commons.lang.StringUtils;
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
	public String importToDatabase(File shpFile, Map<String, String> columns, String workspace, String EPSGCode) throws SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException, AttributeNotANumberException {
		LOGGER.info("Attempting to begin Import of PDB to database.");
		String viewName = null;
		updateProcessInformation(String.format("Importing pdb into database %s", shpFile.getName()));
		BidiMap bm = new DualHashBidiMap(columns);
		String biasFieldName = (String) bm.getKey(Constants.BIAS_ATTR);  //refer to the shapefile attr (not the geo), dbf file type adds attributes 
		String biasUncyFieldName = (String) bm.getKey(Constants.BIAS_UNCY_ATTR);
		String profileIdFieldName = (String) bm.getKey(Constants.PROFILE_ID_ATTR);
		String segmentIdFieldName = (String) bm.getKey(Constants.SEGMENT_ID_ATTR); 
		
		int MAX_POINTS_AT_ONCE = 500;

		updateProcessInformation("Importing pdb into database: Reading PDB column names from Dbase file.");
			
		try (Connection connection = getConnection()) {
		LOGGER.info("Import Pdb into DB: Obtained connection" );
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
						AttributeGetter attGetter = new AttributeGetter(sf.getFeatureType());
						
						int intVal = attGetter.getIntValue(Constants.PROFILE_ID_ATTR, sf);
						pdb.setProfileId(intVal);
						
						double dubVal = attGetter.getDoubleValue(Constants.BIAS_ATTR, sf);
						pdb.setBias(dubVal);

						double uncyDubVal = attGetter.getDoubleValue(Constants.BIAS_UNCY_ATTR, sf);
						pdb.setUncyb(uncyDubVal);

						BigInteger segmentId = getBigIntValue(Constants.SEGMENT_ID_ATTR, sf);  
						pdb.setSegmentId(segmentId);
						
						Geometry geom = (Geometry)sf.getDefaultGeometry();
						
						if (geom instanceof Point){
							Point point = (Point)geom;
							pdb.setX(point.getX());
							pdb.setY(point.getY());
						}
						else
						{
							throw new UnsupportedFeatureTypeException("Only Points supported for PDB inserts.");
						}
						
						pdbList.add(pdb);

						if (pdbList.size() == MAX_POINTS_AT_ONCE) { 
							insertPointsIntoPdbTable(connection, pdbList);  
							pdbList.clear();
						}
					} // close while
					
					//insert the remainder of the pdb points into the table
					if (pdbList.size() > 0){
						insertPointsIntoPdbTable(connection, pdbList);
					}
					
					viewName = createViewAgainstWorkspace(connection, workspace);
					if (StringUtils.isBlank(viewName)) {
						LOGGER.error("Unable to create pdb view against workspace.");
						throw new SQLException("Could not create view");
					}

					new PostgresDAO().addViewToMetadataTable(connection, viewName);

					connection.commit();
				} catch (NoSuchElementException | SQLException ex) {
					LOGGER.error("Error while attempting insert into PDB table. ", ex);
					connection.rollback();
					throw ex;
				} finally {
					iter.close();
				}
			}
		} catch (SchemaException | TransformException | FactoryException ex) {
			LOGGER.error("Error while inserting into PDB table. ", ex);
			throw ex;
		}
		return viewName;
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
