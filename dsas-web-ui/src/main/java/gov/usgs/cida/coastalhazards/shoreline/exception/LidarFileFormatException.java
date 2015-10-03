package gov.usgs.cida.coastalhazards.shoreline.exception;

/**
 * Exception gets thrown when a file does not meet expected format for DSAS
 * defined lidar zip files.
 *
 * @author thongsav
 *
 */
public class LidarFileFormatException extends ShorelineFileFormatException {

	private static final long serialVersionUID = -5924891879480008637L;

	public LidarFileFormatException(String message) {
		super(message);
	}
}
