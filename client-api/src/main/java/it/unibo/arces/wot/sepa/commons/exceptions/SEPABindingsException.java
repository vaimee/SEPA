package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPABindingsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8836990995757328932L;

	private String message = null;
	
	public SEPABindingsException(Throwable e){
		super.initCause(e);
	}

	public SEPABindingsException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}

}
