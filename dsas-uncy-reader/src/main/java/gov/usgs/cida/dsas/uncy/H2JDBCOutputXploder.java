package gov.usgs.cida.dsas.uncy;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
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

	public H2JDBCOutputXploder(Map<String, Object> config) throws IOException {
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
		}
		dbConfig.put(JDBCDataStoreFactory.DBTYPE.key, dbType);
		dbConfig.putAll(config);
		dbConfig.put(PORT_PARAM, config.get(PORT_PARAM));
	}

	@Override
	protected JDBCDataStore getDataStore() throws IOException {
		return new H2DataStoreFactory().createDataStore(dbConfig);
	}

}
