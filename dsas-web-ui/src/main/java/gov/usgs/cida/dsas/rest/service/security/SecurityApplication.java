package gov.usgs.cida.dsas.rest.service.security;

import gov.usgs.cida.auth.client.AuthClientSingleton;
import gov.usgs.cida.auth.client.CachingAuthClient;
import gov.usgs.cida.auth.client.NullAuthClient;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import javax.ws.rs.ApplicationPath;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@ApplicationPath("/service/security")
public class SecurityApplication extends ResourceConfig {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SecurityApplication.class);

	public SecurityApplication() {
		packages(true, this.getClass().getPackage().getName());
		register(JspMvcFeature.class);

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
