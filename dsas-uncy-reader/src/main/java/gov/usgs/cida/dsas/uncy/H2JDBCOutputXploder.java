package gov.usgs.cida.dsas.uncy;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class H2JDBCOutputXploder extends DatabaseOutputXploder {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2JDBCOutputXploder.class);
	public final static String HOST_PARAM = JDBCDataStoreFactory.HOST.key;
	public final static String PORT_PARAM = JDBCDataStoreFactory.PORT.key;
	public final static String DATABASE_PARAM = JDBCDataStoreFactory.DATABASE.key;
	public final static String USERNAME_PARAM = JDBCDataStoreFactory.USER.key;
	public final static String PASSWORD_PARAM = JDBCDataStoreFactory.PASSWD.key;

	public H2JDBCOutputXploder(Map<String, String> config) {
		super(mergeMaps(config, ImmutableMap.of(JDBCDataStoreFactory.DBTYPE.key, "h2")));

		String[] requiredConfigs = new String[]{
			HOST_PARAM,
			PORT_PARAM,
			DATABASE_PARAM,
			USERNAME_PARAM,
			PASSWORD_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for H2JDBCOutputXploder must include parameter %s", requiredConfig));
			}
			if (StringUtils.isBlank(config.get(requiredConfig))) {
				throw new IllegalArgumentException(String.format("Configuration map for H2JDBCOutputXploder must include value for parameter %s", requiredConfig));
			}
		}
		dbConfig.put(JDBCDataStoreFactory.DBTYPE.key, dbType);
		dbConfig.putAll(config);
		dbConfig.put(PORT_PARAM, Integer.parseInt(config.get(PORT_PARAM), 10));
	}
	
	@Override // TODO- Get this from the database - This is too specific right now 
	protected SimpleFeatureType createOutputFeatureType(String outputTypeName) throws IOException {
		SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
		ftb.setName("points");
		ftb.setSRS("EPSG:4326");
		ftb.add("GEOM", Point.class, DefaultGeographicCRS.WGS84);
		ftb.add("UNCY", Double.class);
		ftb.add("SHORELINE_ID", BigInteger.class);
		ftb.add("SEGMENT_ID", BigInteger.class);
		return ftb.buildFeatureType();
	}
	
	@Override
	public void writePoint(Point p, DbaseFileReader.Row row, double uncy, int recordId, int segmentId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException {

		SimpleFeature writeFeature = featureWriter.next();

		// geometry field is first, otherwise we lose.
		Point np = GEOMETRY_FACTORY.createPoint(p.getCoordinate());
		writeFeature.setAttribute(0, recordId);
		writeFeature.setAttribute(1, segmentId);
		writeFeature.setAttribute(2, np);
		writeFeature.setAttribute(3, row.read(uncertaintyIdIdx));

		featureWriter.write();
	}

		@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx) throws IOException {
		if (outputFeatureType == null) {
			outputFeatureType = createOutputFeatureType();
		}
		DataStore ds = DataStoreFinder.getDataStore(dbConfig);
		FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = ds.getFeatureWriterAppend(outputFeatureType.getName().getLocalPart(), tx);
		
		return featureWriter;
	}

}
