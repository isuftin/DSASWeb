package gov.usgs.cida.dsas.rest.service.security.ui;

import gov.usgs.cida.dsas.rest.service.ServiceURI;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.SECURITY_UI_ENDPOINT)
public class SecurityUIApplication extends ResourceConfig {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SecurityUIApplication.class);

	public SecurityUIApplication() {
		packages(this.getClass().getPackage().getName());
		register(JspMvcFeature.class);
	}
}
