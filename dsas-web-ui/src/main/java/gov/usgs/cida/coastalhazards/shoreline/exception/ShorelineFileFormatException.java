package gov.usgs.cida.coastalhazards.shoreline.exception;

/**
 *
 * @author isuftin
 */
public class ShorelineFileFormatException extends Exception {

	private static final long serialVersionUID = -1833974645020482903L;

	public ShorelineFileFormatException() {
		super();
	}

	public ShorelineFileFormatException(Throwable cause) {
		super(cause);
	}

	public ShorelineFileFormatException(String message) {
		super(message);
	}

	public ShorelineFileFormatException(Throwable cause, String message) {
		super(message, cause);
	}
}
