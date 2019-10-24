package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAPropertiesException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2906022687133673347L;

	private String message = null;
	
	public SEPAPropertiesException(Throwable e){
		super.initCause(e);
	}

	public SEPAPropertiesException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}

}
