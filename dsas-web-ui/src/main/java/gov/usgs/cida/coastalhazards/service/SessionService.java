package gov.usgs.cida.coastalhazards.service;

import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.coastalhazards.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.utilities.communication.RequestResponseHelper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class SessionService extends HttpServlet {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SessionService.class);
	private static GeoserverDAO geoserverHandler = null;
	private static String geoserverEndpoint = null;
	private static String geoserverUsername = null;
	private static String geoserverPassword = null;
	private static String geoserverDataDir = null;
	private static final long serialVersionUID = 5022377389976105019L;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init();
		geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		geoserverDataDir = PropertyUtil.getProperty(Property.GEOSERVER_DATA_DIRECTORY);
		geoserverHandler = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = request.getParameter("action");
		String workspace = request.getParameter("workspace");
		String layer = request.getParameter("layer");
		String store = request.getParameter("store");
		Map<String, String> responseMap = new HashMap<>();

		if (!StringUtils.isEmpty(action)) {
			action = action.trim().toLowerCase(Locale.US);
			if ("prepare".equals(action)) {
				try {
					geoserverHandler.prepareWorkspace(geoserverDataDir, workspace);
				} catch (IOException | IllegalArgumentException | URISyntaxException ex) {
					responseMap.put("error", "Could not create workspace: " + ex.getMessage());
					RequestResponseHelper.sendErrorResponse(response, responseMap);
					return;
				}
			} else if ("remove-layer".equals(action) && !"published".equalsIgnoreCase(workspace.trim())) {
				try {
					LOG.info("Remove layer called");

					boolean isShoreline = Boolean.parseBoolean(request.getParameter("isShoreline"));

					if (isShoreline) {
						LOG.info("Shoreline layer being removed. First going to try to remove from database");
						ShorelineShapefileDAO dao = new ShorelineShapefileDAO();
						if (dao.removeShorelines(workspace, layer)) {
							LOG.info("No more shorelines exist workspace {}. Will delete the view", workspace);
							String viewName = workspace + "_shorelines";
							dao.removeShorelineView(viewName);
							LOG.info("Deleted view {}", viewName);
							if (dao.getShorelineCountInShorelineView(workspace) == 0) {
								geoserverHandler.removeLayer(workspace, store, viewName);
							}
						}
					} else {
						geoserverHandler.removeLayer(workspace, store, layer);
					}
				} catch (MalformedURLException | SQLException ex) {
					responseMap.put("error", "Could not remove layer: " + ex.getMessage());
					RequestResponseHelper.sendErrorResponse(response, responseMap);
					return;
				}
			} else if ("logout".equals(action)) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					try {
						session.invalidate();
						Cookie cookie = new Cookie("JSESSIONID", null);
						cookie.setPath(request.getContextPath());
						cookie.setMaxAge(0);
						response.addCookie(cookie);
					} catch (IllegalStateException ex) {
						// Session was already invalidated
					}
				}
			} else if ("get-oid-info".equals(action)) {
				HttpSession session = request.getSession(false);
				if (session == null || session.getAttribute("oid-info") == null) {
					responseMap.put("error", "OpenID credentials not in session.");
					RequestResponseHelper.sendErrorResponse(response, responseMap);
					return;
				} else {
					Map<String, String> oidInfoMap = ((Map<String, String>) session.getAttribute("oid-info"));
					responseMap.put("firstname", oidInfoMap.get("oid-firstname"));
					responseMap.put("lastname", oidInfoMap.get("oid-lastname"));
					responseMap.put("country", oidInfoMap.get("oid-country"));
					responseMap.put("language", oidInfoMap.get("oid-language"));
					responseMap.put("email", oidInfoMap.get("oid-email"));
				}
			}
		}

		RequestResponseHelper.sendSuccessResponse(response, responseMap);

	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
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
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>
}
