package gov.usgs.cida.dsas.exceptions;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class InputFileFormatException extends RuntimeException {

    public InputFileFormatException() {
        super();
    }

    public InputFileFormatException(String message) {
        super(message);
    }
    
}
