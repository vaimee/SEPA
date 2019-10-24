package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAProcessingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5490593006732026048L;

	private String message = null;
	
	public SEPAProcessingException(Throwable e){
		super.initCause(e);
	}

	public SEPAProcessingException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}

}