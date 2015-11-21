package gov.usgs.cida.dsas.rest.service.shapefile;

import gov.usgs.cida.dsas.service.ServiceURI;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author isuftin
 */
@ApplicationPath(ServiceURI.SHAPEFILE_SERVICE_ENDPOINT)
public class ShapefileApplication extends ResourceConfig {

	public ShapefileApplication() {
		packages(this.getClass().getPackage().getName());
		register(MultiPartFeature.class);
	}

}
