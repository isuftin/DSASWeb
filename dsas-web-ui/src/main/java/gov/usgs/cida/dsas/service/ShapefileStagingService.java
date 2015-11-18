package gov.usgs.cida.dsas.service;

import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse.ResponseType;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.utilities.service.ServiceHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Receives a shapefile from the client, reads the featuretype from it and sends
 * back a file token which will later be used to read in the shapefile
 *
 * @author isuftin
 */
@MultipartConfig
@WebServlet(name = "ShapefileStagingService", urlPatterns = {"/service/shapefile/stage"})
public class ShapefileStagingService extends FileIngestionServlet {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapefileStagingService.class);
	private static final long serialVersionUID = 6616007726957388275L;
	private static final File baseDirectory = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
	private static final File uploadDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init();
		
		this.maxFileSize = this.getMaxFileSize();
		
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(405);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean success = false;
		Map<String, String> responseMap = new HashMap<>();
		ResponseType responseType = ServiceHelper.getResponseType(request);
		
		File shapeZip = saveZipFileFromRequest(request);
		
	}
	
	private File saveZipFileFromRequest(HttpServletRequest request) throws IOException, ServletException {
		Part zipFilePart = request.getPart("file");

		File zipFile;
		try (InputStream inputStream = zipFilePart.getInputStream();) {
			zipFile = Files.createTempFile(uploadDirectory.toPath(), null, ".zip").toFile();
			IOUtils.copyLarge(inputStream, new FileOutputStream(zipFile));
		}
		
		return FileHelper.flattenZipFile(zipFile);
	}

}
