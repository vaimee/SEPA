package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAProtocolException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4338523280742810802L;

	public SEPAProtocolException(Throwable e){
		super.initCause(e);
	}
	
	public SEPAProtocolException(String s) {
		super.initCause(new Exception(s));
	}
}
