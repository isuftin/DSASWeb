package gov.usgs.cida.dsas.uncy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Creates an exploder which will output directly to a database.
 *
 * @see
 * <a href="http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html">http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html</a>
 * @author isuftin
 */
public abstract class DatabaseOutputXploder extends Xploder {

	public final static String DB_TYPE_PARAM = "dbtype";
	public final String dbType;
	protected final Map<String, Object> dbConfig = new HashMap<>();

	public DatabaseOutputXploder(Map<String, String> config) throws IOException {
		super(config);

		String[] requiredConfigs = new String[]{
			DB_TYPE_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for DatabaseOutputExploder must include parameter %s", requiredConfig));
			}
			if (StringUtils.isBlank(config.get(requiredConfig))) {
				throw new IllegalArgumentException(String.format("Configuration map for DatabaseOutputExploder must include value for parameter %s", requiredConfig));
			}
		}

		this.dbType = config.get(DB_TYPE_PARAM);
	}

	public static Map<String, String> mergeMaps(Map<String, String> m1, Map<String, String> m2) {
		Map<String, String> mergedMap = new HashMap<>(m1.size() + m2.size());
		mergedMap.putAll(m1);
		mergedMap.putAll(m2);
		return mergedMap;
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
