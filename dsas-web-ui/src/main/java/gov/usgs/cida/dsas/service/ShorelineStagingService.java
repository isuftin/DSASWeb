package gov.usgs.cida.dsas.service;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.rest.service.shapefile.ShapefileImportProcess;
import gov.usgs.cida.dsas.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.shoreline.file.IShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineFileFactory;
import gov.usgs.cida.dsas.shoreline.file.TokenToShorelineFileSingleton;
import gov.usgs.cida.dsas.utilities.service.ServiceHelper;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse.ResponseType;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.ws.rs.core.Response;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.LoggerFactory;

/**
 * Receives a shapefile from the client, reads the featuretype from it and sends
 * back a file token which will later be used to read in the shoreline file,
 * rename columns and finally import it into the geospatial server as a resource
 *
 * @author isuftin
 */
@MultipartConfig
public class ShorelineStagingService extends HttpServlet {

	private static final long serialVersionUID = 2377995353146379768L;
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineStagingService.class);
	private static final Map<String, String> tokenMap = new HashMap<>();
	private final static String TOKEN_STRING = "token";
	private final static String ACTION_STRING = "action";
	private final static String STAGE_ACTION_STRING = "stage";
	private final static String IMPORT_ACTION_STRING = "import";
	private final static String READDBF_ACTION_STRING = "read-dbf";
	private final static String DELETE_TOKEN_ACTION_STRING = "delete-token";

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean success = false;
		Map<String, String> responseMap = new HashMap<>();
		ResponseType responseType = ServiceHelper.getResponseType(request);

		String action = request.getParameter(ACTION_STRING);

		if (StringUtils.isBlank(action)) {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{ACTION_STRING}, responseType);
		} else if (action.equalsIgnoreCase(STAGE_ACTION_STRING)) {
			// Client is uploading a file. I want to stage the file and return a token
			ShorelineFile shorelineFile = null;
			try {
				Part zipFilePart = request.getPart("file");
				
				ShorelineFileFactory shorelineFactory;
				try (InputStream inputStream = zipFilePart.getInputStream();) {
					shorelineFactory = new ShorelineFileFactory(request, inputStream);
				}
				
				shorelineFile = shorelineFactory.buildShorelineFile();
				String token = TokenToShorelineFileSingleton.addShorelineFile(shorelineFile);
				responseMap.put(TOKEN_STRING, token);
				success = true;
			} catch (FileUploadException | IOException | ShorelineFileFormatException ex) {
				if (shorelineFile != null) {
					shorelineFile.clear();
				}
				sendException(response, "Could not stage file", ex, responseType);
			}
		} else if (action.equalsIgnoreCase(IMPORT_ACTION_STRING)) {
			// Client is requesting to import a file associated with a token
			String token = request.getParameter(TOKEN_STRING);

			if (StringUtils.isNotBlank(token)) {
				String columnsString = request.getParameter("columns");
				Map<String, String> columns = new HashMap<>();
				if (StringUtils.isNotBlank(columnsString)) {
					columns = new Gson().fromJson(columnsString, Map.class);
					ShapefileImportProcess process = new ShapefileImportProcess(token, columns);
					Thread thread = new Thread(process);
					thread.start();
					response.addHeader(HttpHeaders.LOCATION, ServiceURI.PROCESS_SERVICE_ENDPOINT + "/" + process.getProcessId());
					response.setStatus(Response.Status.ACCEPTED.getStatusCode());
					IOUtils.copy(new ByteArrayInputStream(new byte[0]), response.getWriter());
					response.flushBuffer();
				} else {
					ServiceHelper.sendNotEnoughParametersError(response, new String[]{"columns"}, responseType);
				}
				
			} else {
				ServiceHelper.sendNotEnoughParametersError(response, new String[]{TOKEN_STRING}, responseType);
			}
		} else {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{STAGE_ACTION_STRING, IMPORT_ACTION_STRING}, responseType);
		}

		if (success) {
			RequestResponse.sendSuccessResponse(response, responseMap, responseType);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		ResponseType responseType = ServiceHelper.getResponseType(request);
		Map<String, String> responseMap = new HashMap<>();

		String action = request.getParameter(ACTION_STRING);
		if (StringUtils.isBlank(action)) {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{ACTION_STRING}, responseType);
		} else if (action.equalsIgnoreCase(DELETE_TOKEN_ACTION_STRING)) {
			String token = request.getParameter(TOKEN_STRING);
			if (StringUtils.isBlank(token)) {
				ServiceHelper.sendNotEnoughParametersError(response, new String[]{TOKEN_STRING}, responseType);
			} else {
				IShorelineFile shorelineFile = TokenToShorelineFileSingleton.getShorelineFile(token);

				if (null != shorelineFile && shorelineFile.exists()) {
					shorelineFile.clear();
				}
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ResponseType responseType = ServiceHelper.getResponseType(request);
		Map<String, String> responseMap = new HashMap<>();
		boolean success = false;

		String action = request.getParameter(ACTION_STRING);
		if (StringUtils.isBlank(action)) {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{ACTION_STRING}, responseType);
		} else if (action.equalsIgnoreCase(READDBF_ACTION_STRING)) {
			String token = request.getParameter(TOKEN_STRING);
			if (StringUtils.isBlank(token)) {
				ServiceHelper.sendNotEnoughParametersError(response, new String[]{TOKEN_STRING}, responseType);
			} else {
				// Future
				IShorelineFile shorelineFile = null;
				try {
					shorelineFile = TokenToShorelineFileSingleton.getShorelineFile(token);

					if (null == shorelineFile || !shorelineFile.exists()) {
						throw new FileNotFoundException();
					}
				} catch (FileNotFoundException ex) {
					if (null != shorelineFile) {
						shorelineFile.clear();
					}
					throw ex;
				}

				String[] columns = shorelineFile.getColumns();
				String commaSepColumns = StringUtils.join(columns, ",");
				responseMap.put("headers", commaSepColumns);
				success = true;
			}
		} else {
			ServiceHelper.sendNotEnoughParametersError(response, new String[]{READDBF_ACTION_STRING}, responseType);
		}

		if (success) {
			RequestResponse.sendSuccessResponse(response, responseMap, responseType);
		} else {
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
		}
	}

	private void sendException(HttpServletResponse response, String error, Throwable t, ResponseType responseType) {
		Map<String, String> responseMap = new HashMap<>(1);
		responseMap.put("error", error);
		RequestResponse.sendErrorResponse(response, responseMap, responseType);
		LOGGER.warn(t.getMessage());
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return " * Receives a shapefile from the client, reads the featuretype from it and sends"
				+ " * back a file token which will later be used to read in the shoreline file, rename"
				+ " * columns and finally import it into the geospatial server as a resource";
	}

	/**
	 * Will try to delete files in token map on server shutdown
	 */
	@Override
	public void destroy() {
		TokenToShorelineFileSingleton.clear();
	}

}
