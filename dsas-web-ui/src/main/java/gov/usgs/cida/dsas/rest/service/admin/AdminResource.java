package gov.usgs.cida.dsas.rest.service.admin;

import gov.usgs.cida.dsas.rest.service.security.TokenBasedSecurityFilter;
import java.util.HashMap;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 *
 * @author isuftin
 */
@Path("/")
@PermitAll
public class AdminResource {
	
	@RolesAllowed({TokenBasedSecurityFilter.DSAS_AUTHORIZED_ROLE})
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response viewAdminPage() {
		return Response.ok(new Viewable("/WEB-INF/jsp/admin/index.jsp", new HashMap<>())).build();
	}
}
