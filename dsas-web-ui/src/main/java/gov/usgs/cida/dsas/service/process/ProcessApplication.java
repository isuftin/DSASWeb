package gov.usgs.cida.dsas.service.process;

import gov.usgs.cida.dsas.rest.service.ServiceURI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.PROCESS_SERVICE_ENDPOINT)
public class ProcessApplication extends ResourceConfig {

	public ProcessApplication() {
		packages(this.getClass().getPackage().getName());
		Map<String, Object> props = new HashMap<>();
		// https://jersey.java.net/project-info/2.22.1/jersey/jersey-server/xref/org/glassfish/jersey/server/ServerProperties.html#747
		// https://github.com/jersey/jersey/blob/master/docs/src/main/docbook/migration.xml#L81-L99
		// This allows us to not include the full URI with the Location header.
		// Doing this lets us make use of proxies and balancers without tying us 
		// to a given server
		property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, "true");
	}
}
