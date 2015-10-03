package gov.usgs.cida.coastalhazards.service;

import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO.DBaseColumn.ColumnType;
import gov.usgs.cida.utilities.communication.RequestResponseHelper;
import gov.usgs.cida.utilities.file.FileHelper;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author isuftin
 * @author rhayes
 */
public class ShapefileImportService extends HttpServlet {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ShapefileImportService.class);
	private static final long serialVersionUID = -4228098450640162920L;
	private static final String PTS_SUFFIX = gov.usgs.cida.coastalhazards.uncy.Xploder.PTS_SUFFIX;
	private String uploadDirectory = null;
	private String geoserverEndpoint = null;
	private String geoserverUsername = null;
	private String geoserverPassword = null;
	private String geoserverDataDir = null;
	// GeoserverDAO and GeoServerRESTManager are non-serializable. This means
	// that if this servlet is serialized (if this application is clustered), this
	// mioght become a problem
	private transient GeoserverDAO geoserverHandler = null;

	@Override
	public void init() throws ServletException {
		super.init();
		LOG.info("Initializing ShapefileImportService servlet");
		uploadDirectory = PropertyUtil.getProperty(Property.DIRECTORIES_BASE) + File.separatorChar + PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD);
		geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		geoserverDataDir = PropertyUtil.getProperty(Property.GEOSERVER_DATA_DIRECTORY);
		geoserverHandler = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, IllegalArgumentException {
		String fileToken = request.getParameter("file-token");
		String featureName = request.getParameter("feature-name");
		String workspace = request.getParameter("workspace");
		String store = request.getParameter("store");
		String[] extraColumns = request.getParameterValues("extra-column");

		File shapeFile;
		String name;
		Map<String, String> responseMap = new HashMap<>();

		List<GeoserverDAO.DBaseColumn> dbcList = new ArrayList<>();

		if (extraColumns != null) {
			for (String extraColumn : extraColumns) {
				String[] columnParams = extraColumn.trim().split("\\|");

				if (columnParams.length < 3 || columnParams.length > 4) {
					responseMap.put("error", "Extra columns parameter has too few or too many parameters");
					RequestResponseHelper.sendErrorResponse(response, responseMap);
					return;
				}

				char columnTypeChar = Character.toUpperCase(columnParams[0].charAt(0));
				ColumnType ct;
				String columnName = columnParams[1];
				int fieldLength;
				int decimalCount = columnParams.length == 3 ? 0 : Integer.getInteger(columnParams[3]);

				switch (columnTypeChar) {
					case 'C': {
						ct = ColumnType.STRING;
						fieldLength = Integer.getInteger(columnParams[2], 254);
						break;
					}
					case 'N': {
						ct = ColumnType.NUMERIC;
						fieldLength = Integer.getInteger(columnParams[2], 18);
						break;
					}
					case 'F': {
						ct = ColumnType.FLOATING;
						fieldLength = Integer.getInteger(columnParams[2], 20);
						break;
					}
					case 'L': {
						ct = ColumnType.LOGICAL;
						fieldLength = Integer.getInteger(columnParams[2], 1);
						break;
					}
					case 'D': {
						ct = ColumnType.DATE;
						fieldLength = Integer.getInteger(columnParams[2], 8);
						break;
					}
					case '@': {
						ct = ColumnType.TIMESTAMP;
						fieldLength = Integer.getInteger(columnParams[2], 8);
						break;
					}
					default: {
						responseMap.put("error", "Column Type must be 'C' (Character), 'N' (Numeric), 'F' (Floating), 'L' (Logical (Boolean)), 'D' (Date) or '@' (Timestamp)");
						RequestResponseHelper.sendErrorResponse(response, responseMap);
						return;
					}
				}
				GeoserverDAO.DBaseColumn dbc = new GeoserverDAO.DBaseColumn(ct, columnName, fieldLength, decimalCount);
				dbcList.add(dbc);
			}
		}

		File fileDirectoryHandle;
		if (StringUtils.isBlank(fileToken)) {
			// If fileToken is blank, create a blank shapefile to import
			fileToken = UUID.randomUUID().toString();
			String fileName = UUID.randomUUID().toString();
			File subdir = new File(uploadDirectory + File.separator + fileToken);
			FileUtils.forceMkdir(subdir);
			File emptyShapeFile = geoserverHandler.createEmptyShapefile(subdir.getPath(), fileName, dbcList);
			FileHelper.zipFile(emptyShapeFile.getParentFile(), null, null);
			fileDirectoryHandle = new File(new File(uploadDirectory), fileToken);
		} else {
			fileDirectoryHandle = new File(new File(uploadDirectory), fileToken);
		}

		if (StringUtils.isBlank(workspace)) {
			responseMap.put("error", "Parameter 'workspace' cannot be blank");
			RequestResponseHelper.sendErrorResponse(response, responseMap);
			return;
		}

		if (!fileDirectoryHandle.exists() || fileDirectoryHandle.listFiles().length == 0) {
			FileUtils.deleteQuietly(fileDirectoryHandle);
			responseMap.put("error", "The file denoted by token " + fileToken + " does not exist.");
			RequestResponseHelper.sendErrorResponse(response, responseMap);
			return;
		}
		try {
			geoserverHandler.prepareWorkspace(geoserverDataDir, workspace);
		} catch (MalformedURLException | URISyntaxException ex) {
			responseMap.put("error", "Could not create workspace: " + ex.getMessage());
			RequestResponseHelper.sendErrorResponse(response, responseMap);
			return;
		}

		FileFilter zipFileFilter = new WildcardFileFilter("*.zip");

		// Upload directory might contain both input shapefile and shapefile_pts
		File[] zipFiles = fileDirectoryHandle.listFiles(zipFileFilter);
		File pts_File = null;
		shapeFile = null;
		for (File file : zipFiles) {
			if (file.getName().endsWith(PTS_SUFFIX + ".zip")) {
				pts_File = file;
			} else {
				shapeFile = file;
			}
		}

		if (shapeFile == null) {
			responseMap.put("error", "No input located");
			RequestResponseHelper.sendErrorResponse(response, responseMap);
			return;
		}

		responseMap.put("file-token", fileToken);
		responseMap.put("endpoint", geoserverEndpoint);

		name = StringUtils.isBlank(featureName) ? FilenameUtils.removeExtension(shapeFile.getName()) : featureName;

		if (pts_File != null) {
			String pts_name = name + PTS_SUFFIX;
			HttpResponse pts_importResponse = geoserverHandler.importFeaturesFromFile(pts_File, workspace, store, pts_name);
			String pts_responseText = IOUtils.toString(pts_importResponse.getEntity().getContent());

			if (!pts_responseText.toLowerCase(Locale.getDefault()).contains("ows:exception")) {
				responseMap.put("pts_feature", pts_responseText);
			} else {
				InputSource source = new InputSource(new StringReader(pts_responseText));
				XPath xPath = XPathFactory.newInstance().newXPath();
				NodeList list;
				String error = "";
				try {
					list = (NodeList) xPath.evaluate("//*[local-name()='ExceptionText']", source, XPathConstants.NODESET);
					error = list.item(0).getTextContent();
				} catch (XPathExpressionException ex) {
					Logger.getLogger(ShapefileImportService.class.getName()).log(Level.SEVERE, null, ex);
				}

				responseMap.put("pts_error", error);
			}
		}

		HttpResponse importResponse = geoserverHandler.importFeaturesFromFile(shapeFile, workspace, store, name);
		String responseText = IOUtils.toString(importResponse.getEntity().getContent());

		if (!responseText.toLowerCase(Locale.getDefault()).contains("ows:exception")) {
			responseMap.put("feature", responseText);
			RequestResponseHelper.sendSuccessResponse(response, responseMap);
		} else {
			InputSource source = new InputSource(new StringReader(responseText));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList list;
			String error = "";
			try {
				list = (NodeList) xPath.evaluate("//*[local-name()='ExceptionText']", source, XPathConstants.NODESET);
				error = list.item(0).getTextContent();
			} catch (XPathExpressionException ex) {
				Logger.getLogger(ShapefileImportService.class.getName()).log(Level.SEVERE, null, ex);
			}

			responseMap.put("error", error);
			RequestResponseHelper.sendErrorResponse(response, responseMap);
		}

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}
}
