package gov.usgs.cida.dsas.rest.service.shapefile;

import gov.usgs.cida.dsas.DSASProcessSingleton;
import gov.usgs.cida.dsas.dao.postgres.PostgresDAO;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.model.DSASProcessStatus;
import gov.usgs.cida.dsas.service.util.TokenFileExchanger;
import gov.usgs.cida.dsas.featureType.file.FeatureTypeFile;
import gov.usgs.cida.dsas.featureType.file.TokenFeatureTypeFileExchanger;
import gov.usgs.cida.dsas.shoreline.file.IShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.TokenToShorelineFileSingleton;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author isuftin
 */
public class ShapefileImportProcess implements Runnable {

	private final String processId;
	private final String fileToken;
	private final String workspace;
	private final DSASProcess process;
	private final Map<String, String> columns;

	public ShapefileImportProcess(String fileToken, Map<String, String> columns, String workspace) {
		this(fileToken, columns, workspace, null);
	}

	public ShapefileImportProcess(String fileToken, Map<String, String> columns, String workspace, String name) {
		this.fileToken = fileToken;
		this.processId = UUID.randomUUID().toString();
		this.process = new DSASProcess(this.processId, name);
		this.columns = columns;
		this.workspace = workspace;
		DSASProcessSingleton.addProcess(this.process);
	}

	@Override
	public void run() {
		this.process.setStatus(DSASProcessStatus.RUNNING);
		//IShorelineFile shorelineFile = null;
		FeatureTypeFile featureTypeFile = null;
		try {
			if (StringUtils.isNotBlank(this.fileToken)) {
				this.process.setPercentCompleted(1);
				//TODO: Change IShorelineFile to a more generic Shapefile interface, 
				// removing the idea of Shorelines from this prcess and push that down
				// to implementing classes
				//shorelineFile = TokenToShorelineFileSingleton.getShorelineFile(this.fileToken); 
				featureTypeFile = TokenFeatureTypeFileExchanger.getFeatureTypeFile(this.fileToken);
				featureTypeFile.setDSASProcess(process);
				
				if (!featureTypeFile.exists()) {
					throw new FileNotFoundException(String.format("File not found for token %s", this.fileToken));
				}
				this.process.setPercentCompleted(33);
				this.process.addProcessInformation(String.format("File found for token %s", this.fileToken));
				this.process.addProcessInformation("Importing file to database");

				String viewName = featureTypeFile.importToDatabase(columns, this.workspace);
				this.process.setPercentCompleted(66);
				this.process.addProcessInformation("Import to database complete");
				
				try {
					new PostgresDAO().updateWorkspaceLastAccessTime(this.workspace); 
					this.process.addProcessInformation("Workspace last access time updated");
				} catch (SQLException ex) {
					this.process.addProcessInformation("Workspace last access time could not be updated");
				}
				
				this.process.addProcessInformation("Importing to Geoserver");

				featureTypeFile.importToGeoserver(viewName, this.workspace);
				this.process.addProcessOutput("layer", viewName);
				this.process.addProcessOutput("workspace", this.workspace);
				this.process.setPercentCompleted(100);
				this.process.addProcessInformation("Import to Geoserver complete");
				this.process.setStatus(DSASProcessStatus.TERMINATED);
				this.process.setRanSuccessfully(Boolean.TRUE); 
			} else {
				throw new IllegalArgumentException("File token was blank or null");
			}
		} catch (Exception ex) {
			this.process.setStatus(DSASProcessStatus.TERMINATED);
			this.process.addProcessInformation(ex.getMessage());
		} finally {
			if (featureTypeFile != null) {
				featureTypeFile.clear();
			}
		}
	}

	public String getProcessId() {
		return processId;
	}

}
