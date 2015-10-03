package gov.usgs.cida.geoutils.geoserver.servlet;

import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse.ResponseType;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.owsutils.commons.shapefile.ProjectionUtils;
import gov.usgs.cida.utilities.service.ServiceHelper;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.referencing.FactoryException;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ShapefileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ShapefileUploadServlet.class);
	private static Integer maxFileSize;
	private static String geoserverEndpoint;
	private static URL geoserverEndpointURL;
	private static String geoserverUsername;
	private static String geoserverPassword;
	private static GeoServerRESTManager gsRestManager;
	// Defaults
	private static String defaultWorkspaceName;
	private static String defaultStoreName;
	private static String defaultSRS;
	private static final String defaultFilenameParam = "qqfile"; // Legacy to handle jquery fineuploader
	private static final Integer defaultMaxFileSize = Integer.MAX_VALUE;
	private static final boolean defaultUseBaseCRSFallback = true;
	private static final boolean defaultOverwriteExistingLayer = false;
	private static final ProjectionPolicy defaultProjectionPolicy = ProjectionPolicy.REPROJECT_TO_DECLARED;
	private static ServletConfig servletConfig;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init();
		ShapefileUploadServlet.servletConfig = servletConfig;

		// The maximum upload file size allowd by this server, 0 = Integer.MAX_VALUE
		String mfsJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_MAX_SIZE);
		if (StringUtils.isNotBlank(mfsJndiProp)) {
			maxFileSize = Integer.parseInt(mfsJndiProp);
		} else {
			maxFileSize = defaultMaxFileSize;
		}

		if (maxFileSize == 0) {
			maxFileSize = defaultMaxFileSize;
		}

		LOG.debug("Maximum allowable file size set to: " + maxFileSize + " bytes");

		String gsepJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		if (StringUtils.isNotBlank(gsepJndiProp)) {
			geoserverEndpoint = gsepJndiProp;
			if (geoserverEndpoint.endsWith("/")) {
				geoserverEndpoint = geoserverEndpoint.substring(0, geoserverEndpoint.length() - 1);
			}
		} else {
			throw new ServletException("Geoserver endpoint is not defined.");
		}
		LOG.debug("Geoserver endpoint set to: " + geoserverEndpoint);

		try {
			geoserverEndpointURL = new URL(geoserverEndpoint);
		} catch (MalformedURLException ex) {
			throw new ServletException("Geoserver endpoint (" + geoserverEndpoint + ") could not be parsed into a valid URL.");
		}

		String gsuserJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		if (StringUtils.isNotBlank(gsuserJndiProp)) {
			geoserverUsername = gsuserJndiProp;
		} else {
			throw new ServletException("Geoserver username is not defined.");
		}
		LOG.debug("Geoserver username set to: " + geoserverUsername);

		// This should only be coming from JNDI or JVM properties
		String gspassJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		if (StringUtils.isNotBlank(gspassJndiProp)) {
			geoserverPassword = gspassJndiProp;
		} else {
			throw new ServletException("Geoserver password is not defined.");
		}
		LOG.debug("Geoserver password is set");

		try {
			gsRestManager = new GeoServerRESTManager(geoserverEndpointURL, geoserverUsername, geoserverPassword);
		} catch (IllegalArgumentException ex) {
			throw new ServletException("Geoserver manager count not be built", ex);
		} catch (MalformedURLException ex) {
			// This should not happen since we take care of it above - we can probably move this into the try block above
			throw new ServletException("Geoserver endpoint (" + geoserverEndpoint + ") could not be parsed into a valid URL.");
		}

		String dwJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_DEFAULT_UPLOAD_WORKSPACE);
		if (StringUtils.isNotBlank(dwJndiProp)) {
			defaultWorkspaceName = dwJndiProp;
		} else {
			defaultWorkspaceName = "";
			LOG.warn("Default workspace is not defined. If a workspace is not passed to during the request, the request will fail.");
		}
		LOG.debug("Default workspace set to: " + defaultWorkspaceName);

		String dsnJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_DEFAULT_UPLOAD_STORENAME);
		if (StringUtils.isNotBlank(dsnJndiProp)) {
			defaultStoreName = dsnJndiProp;
		} else {
			defaultStoreName = "";
			LOG.warn("Default store name is not defined. If a store name is not passed to during the request, the name of the layer will be used as the name of the store");
		}
		LOG.debug("Default store name set to: " + defaultStoreName);

		String dsrsJndiProp = PropertyUtil.getProperty(Property.DEFAULT_SRS);
		if (StringUtils.isNotBlank(dsrsJndiProp)) {
			defaultSRS = dsrsJndiProp;
		} else {
			defaultSRS = "";
			LOG.warn("Default SRS is not defined. If a SRS name is not passed to during the request, the request will fail");
		}
		LOG.debug("Default SRS set to: " + defaultSRS);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, FileNotFoundException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, FileNotFoundException {
		String filenameParam;
		boolean useBaseCRSFailover;
		boolean overwriteExistingLayer;
		// "reproject" (default), "force", "none"
		ProjectionPolicy projectionPolicy;

		Map<String, String> responseMap = new HashMap<>();

		ResponseType responseType = ServiceHelper.getResponseType(request);

		int fileSize = Integer.parseInt(request.getHeader("Content-Length"));
		if (fileSize > maxFileSize) {
			responseMap.put("error", "Upload exceeds max file size of " + maxFileSize + " bytes");
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
			return;
		}

		// The key to search for in the upload form post to find the file
		String fnInitParam = servletConfig.getInitParameter("filename.param");
		String fnJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_FILENAME_PARAM);
		String fnReqParam = request.getParameter("filename.param");
		if (StringUtils.isNotBlank(fnReqParam)) {
			filenameParam = fnReqParam;
		} else if (StringUtils.isNotBlank(fnInitParam)) {
			filenameParam = fnInitParam;
		} else if (StringUtils.isNotBlank(fnJndiProp)) {
			filenameParam = fnJndiProp;
		} else {
			filenameParam = defaultFilenameParam;
		}
		LOG.debug("Filename parameter set to: " + filenameParam);

		String oelInitParam = servletConfig.getInitParameter("overwrite.existing.layer");
		String oelJndiProp = PropertyUtil.getProperty(Property.DEFAULT_OVERWRITE_LAYER);
		String oelReqParam = request.getParameter("overwrite.existing.layer");
		if (StringUtils.isNotBlank(oelReqParam)) {
			overwriteExistingLayer = Boolean.parseBoolean(oelReqParam);
		} else if (StringUtils.isNotBlank(oelInitParam)) {
			overwriteExistingLayer = Boolean.parseBoolean(oelInitParam);
		} else if (StringUtils.isNotBlank(oelJndiProp)) {
			overwriteExistingLayer = Boolean.parseBoolean(oelJndiProp);
		} else {
			overwriteExistingLayer = defaultOverwriteExistingLayer;
		}
		LOG.debug("Overwrite existing layer set to: " + overwriteExistingLayer);

		LOG.debug("Cleaning file name.\nWas: " + filenameParam);
		String filename = cleanFileName(request.getParameter(filenameParam));
		LOG.debug("Is: " + filename);
		if (filenameParam.equals(filename)) {
			LOG.debug("(No change)");
		}

		String tempDir = System.getProperty("java.io.tmpdir");
		File shapeZipFile = new File(tempDir + File.separator + filename + new Date().getTime());
		LOG.debug("Temporary file set to " + shapeZipFile.getPath());

		String layerName = request.getParameter("layer");
		if (StringUtils.isBlank(layerName)) {
			layerName = filename.split("\\.")[0];
		}
		layerName = layerName.trim().replaceAll("\\.", "_").replaceAll(" ", "_");
		LOG.debug("Layer name set to " + layerName);

		String workspaceName = request.getParameter("workspace");
		if (StringUtils.isBlank(workspaceName)) {
			workspaceName = defaultWorkspaceName;
		}
		if (StringUtils.isBlank(workspaceName)) {
			responseMap.put("error", "Parameter \"workspace\" is mandatory");
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
			return;
		}
		LOG.debug("Workspace name set to " + workspaceName);

		String storeName = request.getParameter("store");
		if (StringUtils.isBlank(storeName)) {
			storeName = defaultStoreName;
		}
		if (StringUtils.isBlank(storeName)) {
			storeName = layerName;
		}
		LOG.debug("Store name set to " + storeName);

		String srsName = request.getParameter("srs");
		if (StringUtils.isBlank(srsName)) {
			srsName = defaultSRS;
		}
		if (StringUtils.isBlank(srsName)) {
			responseMap.put("error", "Parameter \"srs\" is mandatory");
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
			return;
		}
		LOG.debug("SRS name set to " + srsName);

		String bCRSfoInitParam = servletConfig.getInitParameter("use.crs.failover");
		String bCRSfoJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_USE_CRS_FAILOVER);
		String bCRSfoReqParam = request.getParameter("use.crs.failover");
		if (StringUtils.isNotBlank(bCRSfoReqParam)) {
			useBaseCRSFailover = Boolean.parseBoolean(bCRSfoReqParam);
		} else if (StringUtils.isNotBlank(bCRSfoInitParam)) {
			useBaseCRSFailover = Boolean.parseBoolean(bCRSfoInitParam);
		} else if (StringUtils.isNotBlank(bCRSfoJndiProp)) {
			useBaseCRSFailover = Boolean.parseBoolean(bCRSfoJndiProp);
		} else {
			useBaseCRSFailover = defaultUseBaseCRSFallback;
		}
		LOG.debug("Use base CRS failover set to: " + useBaseCRSFailover);

		String projectionPolicyReqParam = request.getParameter("projection.policy");
		String ppInitParam = servletConfig.getInitParameter("projection.policy");
		String ppJndiProp = PropertyUtil.getProperty(Property.GEOSERVER_LAYER_PROJECTION_POLICY);
		if (StringUtils.isNotBlank(projectionPolicyReqParam)) {
			if (projectionPolicyReqParam.equalsIgnoreCase("reproject")) {
				projectionPolicy = ProjectionPolicy.REPROJECT_TO_DECLARED;
			} else if (projectionPolicyReqParam.equalsIgnoreCase("force")) {
				projectionPolicy = ProjectionPolicy.FORCE_DECLARED;
			} else {
				projectionPolicy = defaultProjectionPolicy;
			}
		} else if (StringUtils.isNotBlank(ppInitParam)) {
			if (ppInitParam.equalsIgnoreCase("reproject")) {
				projectionPolicy = ProjectionPolicy.REPROJECT_TO_DECLARED;
			} else if (ppInitParam.equalsIgnoreCase("force")) {
				projectionPolicy = ProjectionPolicy.FORCE_DECLARED;
			} else {
				projectionPolicy = ProjectionPolicy.NONE;
			}
		} else if (StringUtils.isNotBlank(ppJndiProp)) {
			if (ppJndiProp.equalsIgnoreCase("reproject")) {
				projectionPolicy = ProjectionPolicy.REPROJECT_TO_DECLARED;
			} else if (ppJndiProp.equalsIgnoreCase("force")) {
				projectionPolicy = ProjectionPolicy.FORCE_DECLARED;
			} else {
				projectionPolicy = ProjectionPolicy.NONE;
			}
		} else {
			projectionPolicy = defaultProjectionPolicy;
		}
		LOG.debug("Projection policy set to: " + projectionPolicy.name());
		LOG.debug("Projection policy re-set to: " + projectionPolicy);

		try {
			RequestResponse.saveFileFromRequest(request, shapeZipFile, filenameParam);
			LOG.debug("File saved to " + shapeZipFile.getPath());

			//Each of these steps can be overridden
			shapeZipFile = cleanUploadedFile(shapeZipFile);
			validateUploadedFile(shapeZipFile); //throws exception if invalid
			shapeZipFile = transformUploadedFile(shapeZipFile);
		} catch (Exception ex) {
			LOG.warn(ex.getMessage());
			responseMap.put("error", "Unable to upload file");
			responseMap.put("exception", ex.getMessage());
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
			return;
		}

		try {
			srsName = ProjectionUtils.getProjectionFromShapefileZip(shapeZipFile, false);
		} catch (IOException | FactoryException | ProjectionException ex) {
			responseMap.put("warning", "WARNING: Could not find EPSG code for prj definition. The geographic coordinate system '" + srsName + "' will be used ");
		}

		String importResponse;
		try {
			importResponse = importShapefile(shapeZipFile, overwriteExistingLayer, workspaceName, storeName, layerName, srsName, projectionPolicy);

			if (importResponse.toLowerCase(Locale.getDefault()).contains("exception")) {
				String error = parseWPSErrorText(importResponse);
				LOG.debug("Shapefile could not be imported successfully");
				responseMap.put("error", error);
				RequestResponse.sendErrorResponse(response, responseMap, responseType);
			} else {
				LOG.debug("Shapefile has been imported successfully");
				responseMap.put("name", importResponse);
				responseMap.put("workspace", workspaceName);
				responseMap.put("store", storeName);
				RequestResponse.sendSuccessResponse(response, responseMap, responseType);
			}

		} catch (IllegalArgumentException | IOException | ParserConfigurationException | SAXException ex) {
			LOG.warn(ex.getMessage());
			responseMap.put("error", "Unable to upload file");
			responseMap.put("exception", ex.getMessage());
			RequestResponse.sendErrorResponse(response, responseMap, responseType);
		} finally {
			FileUtils.deleteQuietly(shapeZipFile);
		}

	}

	/**
	 * Hook to allow subclasses to clean the upload prior to checking
	 * validation.
	 *
	 * By default this implementation flattens the directory structure via
	 * FileHelper.flattenZipFile().
	 *
	 * If overriding, be sure to delete the old file if you create a new one.
	 *
	 * @param shapefileZip
	 * @return A reference to the updated shapefile zip.
	 * @throws java.io.IOException
	 */
	protected File cleanUploadedFile(File shapefileZip) throws IOException {
		FileHelper.flattenZipFile(shapefileZip.getPath());
		LOG.debug("Zip file directory structure flattened");

		//Flattening is done in place, so just return the same reference
		return shapefileZip;
	}

	/**
	 * Checks that the upload file is valid, after it is cleaned
	 * (cleanUploadedFile).
	 *
	 * If the file is valid, null is returned. If invalid, an Exception is
	 * returned.
	 *
	 * @param shapefileZip
	 * @throws java.lang.Exception
	 */
	protected void validateUploadedFile(File shapefileZip) throws Exception {
		FileHelper.validateShapefileZip(shapefileZip);
		LOG.debug("Zip file seems to be a valid shapefile");
	}

	/**
	 * Hook to allow subclasses to process the upload prior to importing as a
	 * layer.
	 *
	 * If overriding, be sure to delete the old file if you create a new one.
	 *
	 * The default implementation just returns the passed in File.
	 *
	 * @param shapefileZip
	 * @return A reference to the updated shapefile zip.
	 * @throws java.lang.Exception
	 */
	protected File transformUploadedFile(File shapefileZip) throws Exception {
		return shapefileZip;
	}

	/**
	 * Hook to allow subclasses to create their own import processing to get the
	 * shapefile up to Geoserver
	 *
	 * @param shapeZipFile
	 * @param overwriteExistingLayer
	 * @param workspaceName
	 * @param storeName
	 * @param layerName
	 * @param srsName
	 * @param projectionPolicy
	 * @return
	 * @throws IOException
	 */
	protected String importShapefile(File shapeZipFile, boolean overwriteExistingLayer, String workspaceName, String storeName, String layerName, String srsName, ProjectionPolicy projectionPolicy) throws IOException {
		GeoServerRESTPublisher gsPublisher = gsRestManager.getPublisher();
		if (overwriteExistingLayer) {
			// TODO- Using this library, I am unable to rename a layer. If publishing the layer
			// fails, we will have lost this layer due to removal here. 
			if (gsPublisher.unpublishFeatureType(workspaceName, storeName, layerName)) {
				gsPublisher.unpublishCoverage(workspaceName, storeName, layerName);
				gsPublisher.reloadStore(workspaceName, storeName, GeoServerRESTPublisher.StoreType.DATASTORES);
			}
		}

		return importUsingWPS(workspaceName, storeName, layerName, shapeZipFile.toURI(), srsName, projectionPolicy, null);
	}

	private String importUsingWPS(String workspaceName, String storeName, String layerName, URI shapefile, String srsName, ProjectionPolicy projectionPolicy, String styleName) throws IOException {
		FileOutputStream wpsRequestOutputStream = null;
		FileInputStream uploadedInputStream = null;

		File wpsRequestFile = File.createTempFile("wps.upload.", ".xml");
		wpsRequestFile.deleteOnExit();
		try {

			wpsRequestOutputStream = new FileOutputStream(wpsRequestFile);
			uploadedInputStream = new FileInputStream(new File(shapefile));

			wpsRequestOutputStream.write(new String(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<wps:Execute service=\"WPS\" version=\"1.0.0\" "
					+ "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
					+ "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
					+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 "
					+ "http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\">"
					+ "<ows:Identifier>gs:Import</ows:Identifier>"
					+ "<wps:DataInputs>"
					+ "<wps:Input>"
					+ "<ows:Identifier>features</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:ComplexData mimeType=\"application/zip\"><![CDATA[").getBytes());
			IOUtils.copy(uploadedInputStream, new Base64OutputStream(wpsRequestOutputStream, true, 0, null));
			wpsRequestOutputStream.write(new String(
					"]]></wps:ComplexData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>workspace</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + workspaceName + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>store</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + storeName + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>name</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + layerName + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>srs</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + srsName + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>srsHandling</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + projectionPolicy + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>").getBytes());

			// TODO- Not yet implemented
			if (StringUtils.isNotBlank(styleName)) {
				wpsRequestOutputStream.write(new String(
						"<wps:Input>"
						+ "<ows:Identifier>styleName</ows:Identifier>"
						+ "<wps:Data>"
						+ "<wps:LiteralData>" + styleName + "</wps:LiteralData>"
						+ "</wps:Data>"
						+ "</wps:Input>").getBytes());

			}

			wpsRequestOutputStream.write(new String(
					"</wps:DataInputs>"
					+ "<wps:ResponseForm>"
					+ "<wps:RawDataOutput>"
					+ "<ows:Identifier>layerName</ows:Identifier>"
					+ "</wps:RawDataOutput>"
					+ "</wps:ResponseForm>"
					+ "</wps:Execute>").getBytes());
		} finally {
			IOUtils.closeQuietly(wpsRequestOutputStream);
			IOUtils.closeQuietly(uploadedInputStream);
		}

		String url = geoserverEndpointURL.toString();

		return postToWPS(url + (url.endsWith("/") ? "" : "/") + "wps/WebProcessingService?Service=WPS&Request=execute&identifier=gs:Import", wpsRequestFile);
	}

	private String postToWPS(String url, File wpsRequestFile) throws IOException {
		HttpPost post;
		HttpClient httpClient = new DefaultHttpClient();

		post = new HttpPost(url);

		FileInputStream wpsRequestInputStream = null;
		try {
			wpsRequestInputStream = new FileInputStream(wpsRequestFile);

			AbstractHttpEntity entity = new InputStreamEntity(wpsRequestInputStream, wpsRequestFile.length());

			post.setEntity(entity);

			HttpResponse response = httpClient.execute(post);

			return EntityUtils.toString(response.getEntity());

		} finally {
			IOUtils.closeQuietly(wpsRequestInputStream);
			FileUtils.deleteQuietly(wpsRequestFile);
		}
	}

	private String parseWPSErrorText(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setValidating(false);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset())));
		doc.getDocumentElement().normalize();

		JXPathContext ctx = JXPathContext.newContext(doc);
		Node errorNode = (Node) ctx.selectSingleNode("//ows:ExceptionReport/ows:Exception/ows:ExceptionText");
		return errorNode.getChildNodes().item(0).getTextContent();
	}

	private String cleanFileName(String input) {
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
