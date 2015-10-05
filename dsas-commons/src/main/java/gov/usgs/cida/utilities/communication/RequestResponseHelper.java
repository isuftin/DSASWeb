package gov.usgs.cida.utilities.communication;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author isuftin
 */
public class RequestResponseHelper {

    public static void sendErrorResponse(HttpServletResponse response, Map<String, String> responseMap) {
        responseMap.put("success", "false");
        sendJSONResponse(response, responseMap);
    }

    public static void sendSuccessResponse(HttpServletResponse response, Map<String, String> responseMap) {
        responseMap.put("success", "true");
        sendJSONResponse(response, responseMap);
    }

    static void sendJSONResponse(HttpServletResponse response, Map<String, String> responseMap) {
		if (!Boolean.parseBoolean(responseMap.get("success"))) {
			int serverCode = 500;
			if (responseMap.containsKey("serverCode")) {
				serverCode = Integer.parseInt(responseMap.get("serverCode"));
			}
			response.setStatus(serverCode);
		}
		
		String responseContent = new Gson().toJson(responseMap);
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Content-Length", Integer.toString(responseContent.length()));

		Writer writer = null;
		try {
			writer = response.getWriter();
			writer.write(responseContent);
		} catch (IOException ex) {
			Logger.getLogger(RequestResponseHelper.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ex) {
					Logger.getLogger(RequestResponseHelper.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
    }
    
}
