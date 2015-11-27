package gov.usgs.cida.dsas.service.util;

import gov.usgs.cida.utilities.file.TokenToFileSingleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
public class TokenFileExchanger {
	// private constructor prevents instantiation from external classes

	private TokenFileExchanger() {
	}

	/**
	 * Singleton Holder is Loaded on the first execution of the
	 * FileUtil.getInstance() or the first access to SingletonHolder.INSTANCE
	 * and not before. It is thread-safe without requiring synchronized or
	 * volatile as a result and prevents the double-checked locking idiom. --see
	 * Bill Pugh write up for more info
	 */
	private static class SingletonHolder {

		private static final TokenFileExchanger INSTANCE = new TokenFileExchanger();
	}

	public static TokenFileExchanger getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private static final Map<String, File> tokenToFileMap = Collections.synchronizedMap(new HashMap<String, File>());
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenFileExchanger.class);

	/**
	 * Adds the File object to the map. If the file already exists in the map,
	 * returns the token already associated with the file
	 *
	 * @param file
	 * @return token
	 */
	public static String getToken(File file) throws FileNotFoundException {
		if (file.exists()) {
			String filePath = file.getAbsolutePath();
			String token = null;
			Iterator<Map.Entry<String, File>> iterator = tokenToFileMap.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<String, File> entry = iterator.next();
				if (entry.getValue().getAbsolutePath().equals(filePath)) {
					token = entry.getKey();
				}
			}
			if (token == null) {
				token = UUID.randomUUID().toString();
			}

			tokenToFileMap.put(token, file);
			return token;
		}
		throw new FileNotFoundException("File could not be found - was not added to Token-To-File Map");
	}

	/**
	 * Using a token, retrieves a file from internal map.
	 *
	 * @param token
	 * @return null if token does not exist
	 */
	public static File getFile(String token) {
		File file = null;
		if (tokenToFileMap.containsKey(token)) {
			file = tokenToFileMap.get(token);
		}
		return file;
	}

	/**
	 * Removes a token from the map. Optionally deletes the associated file if
	 * it exists
	 *
	 * @param token
	 *
	 * @return true if the file was deleted
	 */
	public static boolean removeToken(String token) {
		boolean allFilesDeleted = true;
		if (StringUtils.isNotBlank(token) && tokenToFileMap.containsKey(token)) {
			File file = tokenToFileMap.get(token);
			tokenToFileMap.remove(token);
			if (null != file && file.exists()) {
				if (!FileUtils.deleteQuietly(file)) {
					LOGGER.info("Could not delete file " + file.getAbsolutePath() + " for token " + token);
					allFilesDeleted = false;
				}
			}
		}
		return allFilesDeleted;
	}

}
