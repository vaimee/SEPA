package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPASecurityException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8556364187417032892L;

	public SEPASecurityException(Throwable e){
		super.initCause(e);
	}

	public SEPASecurityException(String string) {
		super.initCause(new Exception(string));
	}
}
