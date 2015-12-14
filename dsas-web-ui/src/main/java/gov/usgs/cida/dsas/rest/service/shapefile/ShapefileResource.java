package gov.usgs.cida.dsas.rest.service.shapefile;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.featureType.file.FeatureType;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFile;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFileFactory;
import gov.usgs.cida.dsas.featureType.file.TokenFeatureTypeFileExchanger;
import gov.usgs.cida.dsas.rest.service.ServiceURI;
import gov.usgs.cida.dsas.rest.service.security.TokenBasedSecurityFilter;
import gov.usgs.cida.dsas.featureTypeFile.exception.FeatureTypeFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@MultipartConfig
@Path("/")
public class ShapefileResource {
// In Feb 2016, refactor: create a delegate for this class to control bloat. Consider how Lidar is not truly a shape file. 

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapefileResource.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{token}/columns")
	public Response getColumnNames(
			@Context HttpServletRequest req,
			@PathParam("token") String fileToken
	) {
		Response response = null;
		Map<String, String> responseMap = new HashMap<>(1);
		Gson gson = new Gson();

		FeatureTypeFile featureTypeFile = TokenFeatureTypeFileExchanger.getFeatureTypeFile(fileToken); 
		if ( featureTypeFile == null || !featureTypeFile.exists() ) {
			LOGGER.error("Unable to get shape file for token: " + fileToken);
			TokenFeatureTypeFileExchanger.removeToken(fileToken);
			
			responseMap.put("error", "Unable to retrieve shape file with token: " + fileToken);
			response = Response
					.serverError()
					.status(Response.Status.NOT_FOUND)
					.entity(new Gson().toJson(responseMap))
					.build();
		}
		if (response == null) {
			try {				
				String[] names = featureTypeFile.getColumns();
				responseMap.put("headers", gson.toJson(names, String[].class));

				response = Response
						.accepted()
						.entity(gson.toJson(responseMap, HashMap.class))
						.build();
			} catch (IOException ex) {
				LOGGER.error("Error while attempting to get dbf names from featureTypeFile: ", ex);
				responseMap.put("error", ex.getLocalizedMessage());
				response = Response
						.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(gson.toJson(responseMap, HashMap.class))
						.build();
			}
		}
		return response;
	}

	@POST
	@Path("/pdb")
	@RolesAllowed({TokenBasedSecurityFilter.DSAS_AUTHORIZED_ROLE})
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createPdbToken(
			@Context HttpServletRequest req,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDisposition
	) {  //stages the file - uploads the file
		Response response = null;
		Map<String, String> responseMap = new HashMap<>(1);
		Gson gson = new Gson();
		FeatureTypeFile featureTypeFile = null;
		String token = null;

		try { 
			//Note the additional Path attribute of pdb which determines its type.
			featureTypeFile = new FeatureTypeFileFactory().createFeatureTypeFile(fileInputStream, FeatureType.PDB);
			
		} catch (IOException | FeatureTypeFileException ex) {
			LOGGER.error("Error while attempting upload of shapefile. ", ex);
			responseMap.put("error", ex.getLocalizedMessage());
			response = Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(gson.toJson(responseMap, HashMap.class))
					.build();
		}

		if (response == null) {
			try {
				token = TokenFeatureTypeFileExchanger.getToken(featureTypeFile); 
				
				response = Response
						.accepted()
						.header(HttpHeaders.LOCATION, ServiceURI.SHAPEFILE_SERVICE_ENDPOINT + "/" + token)
						.build();
			} catch (FileNotFoundException ex) {
				LOGGER.error("Unable to get token from uploaded zip file: ", ex);
				responseMap.put("error", ex.getLocalizedMessage());
				response = Response
						.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(gson.toJson(responseMap, HashMap.class))
						.build();
			}
		}

		return response;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createToken(
			@Context HttpServletRequest req,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDisposition
	) {  //stages the file - uploads the file
		Response response = null;
		Map<String, String> responseMap = new HashMap<>(1);
		Gson gson = new Gson();
		FeatureTypeFile featureTypeFile = null;
		String token = null;

		try { 
			featureTypeFile = new FeatureTypeFileFactory().createFeatureTypeFile(fileInputStream, FeatureType.SHORELINE);
			
		} catch (IOException | FeatureTypeFileException ex) {
			LOGGER.error("Error while attempting upload of shapefile. ", ex);
			responseMap.put("error", ex.getLocalizedMessage());
			response = Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(gson.toJson(responseMap, HashMap.class))
					.build();
		}

		if (response == null) {
			try {
				token = TokenFeatureTypeFileExchanger.getToken(featureTypeFile); 
				
				response = Response
						.accepted()
						.header(HttpHeaders.LOCATION, ServiceURI.SHAPEFILE_SERVICE_ENDPOINT + "/" + token)
						.build();
			} catch (FileNotFoundException ex) {
				LOGGER.error("Unable to get token from uploaded zip file: ", ex);
				responseMap.put("error", ex.getLocalizedMessage());
				response = Response
						.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(gson.toJson(responseMap, HashMap.class))
						.build();
			}
		}

		return response;
	}
	
	// import pdb shape to the database file
	// Pdb is isolated due to the security needs
	@POST
	@RolesAllowed({TokenBasedSecurityFilter.DSAS_AUTHORIZED_ROLE})
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/pdb/{token}/workspace/{workspace}")
	public Response importPdbShapefile(
			@Context HttpServletRequest req,
			@PathParam("token") String fileToken,
			@PathParam("workspace") String workspace 
	) {
		String columnsString = req.getParameter("columns");  
		Map<String, String> columns = new HashMap<>();
				
		boolean isColumnsStringNotBlank = StringUtils.isNotBlank(columnsString);
		boolean isfileTokenNotBlank = StringUtils.isNotBlank(fileToken);
		boolean isWorkspaceNotBlank = StringUtils.isNotBlank(workspace);
		
		if ((isColumnsStringNotBlank) && (isfileTokenNotBlank) && (isWorkspaceNotBlank)) {
			columns = new Gson().fromJson(columnsString, Map.class);

			ShapefileImportProcess process = new ShapefileImportProcess(fileToken, columns, workspace);
			Thread thread = new Thread(process);
			thread.start();

			return Response
					.accepted()
					.header(HttpHeaders.LOCATION, ServiceURI.PROCESS_SERVICE_ENDPOINT + "/" + process.getProcessId())
					.build();
		} else {
			Map<String, String> map = new HashMap<>();
			if (!isColumnsStringNotBlank)
				map.put("error", "Parameter \"columns\" missing");
			else if (!isfileTokenNotBlank)
				map.put("error", "Parameter \"file token\" missing");
			else if (!isWorkspaceNotBlank)
				map.put("error", "Parameter \"workspace\" missing");
			return Response
					.serverError()
					.status(Response.Status.BAD_REQUEST)
					.entity(new Gson().toJson(map))
					.build();
		}

	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/shoreline/{token}/workspace/{workspace}")
	public Response importShorelineShapefile(
			@Context HttpServletRequest req,
			@PathParam("token") String fileToken,
			@PathParam("workspace") String workspace 
	) {
		String columnsString = req.getParameter("columns");
		Map<String, String> columns = new HashMap<>();
		
		boolean isColumnsStringNotBlank = StringUtils.isNotBlank(columnsString);
		boolean isfileTokenNotBlank = StringUtils.isNotBlank(fileToken);
		boolean isWorkspaceNotBlank = StringUtils.isNotBlank(workspace);
		
		if ( (isColumnsStringNotBlank) && (isfileTokenNotBlank) && (isWorkspaceNotBlank) ) {
			columns = new Gson().fromJson(columnsString, Map.class);

			ShapefileImportProcess process = new ShapefileImportProcess(fileToken, columns, workspace);
			Thread thread = new Thread(process);
			thread.start();

			return Response
					.accepted()
					.header(HttpHeaders.LOCATION, ServiceURI.PROCESS_SERVICE_ENDPOINT + "/" + process.getProcessId())
					.build();
		} else {
			Map<String, String> map = new HashMap<>();
			if (!isColumnsStringNotBlank)
			map.put("error", "Parameter \"columns\" missing");
			else if (!isfileTokenNotBlank)
				map.put("error", "Parameter \"file token\" missing");
			else if (!isWorkspaceNotBlank)
				map.put("error", "Parameter \"workspace\" missing");
			return Response
					.serverError()
					.status(Response.Status.BAD_REQUEST)
					.entity(new Gson().toJson(map))
					.build();
		}

	}
	
}
