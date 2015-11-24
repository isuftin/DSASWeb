package gov.usgs.cida.dsas.rest.service.security;

import java.net.URISyntaxException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 *
 * @author isuftin
 */
@Path("/")
public class SecurityResource {

	private static final long serialVersionUID = 1032470707499055942L;

	@GET
    @Produces(MediaType.TEXT_HTML)
    @Path("login")
    public Response loginPage(@Context HttpServletRequest req) throws URISyntaxException {
        return Response.ok(new Viewable("/WEB-INF/jsp/login/index.jsp", new HashMap<>())).build();
    }
}
