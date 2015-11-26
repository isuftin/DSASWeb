package gov.usgs.cida.dsas;

import gov.usgs.cida.dsas.model.DSASProcess;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author isuftin
 */
public class DSASProcessSingleton {

	private static final Map<String, DSASProcess> procMap = new ConcurrentHashMap<>();

	/**
	 * Gets a DSASProcess from the DSASProcessSingleton
	 *
	 * @param processId
	 * @return
	 */
	public static DSASProcess getProcess(String processId) {
		return procMap.get(processId);
	}

	/**
	 * Adds a process to the singleton. An existing process with a given id will be 
	 * replaced with the new process.
	 * 
	 * @param process
	 * @return returns the process added. 
	 */
	public static DSASProcess addProcess(DSASProcess process) {
		if (process != null) {
			procMap.put(process.getProcessId(), process);
		}
		return process;
	}

	/**
	 * Removes a process from the singleton
	 * 
	 * @param processId
	 * @return the process removed, if found. Otherwise null
	 */
	public static DSASProcess removeDSASProcess(String processId) {
		return procMap.remove(processId);
	}
	
	public static String[] getProcessIds() {
		return procMap.keySet().toArray(new String[procMap.keySet().size()]);
	}
	
	private DSASProcessSingleton() {
		// Static utility class
	}
}
