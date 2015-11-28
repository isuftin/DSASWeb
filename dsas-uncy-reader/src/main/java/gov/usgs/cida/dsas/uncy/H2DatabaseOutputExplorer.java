package gov.usgs.cida.dsas.uncy;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author isuftin
 */
public class H2DatabaseOutputExplorer extends DatabaseOutputXploder {

	public final static String HOST_PARAM = JDBCDataStoreFactory.HOST.getName();
	public final static String PORT_PARAM = JDBCDataStoreFactory.PORT.getName();
	public final static String DATABASE_PARAM = JDBCDataStoreFactory.DATABASE.getName();
	public final static String USERNAME_PARAM = JDBCDataStoreFactory.USER.getName();
	public final static String PASSWORD_PARAM = JDBCDataStoreFactory.PASSWD.getName();
	private final Map<String, Object> dbConfig = new HashMap<>();

	public H2DatabaseOutputExplorer(Map<String, String> config) {
		super(mergeMaps(config, ImmutableMap.of("dbtype", "h2")));

		String[] requiredConfigs = new String[]{
			HOST_PARAM,
			PORT_PARAM,
			DATABASE_PARAM,
			USERNAME_PARAM,
			PASSWORD_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for H2DatabaseOutputExplorer must include parameter %s", requiredConfig));
			}
			if (StringUtils.isBlank(config.get(requiredConfig))) {
				throw new IllegalArgumentException(String.format("Configuration map for H2DatabaseOutputExplorer must include value for parameter %s", requiredConfig));
			}
		}
		dbConfig.putAll(config);
		dbConfig.put(PORT_PARAM, Integer.parseInt(config.get(PORT_PARAM), 10));
	}

	@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx) throws IOException {
		SimpleFeatureType outputFeatureType = createOutputFeatureType();
		DataStore outputStore = DataStoreFinder.getDataStore(dbConfig);
		outputStore.createSchema(outputFeatureType);
		FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = outputStore.getFeatureWriterAppend(outputFeatureType.getTypeName(), tx);
		return featureWriter;
	}

}
