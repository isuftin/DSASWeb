package gov.usgs.cida.coastalhazards.shoreline.exception;

/**
 * Exception gets thrown when a file does not meet expected format for a shape
 * file
 *
 * @author isuftin
 *
 */
public class ShapefileFileFormatExtension extends ShorelineFileFormatException {

	private static final long serialVersionUID = 3920876944615826227L;

	public ShapefileFileFormatExtension(String message) {
		super(message);
	}
}
