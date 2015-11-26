package gov.usgs.cida.dsas.rest.service.admin;

import gov.usgs.cida.auth.client.AuthClientSingleton;
import gov.usgs.cida.auth.client.CachingAuthClient;
import gov.usgs.cida.auth.client.NullAuthClient;
import gov.usgs.cida.dsas.rest.service.ServiceURI;
import gov.usgs.cida.dsas.rest.service.security.DynamicRolesLoginRedirectFeature;
import gov.usgs.cida.dsas.rest.service.security.ForbiddenExceptionMapper;
import gov.usgs.cida.dsas.rest.service.security.TokenBasedSecurityFilter;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import javax.ws.rs.ApplicationPath;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.ADMIN_UI_ENDPOINT)
public class AdminApplication extends ResourceConfig {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdminApplication.class);

	public AdminApplication() {

		packages(true, this.getClass().getPackage().getName());

		register(JspMvcFeature.class);
		register(ForbiddenExceptionMapper.class);
		register(TokenBasedSecurityFilter.class);
		register(DynamicRolesLoginRedirectFeature.class);

		if (!AuthClientSingleton.isInitialized()) {
			String nullRoles = PropertyUtil.getProperty(NullAuthClient.AUTH_ROLES_JNDI_NAME);
			if (StringUtils.isNotBlank(nullRoles)) {
				AuthClientSingleton.initAuthClient(NullAuthClient.class);
			} else {
				AuthClientSingleton.initAuthClient(CachingAuthClient.class);
			}
			if (!AuthClientSingleton.isInitialized()) {
				LOGGER.warn("Could not initialize authentication client. This is only an issue if authentication will be required during normal operation.");
			} else {
				LOGGER.info("Authentication client initialized");
			}
		}
	}
}
