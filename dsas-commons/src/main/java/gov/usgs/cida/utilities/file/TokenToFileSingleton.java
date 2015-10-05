package gov.usgs.cida.utilities.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class TokenToFileSingleton {

	private static final Map<String, File> tokenToFileMap = Collections.synchronizedMap(new HashMap<String, File>());
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokenToFileSingleton.class);

	/**
	 * Adds the File object to the map. If the file already exists in the map,
	 * returns the token already associated with the file
	 *
	 * @param file
	 * @return token
	 */
	public static String addFile(File file) {
		if (file.exists()) {
			String filePath = file.getAbsolutePath();
			Iterator<Entry<String, File>> iterator = tokenToFileMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, File> entry = iterator.next();
				if (entry.getValue().getAbsolutePath().equals(filePath)) {
					return entry.getKey();
				}
			}
			String token = UUID.randomUUID().toString();
			tokenToFileMap.put(token, file);
			return token;
		}
		throw new RuntimeException(new FileNotFoundException("File could not be found - was not added to Token-To-File Map"));
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
	 * @param deleteFile should the file that may be mapped to the token also be
	 * deleted?
	 * @return if deleteFile parameter is true and all files were deleted
	 */
	public static boolean removeToken(String token, boolean deleteFile) {
		boolean allFilesDeleted = deleteFile;
		if (StringUtils.isNotBlank(token) && tokenToFileMap.containsKey(token)) {
			File file = tokenToFileMap.get(token);
			tokenToFileMap.remove(token);
			if (deleteFile && null != file && file.exists()) {
				if (!FileUtils.deleteQuietly(file)) {
					LOGGER.info("Could not delete file " + file.getAbsolutePath() + " for token " + token);
					allFilesDeleted = false;
				}
			}
		}
		return allFilesDeleted;
	}

	private TokenToFileSingleton() {
	}
}
