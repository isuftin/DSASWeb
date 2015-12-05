package gov.usgs.cida.dsas.uncy;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCJNDIDataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An XPloder implementation for the JNDI accessor for PostGIS
 *
 * @author isuftin
 */
public class PostGISJNDIOutputXploder extends DatabaseOutputXploder {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostGISJNDIOutputXploder.class);
	public final static String JNDI_PARAM = JDBCJNDIDataStoreFactory.JNDI_REFNAME.key;

	public PostGISJNDIOutputXploder(Map<String, Object> config) throws IOException {
		super(mergeMaps(config, ImmutableMap.of(JDBCDataStoreFactory.DBTYPE.key, "postgis")));

		String[] requiredConfigs = new String[]{
			JNDI_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for PostGISJNDIOutputXploder must include parameter %s", requiredConfig));
			}
		}

		dbConfig.put(JDBCDataStoreFactory.DBTYPE.key, dbType);
		dbConfig.putAll(config);
	}

	@Override
	protected JDBCDataStore getDataStore() throws IOException {
		return new PostgisNGJNDIDataStoreFactory().createDataStore(dbConfig);
	}

}
