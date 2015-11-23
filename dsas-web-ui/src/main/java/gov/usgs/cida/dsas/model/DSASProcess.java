package gov.usgs.cida.dsas.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

/**
 * Defines a (typically long-running) process.
 *
 * Is used by the client to check the state of a submitter request. Typically
 * used for long-running processes to be able to act asynchronously.
 *
 * @author isuftin
 */
public class DSASProcess {

	private final String processId;
	private final String processName;
	private DSASProcessStatus status;
	private final List<String> processInformation;
	private Integer percentCompleted;
	private Boolean ranSuccessfully = false;
	private Map<String, String> processOutput;

	/**
	 * Creates a process with a random ID
	 */
	public DSASProcess() {
		this(UUID.randomUUID().toString());
	}

	/**
	 * Creates a process with a process ID and a blank name
	 *
	 * @param processId
	 */
	public DSASProcess(String processId) {
		this(processId, "");
	}

	public DSASProcess(String processId, String processName) {
		this.processId = processId;
		this.status = DSASProcessStatus.CREATED;
		this.processInformation = new ArrayList<>();
		this.processOutput = new HashMap<>();
		this.percentCompleted = 0;
		this.processName = StringUtils.isNotBlank(processId) ? processName : "";
	}

	public String getProcessId() {
		return processId;
	}

	public String getProcessName() {
		return processName;
	}

	public DSASProcessStatus getStatus() {
		return status;
	}

	public void setStatus(DSASProcessStatus status) {
		this.status = status;
	}

	public List<String> getProcessInformation() {
		return new ArrayList<>(processInformation);
	}

	public void addProcessInformation(String processInformation) {
		this.processInformation.add(processInformation);
	}

	public Integer getPercentCompleted() {
		return percentCompleted;
	}

	public void setPercentCompleted(Integer percentCompleted) {
		this.percentCompleted = percentCompleted;
	}

	public Boolean getRanSuccessfully() {
		return ranSuccessfully;
	}

	public void setRanSuccessfully(Boolean ranSuccessfully) {
		this.ranSuccessfully = ranSuccessfully;
	}

	public Map<String, String> getProcessOutput() {
		return new HashMap<>(processOutput);
	}

	public Map<String, String> addProcessOutput(String key, String value) {
		this.processOutput.put(key, value);
		return getProcessOutput();
	}
	
	public Map<String, String> addProcessOutput(Map<String, String> output) {
		this.processOutput.putAll(output);
		return getProcessOutput();
	}
	
	public String toJSON() {
		Gson gson = new GsonBuilder().create();
		return gson.toJson(this);
	}
	
	public Map<String, Object> toMap() {
		Map<String, Object> status = new HashMap<>();
		status.put("id", this.processId);
		status.put("name", this.processName);
		status.put("status", this.status.toString());
		status.put("information", this.processInformation.toArray());
		status.put("output", new HashMap<>(this.getProcessOutput()));
		status.put("percentCompleted", this.percentCompleted);
		status.put("ranSuccessfully", this.ranSuccessfully);
		return status;
	}
}
