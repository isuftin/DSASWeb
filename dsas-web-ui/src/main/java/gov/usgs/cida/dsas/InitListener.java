package gov.usgs.cida.dsas;

import gov.usgs.cida.auth.client.AuthClientSingleton;
import gov.usgs.cida.auth.client.CachingAuthClient;
import gov.usgs.cida.auth.client.NullAuthClient;
import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Web application lifecycle listener.
 *
 * @author isuftin
 */
@WebListener
public class InitListener implements ServletContextListener {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InitListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOGGER.info("DSASWeb Application Initializing.");
		ServletContext sc = sce.getServletContext();
		String key = Property.JDBC_NAME.getKey();
		String initParameter = sc.getInitParameter(key);
		System.setProperty(key, initParameter);

		createWorkingDirectories();

		createGeoserverWorkspaces();

		// TODO- Create file cleanup service for work and upload directories
		LOGGER.info("DSASWeb UI Application Initialized.");
	}

	private void createWorkingDirectories() {
		String baseDir = PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectoryPath() + "/DSASWeb");
		String workDir = PropertyUtil.getProperty(Property.DIRECTORIES_WORK, "/work");
		String uploadDir = PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD, "/upload");
		File baseDirFile, workDirFile, uploadDirFile;

		baseDirFile = new File(baseDir);
		workDirFile = new File(baseDirFile, workDir);
		uploadDirFile = new File(baseDirFile, uploadDir);

		if (!baseDirFile.exists()) {
			createDir(baseDirFile);
		}

		if (!workDirFile.exists()) {
			createDir(workDirFile);
		}

		if (!uploadDirFile.exists()) {
			createDir(uploadDirFile);
		}
	}

	private void createGeoserverWorkspaces() {
		try {
			LOGGER.info("Updating published workspace in database");
			new ShorelineShapefileDAO().createViewAgainstPublishedWorkspace();

			LOGGER.info("Updating published workspace in Geoserver");
			String geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
			String geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
			String geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
			GeoserverDAO geoserverHandler = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);
			geoserverHandler.createOrUpdatePublishedWorkspaceOnGeoserver();
		} catch (SQLException ex) {
			LOGGER.warn("Could not access published workspace. This may affect the proper funcitoning of the application", ex);
		} catch (IOException ex) {
			LOGGER.warn("Could not create or update published workspace on Geoserver. This may affect the proper funcitoning of the application", ex);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("DSASWeb Application Destroying.");
		// Do stuff here for application cleanup
		LOGGER.info("DSASWeb Application Destroyed.");
	}

	private void createDir(File directory) {
		try {
			FileUtils.forceMkdir(directory);
		} catch (IOException ex) {
			LOGGER.error(MessageFormat.format("** Work application directory ({0}) could not be created -- the application should not be expected to function normally", directory.getPath()), ex);
		}
	}
}
