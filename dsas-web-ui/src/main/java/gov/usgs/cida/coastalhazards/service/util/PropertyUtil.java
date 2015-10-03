package gov.usgs.cida.coastalhazards.service.util;

import gov.usgs.cida.config.DynamicReadOnlyProperties;
import gov.usgs.cida.utilities.properties.JNDISingleton;

/**
 *
 * @author isuftin
 */
public class PropertyUtil {

	private static DynamicReadOnlyProperties props = null;

	static {
		props = JNDISingleton.getInstance();
	}

	/**
	 * Get a property using Property enum.
	 *
	 * @param property
	 * @return null if property not found
	 */
	public static String getProperty(Property property) {
		return getProperty(property, null);
	}

	/**
	 * Get a property using enum.
	 *
	 * @param property
	 * @param defaultValue
	 * @return
	 */
	public static String getProperty(Property property, String defaultValue) {
		return props.getProperty(property.getKey(), defaultValue);
	}

	/**
	 * Get a property using String key
	 *
	 * @param key
	 * @return null if property not found
	 */
	public static String getProperty(String key) {
		return getProperty(key, null);
	}

	/**
	 * Get a property using String key
	 *
	 * @param key
	 * @param defaultValue String to return if property not found
	 * @return
	 */
	public static String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

}
