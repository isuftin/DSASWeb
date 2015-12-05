package gov.usgs.cida.dsas.model;

/**
 * Defines the current status of a process
 * 
 * @author isuftin
 */
public enum DSASProcessStatus {

	CREATED("created"),
	RUNNING("running"),
	TERMINATED("terminated");

	private final String status;

	private DSASProcessStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return this.status;
	}
}
