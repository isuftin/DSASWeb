package gov.usgs.cida.dsas.exceptions;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class PoorlyDefinedBaselineException extends RuntimeException {

    public PoorlyDefinedBaselineException() {
        super();
    }

    public PoorlyDefinedBaselineException(String message) {
        super(message);
    }
    
}
