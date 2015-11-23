package gov.usgs.cida.dsas.rest.service.shapefile;

import gov.usgs.cida.dsas.DSASProcessSingleton;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.model.DSASProcessStatus;
import gov.usgs.cida.dsas.shoreline.file.IShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.TokenToShorelineFileSingleton;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author isuftin
 */
public class ShapefileImportProcess implements Runnable {

	private final String processId;
	private final String fileToken;
	private final DSASProcess process;
	private final Map<String, String> columns;

	public ShapefileImportProcess(String fileToken, Map<String, String> columns) {
		this(fileToken, columns, null);
	}

	public ShapefileImportProcess(String fileToken, Map<String, String> columns, String name) {
		this.fileToken = fileToken;
		this.processId = UUID.randomUUID().toString();
		this.process = new DSASProcess(this.processId, name);
		this.columns = columns;
		DSASProcessSingleton.addProcess(this.process);
	}

	@Override
	public void run() {
		this.process.setStatus(DSASProcessStatus.RUNNING);
		IShorelineFile shorelineFile = null;
		try {
			if (StringUtils.isNotBlank(this.fileToken)) {
				this.process.setPercentCompleted(1);
				//TODO: Change IShorelineFile to a more generic Shapefile interface, 
				// removing the idea of Shorelines from this prcess and push that down
				// to implementing classes
				shorelineFile = TokenToShorelineFileSingleton.getShorelineFile(this.fileToken);
				if (null == shorelineFile || !shorelineFile.exists()) {
					throw new FileNotFoundException(String.format("File not found for token %s", this.fileToken));
				}
				this.process.setPercentCompleted(33);
				this.process.addProcessInformation(String.format("File found for token %s", this.fileToken));
				this.process.addProcessInformation("Importing file to database");

				String viewName = shorelineFile.importToDatabase(columns);
				this.process.setPercentCompleted(66);
				this.process.addProcessInformation("Import to database complete");
				this.process.addProcessInformation("Importing to Geoserver");

				shorelineFile.importToGeoserver(viewName);
				this.process.addProcessOutput("layer", viewName);
				this.process.addProcessOutput("workspace", shorelineFile.getWorkspace());
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
			if (shorelineFile != null) {
				shorelineFile.clear();
			}
		}
	}

	public String getProcessId() {
		return processId;
	}

}
