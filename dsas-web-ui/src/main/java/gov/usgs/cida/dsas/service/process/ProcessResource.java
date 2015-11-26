package gov.usgs.cida.dsas.service.process;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.DSASProcessSingleton;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.rest.service.ServiceURI;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.http.HttpHeaders;

/**
 *
 * @author isuftin
 */
@Path("/")
public class ProcessResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{token}")
	public Response getProcessStatus(@PathParam("token") String token) {
		Response response;
		DSASProcess process = DSASProcessSingleton.getProcess(token);
		if (process == null) {
			response = Response.status(Response.Status.NOT_FOUND).build();
		} else {
			response = Response
					.ok(new Gson().toJson(process.toMap()))
					.header(HttpHeaders.LOCATION, ServiceURI.PROCESS_SERVICE_ENDPOINT + "/" + process.getProcessId())
					.build();
		}
		return response;
	}
	
	@DELETE
	@Path("{token}")
	public Response removeProcess(@PathParam("token") String token) {
		DSASProcessSingleton.removeDSASProcess(token);
		return  Response.status(Response.Status.NO_CONTENT).build();
	}
}
