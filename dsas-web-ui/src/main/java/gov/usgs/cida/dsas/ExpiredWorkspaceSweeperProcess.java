package gov.usgs.cida.dsas;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.model.DSASProcessStatus;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Weeks;
import org.slf4j.LoggerFactory;

/**
 * Checks the database for expired workspaces and removes them, their views and
 * associated Geoserver workspaces.
 *
 * @author isuftin
 */
public class ExpiredWorkspaceSweeperProcess implements Runnable {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExpiredWorkspaceSweeperProcess.class);
	private static GeoserverDAO gsDao;
	private final String processId;
	private final DSASProcess process;
	private final PostgresDAO pgDao;
	private final long WORKSPACE_MAX_AGE_SECONDS;

	public ExpiredWorkspaceSweeperProcess(String processId) {
		String geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		String geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		String geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		gsDao = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);
		this.processId = StringUtils.isNotBlank(processId) ? processId : UUID.randomUUID().toString();
		this.process = new DSASProcess(this.processId, "Workspace Sweeper Process");
		this.pgDao = new PostgresDAO();
		this.WORKSPACE_MAX_AGE_SECONDS = Long.parseLong(PropertyUtil.getProperty(
				Property.WORKSPACE_MAX_AGE_SECONDS, 
				String.valueOf(Weeks.TWO.toStandardSeconds().getSeconds())));
		this.process.setStatus(DSASProcessStatus.CREATED);
		DSASProcessSingleton.addProcess(this.process);
	}

	@Override
	public void run() {
		this.process.setStatus(DSASProcessStatus.RUNNING);
		try {
			String[] expiredWorkspaces = pgDao.getExpiredWorkspaces(WORKSPACE_MAX_AGE_SECONDS);

			if (expiredWorkspaces.length > 0) {
				LOGGER.debug(String.format("Found %s workspaces that are expired. Will attempt to delete them.", expiredWorkspaces.length));
				for (String workspace : expiredWorkspaces) {
					if (removeWorkspaceFromDatabase(workspace)) {
						this.process.addProcessInformation(String.format("Removed workspae %s from Database", workspace));
					}

					if (removeWorkspaceFromGeoserver(workspace)) {
						this.process.addProcessInformation(String.format("Removed workspae %s from Geoserver", workspace));
					}
				}
			}

			this.process.setRanSuccessfully(Boolean.TRUE);
		} catch (Exception ex) {
			this.process.addProcessInformation(ex.getLocalizedMessage());
			this.process.setRanSuccessfully(Boolean.FALSE);
		} finally {
			this.process.setStatus(DSASProcessStatus.TERMINATED);
		}
	}

	private boolean removeWorkspaceFromDatabase(String workspace) throws SQLException {
		LOGGER.debug(String.format("Attempting to delete workspace %s from Database", workspace));
		boolean success = pgDao.removeWorkspace(workspace);
		if (success) {
			LOGGER.debug(String.format("Deleted workspace %s from database", workspace));
		} else {
			LOGGER.debug(String.format("Could not delete workspace %s from database", workspace));
		}
		return success;
	}

	private boolean removeWorkspaceFromGeoserver(String workspace) throws SQLException {
		LOGGER.debug(String.format("Attempting to delete workspace %s from Geoserver", workspace));
		boolean success = gsDao.deleteWorkspace(workspace);
		if (success) {
			LOGGER.debug(String.format("Deleted workspace %s from geoserver", workspace));
		} else {
			LOGGER.debug(String.format("Could not delete workspace %s from geoserver", workspace));
		}
		return success;
	}

	public String getProcessId() {
		return processId;
	}

}
