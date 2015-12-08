package gov.usgs.cida.dsas.featureTypeFile.exception;

/**
 * Exception gets thrown when a file does not meet expected format for a shape
 * file
 *
 * @author isuftin
 *
 */
public class FeatureTypeFileException extends Exception {

	private static final long serialVersionUID = 3920876944615826227L;

	public FeatureTypeFileException() {
		super();
	}

	public FeatureTypeFileException(Throwable cause) {
		super(cause);
	}

	public FeatureTypeFileException(String message) {
		super(message);
	}

	public FeatureTypeFileException(Throwable cause, String message) {
		super(message, cause);
	}
}
