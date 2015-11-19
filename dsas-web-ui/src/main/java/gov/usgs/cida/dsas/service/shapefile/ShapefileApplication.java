package gov.usgs.cida.dsas.service.shapefile;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author isuftin
 */
@ApplicationPath("/service/shapefile")
public class ShapefileApplication extends ResourceConfig{
	public ShapefileApplication() {
		packages(true, this.getClass().getPackage().getName());
	}
	
}
