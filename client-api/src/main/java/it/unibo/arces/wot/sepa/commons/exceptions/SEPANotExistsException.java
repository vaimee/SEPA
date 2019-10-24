package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPANotExistsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1098612546657008235L;

	private String message = null;
	
	public SEPANotExistsException(Throwable e){
		super.initCause(e);
	}

	public SEPANotExistsException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}
}
