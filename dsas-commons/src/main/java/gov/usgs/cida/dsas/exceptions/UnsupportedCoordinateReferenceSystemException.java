package gov.usgs.cida.dsas.exceptions;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class UnsupportedCoordinateReferenceSystemException extends RuntimeException {

    public UnsupportedCoordinateReferenceSystemException(String message) {
        super(message);
    }

    public UnsupportedCoordinateReferenceSystemException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
