package gov.usgs.cida.utilities.service;

import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ServiceHelper {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ServiceHelper.class);

	public static RequestResponse.ResponseType getResponseType(HttpServletRequest request) {
		RequestResponse.ResponseType responseType = RequestResponse.ResponseType.XML;
		String responseEncoding = request.getParameter("response.encoding");
		if (StringUtils.isBlank(responseEncoding) || responseEncoding.toLowerCase(Locale.getDefault()).contains("json")) {
			responseType = RequestResponse.ResponseType.JSON;
		}
		return responseType;
	}

	public static void sendNotEnoughParametersError(HttpServletResponse response, String[] missingParams, RequestResponse.ResponseType responseType) {
		Map<String, String> responseMap = new HashMap<>(missingParams.length + 1);
		for (String missingParam : missingParams) {
			responseMap.put("error", missingParam + " parameter is required");
			responseMap.put("serverCode", "400");
			LOG.info("Request did not include " + missingParam + " parameter");
		}
		RequestResponse.sendErrorResponse(response, responseMap, responseType);
	}

	public static void sendServiceError(HttpServletResponse response, String[] errors, RequestResponse.ResponseType responseType) {
		Map<String, String> responseMap = new HashMap<>(errors.length + 1);
		for (String err : errors) {
			responseMap.put("error", err);
			responseMap.put("serverCode", "400");
		}
		RequestResponse.sendErrorResponse(response, responseMap, responseType);
	}

	private ServiceHelper() {
	}
}
