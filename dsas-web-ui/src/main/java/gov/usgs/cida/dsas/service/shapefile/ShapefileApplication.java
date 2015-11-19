package gov.usgs.cida.dsas.service.shapefile;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

/**
 *
 * @author isuftin
 */
@ApplicationPath("/service/shapefile")
public class ShapefileApplication extends ResourceConfig{
	public ShapefileApplication() {
		packages(this.getClass().getPackage().getName());
		register(JspMvcFeature.class);
		register(MultiPartFeature.class);
	}
	
}
