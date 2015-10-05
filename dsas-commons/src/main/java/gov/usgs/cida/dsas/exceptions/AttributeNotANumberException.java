package gov.usgs.cida.dsas.exceptions;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class AttributeNotANumberException extends Exception {
	
	private static final long serialVersionUID = -9069102077075615997L;

	public AttributeNotANumberException() {
		super();
	}

	public AttributeNotANumberException(String message) {
		super(message);
	}

}
