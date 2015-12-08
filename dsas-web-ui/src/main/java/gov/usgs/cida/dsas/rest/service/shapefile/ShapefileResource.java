package gov.usgs.cida.dsas.rest.service.shapefile;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.featureType.file.FeatureType;
//import gov.usgs.cida.dsas.model.IShapeFile;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFile;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFileFactory;
import gov.usgs.cida.dsas.featureType.file.TokenFeatureTypeFileExchanger;
import gov.usgs.cida.dsas.pdb.file.PdbFile;
import gov.usgs.cida.dsas.rest.service.ServiceURI;
import gov.usgs.cida.dsas.rest.service.security.TokenBasedSecurityFilter;
import gov.usgs.cida.dsas.service.util.ImportUtil;
import gov.usgs.cida.dsas.service.util.ShapeFileUtil;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
//import gov.usgs.cida.dsas.service.util.TokenFileExchanger;
import gov.usgs.cida.dsas.featureTypeFile.exception.FeatureTypeFileException;
import gov.usgs.cida.dsas.featureTypeFile.exception.LidarFileFormatException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.shoreline.file.ShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineLidarFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineShapefile;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Array;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
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
//	private static final Integer DEFAULT_MAX_FILE_SIZE = Integer.MAX_VALUE;
//	private static final File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
//	private static final File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));

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
		FeatureTypeFile featureTypeFile = null;

		featureTypeFile = TokenFeatureTypeFileExchanger.getFeatureTypeFile(fileToken); // this should return a FeatureTypeFile
		if ( featureTypeFile == null || !featureTypeFile.exists() ) {
			LOGGER.error("Unable to get shape file for token: " + fileToken);
			
			responseMap.put("error", "Unable to retrieve shape file with token: " + fileToken);
			return Response
					.serverError()
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(new Gson().toJson(responseMap))
					.build();
		}
		if (response == null) {
			try {				
				//List<String> nameList = ShapeFileUtil.getDbfColumnNames(featureTypeZip); < this will fail for Lidars
				List<String> nameList = featureTypeFile.getColumns();
				String[] names = nameList.toArray(new String[nameList.size()]);

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
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createToken(
			@Context HttpServletRequest req,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDisposition
	) {  //stages the file - upload the file
		Response response = null;
		Map<String, String> responseMap = new HashMap<>(1);
		Gson gson = new Gson();
		FeatureTypeFile featureTypeFile = null;
		String token = null;

		try { 
			//Since an inputstream is sent, the clean of the name is no longer required
			featureTypeFile = new FeatureTypeFileFactory().createFeatureTypeFile(fileInputStream);
			
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
	// todo move the import to the pdb and shoreline file
	@POST
	@RolesAllowed({TokenBasedSecurityFilter.DSAS_AUTHORIZED_ROLE})
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/pdb/{token}/workspace/{workspace}")
	public Response importPdbShapefile(
			@Context HttpServletRequest req,
			@PathParam("token") String fileToken,
			@PathParam("workspace") String workspace 
	) {
		String columnsString = req.getParameter("columns");  // #TODO# make this pdb specific
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

	// import Shoreline shape to the database file
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
