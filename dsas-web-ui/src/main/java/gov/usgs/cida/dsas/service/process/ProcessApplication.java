package gov.usgs.cida.dsas.service.process;

import gov.usgs.cida.dsas.service.ServiceURI;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.PROCESS_SERVICE_ENDPOINT)
public class ProcessApplication extends ResourceConfig {

	public ProcessApplication() {
		packages(this.getClass().getPackage().getName());
	}
}
