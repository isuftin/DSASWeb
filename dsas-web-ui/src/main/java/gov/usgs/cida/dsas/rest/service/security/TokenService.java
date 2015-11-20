package gov.usgs.cida.dsas.rest.service.security;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author isuftin
 */
@Path("/auth")
public class TokenService {

	private static final long serialVersionUID = 1032470707499055942L;
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response createToken(@Context HttpServletRequest req) {
		Response response;
		Map<String, String> responseMap = new HashMap<>();
		Gson gson = new Gson();
		response = Response.ok(gson.toJson(responseMap, HashMap.class)).build();
		return response;
	}
}
