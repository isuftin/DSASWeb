package gov.usgs.cida.coastalhazards;

import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.coastalhazards.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

/**
 * Web application lifecycle listener.
 *
 * @author isuftin
 */
public class InitListener implements ServletContextListener {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InitListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOGGER.info("Coastal Hazards UI Application Initializing.");
		ServletContext sc = sce.getServletContext();
		String key = Property.JDBC_NAME.getKey();
		String initParameter = sc.getInitParameter(key);
		System.setProperty(key, initParameter);

		String baseDir = PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectoryPath() + "/coastal-hazards");
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

		// TODO- Create file cleanup service for work and upload directories
		LOGGER.info("Coastal Hazards UI Application Initialized.");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("Coastal Hazards UI Application Destroying.");
		// Do stuff here for application cleanup
		LOGGER.info("Coastal Hazards UI Application Destroyed.");
	}

	private void createDir(File directory) {
		try {
			FileUtils.forceMkdir(directory);
		} catch (IOException ex) {
			LOGGER.error(MessageFormat.format("** Work application directory ({0}) could not be created -- the application should not be expected to function normally", directory.getPath()), ex);
		}
	}
}
