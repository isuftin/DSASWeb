package gov.usgs.cida.dsas.dao.shoreline;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.shoreline.file.ShorelineFile;
import gov.usgs.cida.dsas.uncy.ShapefileOutputXploder;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
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
		String viewName = null;
		BidiMap bm = new DualHashBidiMap(columns);
		String dateFieldName = (String) bm.getKey(Constants.DB_DATE_ATTR);
		String uncertaintyFieldName = (String) bm.getKey(Constants.UNCY_ATTR);
		String mhwFieldName = (String) bm.getKey(Constants.MHW_ATTR);
		String orientation = null; // Not yet sure what to do here
		String baseFileName = FilenameUtils.getBaseName(shpFile.getName());
		String name = baseFileName;
		File parentDirectory = shpFile.getParentFile();
		deleteExistingPointFiles(parentDirectory);
		String[][] fieldNames = null;
		int MAX_POINTS_AT_ONCE = 500;

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

		//TODO- Check if incoming shapefile is not already a points shapefile
		Map<String, String> config = new HashMap<>(3);
		config.put(ShapefileOutputXploder.UNCERTAINTY_COLUMN_PARAM, uncertaintyFieldName);
		config.put(ShapefileOutputXploder.INPUT_FILENAME_PARAM, shpFile.getAbsolutePath());
		LOGGER.debug("Exploding shapefile at {}", shpFile.getAbsolutePath());
		updateProcessInformation("Exploding shapefile");
		File outputFile = null;
		int pointCount = 0;
		try (ShapefileOutputXploder xploder = new ShapefileOutputXploder(config)) {
			pointCount = xploder.explode();
			outputFile = xploder.getOutputFile();
		} catch (Exception ex) {
			LOGGER.warn("There was an issue during exploding the Shapefile to points", ex);
		}

		LOGGER.debug("Shapefile exploded");
		updateProcessInformation(String.format("Shapefile exploded to %s points", pointCount));

		FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(outputFile.toURI().toURL());
		int fcSize = fc.size();
		updateProcessInformation(String.format("Will attempt to enter %s features into the database", fcSize));
		Class<?> dateType = fc.getSchema().getDescriptor(dateFieldName).getType().getBinding();
		List<double[]> xyUncies = new ArrayList<>();

		try (Connection connection = getConnection()) {
			if (!fc.isEmpty()) {
				try (FeatureIterator<SimpleFeature> iter = fc.features()) {
					connection.setAutoCommit(false);
					int lastSegmentId = -1;
					long shorelineId = -1;
					while (iter.hasNext()) {
						SimpleFeature sf = iter.next();
						int segmentId = getIntValue(Constants.SEGMENT_ID_ATTR, sf);

						if (lastSegmentId != segmentId) {
							// Either I'm looping for the first time or I've got 
							// a new shoreline that I need to insert. I should 
							// probably insert all of the points for the previous 
							/// shoreline
							if (xyUncies.size() > 0 && shorelineId != -1) {
								insertPointsIntoShorelinePointsTable(connection, shorelineId, lastSegmentId, xyUncies.toArray(new double[xyUncies.size()][]));
								xyUncies.clear();
							}

							// Now I'm ready for the next shoreline
							String source = getSourceFromFC(sf);
							boolean mhw = false;
							Date date = getDateFromFC(dateFieldName, sf, dateType);

							if (StringUtils.isNotBlank(mhwFieldName)) {
								mhw = getBooleanValue(mhwFieldName, sf, false);
							}

							if (StringUtils.isBlank(source)) {
								source = baseFileName;
							}
							shorelineId = insertToShorelinesTable(connection, workspace, date, mhw, source, name, orientation, null);

							if (fieldNames != null && fieldNames.length > 0) {
								Map<String, String> auxCols = getAuxillaryColumnsFromFC(sf, fieldNames);
								for (Entry<String, String> auxEntry : auxCols.entrySet()) {
									if (StringUtils.isNotBlank(auxEntry.getValue())) {
										insertAuxillaryAttribute(connection, shorelineId, auxEntry.getKey(), auxEntry.getValue());
									}
								}
							}

							lastSegmentId = segmentId;
						}
						xyUncies.add(getXYAndUncertaintyFromSimpleFeature(sf, uncertaintyFieldName));

						if (xyUncies.size() == MAX_POINTS_AT_ONCE) {
							insertPointsIntoShorelinePointsTable(connection, shorelineId, segmentId, xyUncies.toArray(new double[xyUncies.size()][]));
							xyUncies.clear();
						}
					}

					if (xyUncies.size() > 0) {
						insertPointsIntoShorelinePointsTable(connection, shorelineId, lastSegmentId, xyUncies.toArray(new double[xyUncies.size()][]));
						xyUncies.clear();
					}

					viewName = createViewAgainstWorkspace(connection, workspace);
					if (StringUtils.isBlank(viewName)) {
						throw new SQLException("Could not create view");
					}

					new PostgresDAO().addViewToMetadataTable(connection, viewName);

					connection.commit();
				} catch (NamingException | NoSuchElementException | ParseException | SQLException ex) {
					connection.rollback();
					throw ex;
				}
			}
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

	public double getDoubleValue(String attribute, SimpleFeature feature) {
		Object value = feature.getAttribute(attribute);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			throw new ClassCastException("This attribute is not a floating point value");
		}
	}

	public boolean getBooleanValue(String attribute, SimpleFeature feature, boolean defaultValue) {
		Object value = feature.getAttribute(attribute);
		return AttributeGetter.extractBooleanValue(value, defaultValue);
	}

	private double[] getXYAndUncertaintyFromSimpleFeature(SimpleFeature sf, String uncertaintyFieldName) {
		double x = sf.getBounds().getMaxX();
		double y = sf.getBounds().getMaxY();
		double uncertainty = getDoubleValue(uncertaintyFieldName, sf);
		return new double[]{x, y, uncertainty};
	}

	private Collection<File> deleteExistingPointFiles(File directory) {
		Collection<File> existingPointFiles = FileUtils.listFiles(directory, new PrefixFileFilter("*_pts"), null);
		for (File existingPtFile : existingPointFiles) {
			existingPtFile.delete();
		}
		return existingPointFiles;
	}

	private Date getDateFromFC(String dateFieldName, SimpleFeature sf, Class<?> fromType) throws ParseException {
		Date result = null;

		if (fromType == java.lang.String.class) {
			String dateString = (String) sf.getAttribute(dateFieldName);

			try {
				result = new SimpleDateFormat("MM/dd/yyyy").parse(dateString);
			} catch (ParseException ex) {
				LOGGER.debug("Could not parse date in format 'MM/dd/yyyy' - Will try non-padded", ex);
			}

			if (null == result) {
				result = new SimpleDateFormat("M/d/yyyy").parse(dateString);
			}
		} else if (fromType == java.util.Date.class) {
			result = (Date) sf.getAttribute(dateFieldName);
		}

		return result;
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

	private Map<String, String> getAuxillaryColumnsFromFC(SimpleFeature sf, String[][] fieldNames) {
		// C (Character)  = String
		// N (Numeric)   = Integer or Long or Double (depends on field's decimal count and fieldLength)
		// F (Floating)  = Double
		// L (Logical)   = Boolean
		// D (Date)		 = java.util.Date (Without time)
		// @ (Timestamp) = java.sql.Timestamp (With time)
		// Unknown		 = String
		Map<String, String> auxillaryAttributes = new HashMap<>();
		for (String attribute : ShorelineFile.AUXILLARY_ATTRIBUTES) {
			for (String[] fname : fieldNames) {
				String fieldName = fname[0];
				char fieldType = fname[1].charAt(0);
				String cleanedFieldName = fieldName.trim().replaceAll("_", "");
				String cleanedAttribute = attribute.trim().replaceAll("_", "");
				if (cleanedFieldName.equalsIgnoreCase(cleanedAttribute)) {
					Object attrObj = sf.getAttribute(fieldName);
					if (attrObj != null) {
						switch (fieldType) {
							case 'D':
							case '@':
								auxillaryAttributes.put(attribute.toLowerCase(), String.valueOf(((Date) attrObj).getTime()));
								break;
							case 'L':
							case 'N':
							case 'F':
							case 'C':
							default:
								auxillaryAttributes.put(attribute.toLowerCase(), String.valueOf(sf.getAttribute(fieldName)));
						}
					}
				}
			}

		}
		return auxillaryAttributes;
	}
}
