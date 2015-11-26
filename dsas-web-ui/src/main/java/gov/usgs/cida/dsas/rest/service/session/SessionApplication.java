package gov.usgs.cida.dsas.rest.service.session;

import gov.usgs.cida.dsas.rest.service.ServiceURI;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.SESSION_SERVICE_ENDPOINT)
public class SessionApplication extends ResourceConfig {

	public SessionApplication() {
		packages(this.getClass().getPackage().getName());
		
		// https://jersey.java.net/project-info/2.22.1/jersey/jersey-server/xref/org/glassfish/jersey/server/ServerProperties.html#747
		// https://github.com/jersey/jersey/blob/master/docs/src/main/docbook/migration.xml#L81-L99
		// This allows us to not include the full URI with the Location header.
		// Doing this lets us make use of proxies and balancers without tying us 
		// to a given server
		property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, "true");
	}
}
