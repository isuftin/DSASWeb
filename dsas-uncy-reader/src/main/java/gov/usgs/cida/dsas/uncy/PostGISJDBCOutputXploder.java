package gov.usgs.cida.dsas.uncy;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Point;
import static gov.usgs.cida.dsas.uncy.Xploder.GEOMETRY_FACTORY;
import java.io.IOException;
import java.util.Map;
import org.geotools.data.FeatureWriter;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class PostGISJDBCOutputXploder extends DatabaseOutputXploder {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostGISJDBCOutputXploder.class);
	public final static String HOST_PARAM = JDBCDataStoreFactory.HOST.key;
	public final static String PORT_PARAM = JDBCDataStoreFactory.PORT.key;
	public final static String DATABASE_PARAM = JDBCDataStoreFactory.DATABASE.key;
	public final static String SCHEMA_PARAM = JDBCDataStoreFactory.SCHEMA.key;
	public final static String USERNAME_PARAM = JDBCDataStoreFactory.USER.key;
	public final static String PASSWORD_PARAM = JDBCDataStoreFactory.PASSWD.key;

	public PostGISJDBCOutputXploder(Map<String, Object> config) throws IOException {
		super(mergeMaps(config, ImmutableMap.of(JDBCDataStoreFactory.DBTYPE.key, "postgis")));

		String[] requiredConfigs = new String[]{
			HOST_PARAM,
			PORT_PARAM,
			DATABASE_PARAM,
			SCHEMA_PARAM,
			USERNAME_PARAM,
			PASSWORD_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for PostGISJDBCOutputXploder must include parameter %s", requiredConfig));
			}
		}
		dbConfig.put(JDBCDataStoreFactory.DBTYPE.key, dbType);
		dbConfig.putAll(config);
		dbConfig.put(PORT_PARAM, config.get(PORT_PARAM));
	}

	@Override
	protected SimpleFeatureType createOutputFeatureType(String outputTypeName) throws IOException {
		JDBCDataStore createDataStore = null;
		try {
			createDataStore = new PostgisNGDataStoreFactory().createDataStore(dbConfig);
			return createDataStore.getSchema(outputTypeName);
		} finally {
			if (createDataStore != null) {
				createDataStore.dispose();
			}
		}
	}

	@Override
	public void writePoint(Point p, DbaseFileReader.Row row, double uncy, long recordId, int segmentId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException {

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
	protected JDBCDataStore getDataStore() throws IOException {
		return new PostgisNGDataStoreFactory().createDataStore(dbConfig);
	}

}
