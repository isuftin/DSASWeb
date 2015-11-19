package gov.usgs.cida.dsas.service;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

/**
 *
 * @author isuftin
 */
@ApplicationPath("/service")
public class ServiceApplication extends ResourceConfig {

	public ServiceApplication() {
		packages(true, this.getClass().getPackage().getName()); 
		register(JspMvcFeature.class);
	}
}
