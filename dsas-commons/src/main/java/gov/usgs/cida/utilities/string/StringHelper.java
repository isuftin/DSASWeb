package gov.usgs.cida.utilities.string;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author isuftin
 */
public class StringHelper {

	public static String makeSHA1Hash(String str) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.reset();
		byte[] buffer = str.getBytes();
		md.update(buffer);
		byte[] digest = md.digest();

		String hexStr = "";
		for (int i = 0; i < digest.length; i++) {
			hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
		}
		return hexStr;
	}
}
