package gov.usgs.cida.dsas.rest.service.security;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author isuftin
 */
@Path("/")
public class SecurityResource {

	private static final long serialVersionUID = 1032470707499055942L;

	@HEAD
	@Path("check/{token}")
	public Response checkToken(@PathParam("token") String token) throws java.net.URISyntaxException {
		if (StringUtils.isBlank(token)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			return Response.ok().build();
		}
	}
	
}
