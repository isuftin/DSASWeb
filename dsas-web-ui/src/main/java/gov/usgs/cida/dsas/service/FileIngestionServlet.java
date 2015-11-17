package gov.usgs.cida.dsas.service;

import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author isuftin
 */
public abstract class FileIngestionServlet extends HttpServlet {
	
	protected static Integer maxFileSize;
	private static final Integer DEFAULT_MAX_FILE_SIZE = Integer.MAX_VALUE;
	
	@Override
	protected abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

	@Override
	protected abstract void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

	protected Integer getMaxFileSize() {
		Integer maxFSize = DEFAULT_MAX_FILE_SIZE;
		String mfsJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_MAX_SIZE);
		if (StringUtils.isNotBlank(mfsJndiProp)) {
			maxFSize = Integer.parseInt(mfsJndiProp);
		}
		return maxFSize;
	}
	
	protected String cleanFileName(String input) {
		String updated = input;

		// Test the first character and if numeric, prepend with underscore
		if (input.substring(0, 1).matches("[0-9]")) {
			updated = "_" + input;
		}

		// Test the rest of the characters and replace anything that's not a 
		// letter, digit or period with an underscore
		char[] inputArr = updated.toCharArray();
		for (int cInd = 0; cInd < inputArr.length; cInd++) {
			if (!Character.isLetterOrDigit(inputArr[cInd]) && !(inputArr[cInd] == '.')) {
				inputArr[cInd] = '_';
			}
		}
		return String.valueOf(inputArr);
	}
}
