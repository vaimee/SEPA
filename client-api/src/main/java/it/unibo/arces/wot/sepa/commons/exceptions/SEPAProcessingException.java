package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAProcessingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5490593006732026048L;

	public SEPAProcessingException(Throwable e){
		super.initCause(e);
	}

	public SEPAProcessingException(String string) {
		super.initCause(new Exception(string));
	}

}