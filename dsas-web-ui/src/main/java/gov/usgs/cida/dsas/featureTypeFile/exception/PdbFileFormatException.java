package gov.usgs.cida.dsas.featureTypeFile.exception;

/**
 * Exception gets thrown when a file does not meet expected format for DSAS
 * defined pdb zip files.
 *
 * @author thongsav
 *
 */
public class PdbFileFormatException extends FeatureTypeFileException {

	private static final long serialVersionUID = -1457152018867679890L;

	public PdbFileFormatException(String message) {
		super(message);
	}
}
