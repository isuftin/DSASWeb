package gov.usgs.cida.dsas.rest.service.session;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.rest.service.ServiceURI;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@Path("/")
public class SessionResource {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SessionResource.class);
	private static GeoserverDAO geoserverHandler = null;
	private static String geoserverEndpoint = null;
	private static String geoserverUsername = null;
	private static String geoserverPassword = null;
	private static String geoserverDataDir = null;

	public SessionResource() {
		geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		geoserverDataDir = PropertyUtil.getProperty(Property.GEOSERVER_DATA_DIRECTORY);
		geoserverHandler = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);
	}
	
	@PUT
	@Path("{token}")
	public Response updateTokenLastAccessed(@PathParam("token") String token) {
		Response response;
		PostgresDAO pgDao = new PostgresDAO();
		try {
			if (!pgDao.workspaceExists(token)) {
				response = Response.status(Status.NOT_FOUND).build();
			} else if (new PostgresDAO().updateWorkspaceLastAccessTime(token)) {
				response = Response.noContent().contentLocation(new URI(ServiceURI.SESSION_SERVICE_ENDPOINT + "/" + token)).build();
			} else {
				response = Response.status(Status.PRECONDITION_FAILED).build();
			}
		} catch (SQLException | URISyntaxException ex) {
			Map<String, String> map = new HashMap<>();
			map.put("error", ex.getLocalizedMessage());
			response = Response.serverError().entity(new Gson().toJson(map)).build();
		}
		return response;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorkspaceWithoutTokenParam() {
		return createWorkspace(UUID.randomUUID().toString().replaceAll("-", ""));
	}

	@POST
	@Path("{token}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorkspace(@PathParam("token") String token) {
		Response response;
		String _token = token;

		if (StringUtils.isBlank(_token)) {
			_token = UUID.randomUUID().toString().replaceAll("-", "");
		}

		try {
			new PostgresDAO().createWorkspace(_token);
			geoserverHandler.prepareWorkspace(geoserverDataDir, _token);
			response = Response.created(new URI(ServiceURI.SESSION_SERVICE_ENDPOINT + "/" + _token)).build();
		} catch (IllegalArgumentException | IOException | URISyntaxException | SQLException ex) {
			Map<String, String> map = new HashMap<>();
			map.put("error", ex.getMessage());
			response = Response.serverError().entity(new Gson().toJson(map)).build();
		}
		return response;
	}

	@DELETE
	@Path("{token}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteWorkspace(@PathParam("token") String token) {
		Response response;
		Map<String, String> map = null;
		// Make sure the workspace being deleted is not required by the application
		if (token.toLowerCase().trim().equals("published")) {
			response = Response.status(Status.FORBIDDEN).build();
		} else if (!geoserverHandler.workspaceExists(token)) {
			// If the workspace doesn't exist, send no content. This will be the same 
			// response if the workspace got deleted.
			response = Response.noContent().build();
		} else if (geoserverHandler.deleteWorkspace(token)) {
			// Try deleting the workspace.
			try {
				new PostgresDAO().removeWorkspace(token);
				response = Response.noContent().build();
			} catch (SQLException ex) {
				LOGGER.warn(String.format("Could not remove workspace %s", token), ex);
				map = new HashMap<>(1);
				map.put("error", ex.getLocalizedMessage());
				response = Response.serverError().entity(new Gson().toJson(map)).build();
			}

		} else {
			map = new HashMap<>(1);
			map.put("error", String.format("Workspace %s could not be deleted", token));
			response = Response.serverError().entity(new Gson().toJson(map)).build();
		}

		return response;
	}
}
