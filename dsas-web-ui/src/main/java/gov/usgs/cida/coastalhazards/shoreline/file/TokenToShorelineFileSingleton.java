package gov.usgs.cida.coastalhazards.shoreline.file;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class TokenToShorelineFileSingleton {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenToShorelineFileSingleton.class);
	private static final Map<String, IShorelineFile> tokenToFileMap = Collections.synchronizedMap(new HashMap<String, IShorelineFile>());

	/**
	 * Adds the File object to the map. If the file already exists in the map,
	 * returns the token already associated with the file
	 *
	 * @param shorelineFile
	 * @return token
	 */
	public static String addShorelineFile(IShorelineFile shorelineFile) {
		String token = null;
		if (shorelineFile == null) {
			throw new NullPointerException();
		}

		Set<String> kSet = tokenToFileMap.keySet();

		Iterator<String> kIter = kSet.iterator();
		while (kIter.hasNext() && null == token) {
			String key = kIter.next();
			if (tokenToFileMap.get(key).equals(shorelineFile)) {
				token = key;
			}
		}

		if (token == null) {
			token = UUID.randomUUID().toString();
		}

		tokenToFileMap.put(token, shorelineFile);
		return token;
	}

	/**
	 * Using a token, retrieves a file from internal map.
	 *
	 * @param token
	 * @return null if token does not exist
	 */
	public static IShorelineFile getShorelineFile(String token) {
		IShorelineFile file = null;
		if (tokenToFileMap.containsKey(token)) {
			file = tokenToFileMap.get(token);
		}
		return file;
	}

	public static void clear() {
		for (IShorelineFile shorelineFile : tokenToFileMap.values()) {
			shorelineFile.clear();
		}
		tokenToFileMap.clear();
	}

	/**
	 * Removes a token from the map.
	 *
	 * @param token
	 */
	public static void removeToken(String token) {
		if (StringUtils.isNotBlank(token) && tokenToFileMap.containsKey(token)) {
			ShorelineFile shorelineFile = (ShorelineFile) tokenToFileMap.remove(token);
			shorelineFile.deleteDirectory();
		}
	}

	private TokenToShorelineFileSingleton() {
	}
}
