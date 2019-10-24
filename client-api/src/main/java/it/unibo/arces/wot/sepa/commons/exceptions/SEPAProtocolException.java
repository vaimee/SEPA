package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAProtocolException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4338523280742810802L;

	private String message = null;
	
	public SEPAProtocolException(Throwable e){
		super.initCause(e);
	}
	
	public SEPAProtocolException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}
}
