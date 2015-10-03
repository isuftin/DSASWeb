package gov.usgs.cida.coastalhazards.service;

import com.google.gson.GsonBuilder;
import gov.usgs.cida.coastalhazards.dao.postgres.PostgresDAO;
import gov.usgs.cida.coastalhazards.dao.shoreline.Shoreline;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import gov.usgs.cida.utilities.service.ServiceHelper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShorelineService extends HttpServlet {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineService.class);
	public final static String ACTION_STRING = "action";
	public final static String GET_AUX_NAMES_ACTION_STRING = "getAuxillaryNames";
	public final static String UPDATE_AUX_NAME_ACTION_STRING = "updateAuxillaryName";
	public final static String GET_DATE_TO_AUX_MAP_ACTION_STRING = "getDateToAuxValues";
	public final static String GET_SHORELINES_WITH_BBOX_ACTION_STRING = "getShorelinesWithBbox";
	public final static String SHORELINES_PARAM_STRING = "shorelines";
	public final static String WORKSPACE_PARAM_STRING = "workspace";
	public final static String BBOX_PARAM_STRING = "bbox";
	public final static String AUX_NAMES_TOKEN_STRING = "names";
	public final static String AUX_NAME_TOKEN_STRING = "name";
	public final static String VALUES_TOKEN_STRING = "values";
	public final static String UPDATED_TOKEN_STRING = "updated";

	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		RequestResponse.ResponseType responseType = ServiceHelper.getResponseType(request);
		Map<String, String> responseMap = new HashMap<>();
		String action = request.getParameter(ACTION_STRING);
		String workspace = request.getParameter(WORKSPACE_PARAM_STRING);

		if (StringUtils.isBlank(action)) {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{ACTION_STRING}, responseType);
		} else if (StringUtils.isBlank(workspace)) {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{WORKSPACE_PARAM_STRING}, responseType);
		} else if (action.equalsIgnoreCase(GET_AUX_NAMES_ACTION_STRING)) {
			try {
				String[] auxillaryNames = new PostgresDAO().getAvailableAuxillaryNamesFromWorkspace(workspace);
				responseMap.put(AUX_NAMES_TOKEN_STRING, new GsonBuilder().create().toJson(auxillaryNames));
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			} catch (SQLException error) {
				responseMap.put("error", error.getMessage());
				RequestResponse.sendErrorResponse(response, responseMap, responseType);
				LOGGER.warn(error.getMessage());
			}
		} else if (action.equalsIgnoreCase(GET_DATE_TO_AUX_MAP_ACTION_STRING)) {
			try {
				PostgresDAO dao = new PostgresDAO();
				Map<String, String> mapping = dao.getShorelineDateToAuxValueMap(workspace);
				String auxName = dao.getCurrentShorelineAuxNameForWorkspace(workspace);
				responseMap.put(AUX_NAME_TOKEN_STRING, auxName);
				responseMap.put(VALUES_TOKEN_STRING, new GsonBuilder().create().toJson(mapping));
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			} catch (SQLException error) {
				responseMap.put("error", error.getMessage());
				RequestResponse.sendErrorResponse(response, responseMap, responseType);
				LOGGER.warn(error.getMessage());
			}
		} else if (action.equalsIgnoreCase(GET_SHORELINES_WITH_BBOX_ACTION_STRING)) {
			try {
				PostgresDAO dao = new PostgresDAO();
				double[] bbox = new double[4];
				String bboxString = request.getParameter(BBOX_PARAM_STRING);
				if (StringUtils.isBlank(bboxString)) {
					throw new IllegalArgumentException("bbox parameter is required");
				}
				String[] bboxCorners = bboxString.split(",");
				if (bboxCorners.length != 4) {
					throw new IllegalArgumentException("bbox requires 4 coordinates: minLon, minLat, maxLon, maxLat");
				}
				for (int cornerInd = 0;cornerInd < bboxCorners.length;cornerInd++) {
					bbox[cornerInd] = Double.parseDouble(bboxCorners[cornerInd]);
				}
				
				List<Shoreline> shorelinesFromBoundingBox = dao.getShorelinesFromBoundingBox(workspace + "_" + SHORELINES_PARAM_STRING, bbox);
				responseMap.put(SHORELINES_PARAM_STRING, new GsonBuilder().create().toJson(shorelinesFromBoundingBox));
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			}catch (SQLException | IllegalArgumentException error) {
				responseMap.put("error", error.getMessage());
				RequestResponse.sendErrorResponse(response, responseMap, responseType);
				LOGGER.warn(error.getMessage());
			}
		} else {
			responseMap.put("serverCode", "404");
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
		}

	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		RequestResponse.ResponseType responseType = ServiceHelper.getResponseType(request);
		Map<String, String> responseMap = new HashMap<>();
		String action = request.getParameter(ACTION_STRING);
		if (action.equalsIgnoreCase(UPDATE_AUX_NAME_ACTION_STRING)) {
			String workspace = request.getParameter(WORKSPACE_PARAM_STRING);
			String auxName = request.getParameter(AUX_NAME_TOKEN_STRING);
			if (StringUtils.isNotBlank(workspace)) {
				boolean updated = false;
				try {
					updated = new PostgresDAO().updateShorelineAuxillaryName(workspace, auxName);
					LOGGER.debug("Updated auxillary column on workspace {} to {}", workspace, auxName);
				} catch (SQLException e) {
					LOGGER.warn("Could not update auxillary column for workspace " + workspace + " to " + auxName, e);
				}
				responseMap.put(UPDATED_TOKEN_STRING, Boolean.toString(updated));
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			} else {
				ServiceHelper.sendNotEnoughParametersError(response, new String[]{WORKSPACE_PARAM_STRING, AUX_NAME_TOKEN_STRING}, responseType);
			}
		}
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Map<String, String> responseMap = new HashMap<>();
		responseMap.put("serverCode", "405");
		RequestResponse.ResponseType responseType = ServiceHelper.getResponseType(request);
		RequestResponse.sendErrorResponse(response, responseMap, responseType);
	}

}
