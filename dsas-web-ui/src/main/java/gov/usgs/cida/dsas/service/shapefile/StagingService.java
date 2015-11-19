package gov.usgs.cida.dsas.service.shapefile;

import com.google.gson.Gson;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
@MultipartConfig
@Path("/staging")
public class StagingService {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StagingService.class);
	private static final Integer DEFAULT_MAX_FILE_SIZE = Integer.MAX_VALUE;
	private static final File baseDirectory = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
	private static final File uploadDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createToken(
			@Context HttpServletRequest req,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDisposition
	) {
		Response response;
		Map<String, String> responseMap = new HashMap<>();
		Gson gson = new Gson();
		File shapeZip = null;

		try {
			shapeZip = Files.createTempFile(uploadDirectory.toPath(), null, ".zip").toFile();
			IOUtils.copyLarge(fileInputStream, new FileOutputStream(shapeZip));
			FileHelper.flattenZipFile(shapeZip);
		} catch (IOException ex) {
			LOGGER.warn("Could not create token for uploaded shapefile. ", ex);
		}

		if (shapeZip != null) {
			// TODO - Perform some validation on the shapefile
			String token = UUID.randomUUID().toString();
			// TODO - Associate token with file in a Singleton
			responseMap.put("success", "true");
			responseMap.put("token", token);
			response = Response.ok(gson.toJson(responseMap, HashMap.class)).build();
		} else {
			responseMap.put("success", "false");
			responseMap.put("error", "what happened");
			response = Response.serverError().entity(gson.toJson(responseMap, HashMap.class)).build();
		}

		return response;
	}

	protected final Integer getMaxFileSize() {
		Integer maxFSize = DEFAULT_MAX_FILE_SIZE;
		String mfsJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_MAX_SIZE);
		if (StringUtils.isNotBlank(mfsJndiProp)) {
			maxFSize = Integer.parseInt(mfsJndiProp);
		}
		return maxFSize;
	}

	protected String cleanFileName(String input) {
		String updated = input;

		// Test the first character and if numeric, prepend with underscore
		if (input.substring(0, 1).matches("[0-9]")) {
			updated = "_" + input;
		}

		// Test the rest of the characters and replace anything that's not a 
		// letter, digit or period with an underscore
		char[] inputArr = updated.toCharArray();
		for (int cInd = 0; cInd < inputArr.length; cInd++) {
			if (!Character.isLetterOrDigit(inputArr[cInd]) && !(inputArr[cInd] == '.')) {
				inputArr[cInd] = '_';
			}
		}
		return String.valueOf(inputArr);
	}
}
