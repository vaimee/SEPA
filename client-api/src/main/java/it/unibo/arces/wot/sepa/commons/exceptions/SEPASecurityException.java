package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPASecurityException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8556364187417032892L;

	private String message = null;
	
	public SEPASecurityException(Throwable e){
		super.initCause(e);
	}

	public SEPASecurityException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}
}
