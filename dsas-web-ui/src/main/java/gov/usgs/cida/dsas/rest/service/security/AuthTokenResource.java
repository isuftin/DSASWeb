package gov.usgs.cida.dsas.rest.service.security;

import gov.usgs.cida.auth.client.AuthClientSingleton;
import gov.usgs.cida.auth.client.IAuthClient;
import gov.usgs.cida.auth.ws.rs.service.AbstractAuthTokenService;
import java.util.List;
import javax.ws.rs.Path;

/**
 *
 * @author isuftin
 */
@Path("/auth")
public class AuthTokenResource  extends AbstractAuthTokenService {
	public static final String AUTH_TOKEN_LABEL = "DSASAuthenticationToken";

	@Override
	public IAuthClient getAuthClient() {
		return AuthClientSingleton.getAuthClient();
	}

	@Override
	public List<String> getAdditionalRoles() {
		return TokenBasedSecurityFilter.ACCEPTED_ROLES;
	}
}
