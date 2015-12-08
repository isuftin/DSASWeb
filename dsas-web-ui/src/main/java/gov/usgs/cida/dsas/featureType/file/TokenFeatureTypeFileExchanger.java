package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.service.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Either accepts a file and returns a token (random uuid string) or exchanges
 * the token for the file.
 *
 * @author smlarson
 */
public class TokenFeatureTypeFileExchanger {

	private TokenFeatureTypeFileExchanger() {
		// private constructor prevents instantiation from external classes
	}

	/**
	 * Singleton Holder is Loaded on the first execution of the
	 * FileUtil.getInstance() or the first access to SingletonHolder.INSTANCE
	 * and not before. It is thread-safe without requiring synchronized or
	 * volatile as a result and prevents the double-checked locking idiom. --see
	 * Bill Pugh write up for more info
	 */
	private static class SingletonHolder {

		private static final TokenFeatureTypeFileExchanger INSTANCE = new TokenFeatureTypeFileExchanger();
	}

	public static TokenFeatureTypeFileExchanger getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private static final Map<String, FeatureTypeFile> tokenToFileMap = Collections.synchronizedMap(new HashMap<String, FeatureTypeFile>());
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenFeatureTypeFileExchanger.class);

	/**
	 * Adds the File object to the map. If the file already exists in the map,
	 * returns the token already associated with the file
	 *
	 * @param file
	 * @return token
	 * @throws java.io.FileNotFoundException
	 */
	public static String getToken(FeatureTypeFile featureTypeFile) throws FileNotFoundException {
		String token = null;
		if (featureTypeFile == null) {
			throw new NullPointerException();
		}

		Set<String> kSet = tokenToFileMap.keySet();

		Iterator<String> kIter = kSet.iterator();
		while (kIter.hasNext() && null == token) {
			String key = kIter.next();
			if (tokenToFileMap.get(key).equals(featureTypeFile)) {
				token = key;
			}
		}

		if (token == null) {
			token = UUID.randomUUID().toString();
		}

		tokenToFileMap.put(token, featureTypeFile);
		return token;
	}

	/**
	 * Using a token, retrieves a file from internal map.
	 *
	 * @param token
	 * @return null if token does not exist
	 */
	public static FeatureTypeFile getFeatureTypeFile(String token) {
		FeatureTypeFile ftfile = null;
		if (tokenToFileMap.containsKey(token)) {
			ftfile = tokenToFileMap.get(token);
		}
		return ftfile;
	}

	/**
	 * Removes a token from the map. Optionally deletes the associated file if
	 * it exists
	 *
	 * @param token
	 *
	 * @return true if the file was deleted
	 */
	public static void removeToken(String token) {
		if (StringUtils.isNotBlank(token) && tokenToFileMap.containsKey(token)) {
			FeatureTypeFile shorelineFile = (FeatureTypeFile) tokenToFileMap.remove(token);
		}
	}

}
