package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPANotExistsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1098612546657008235L;

	public SEPANotExistsException(Throwable e){
		super.initCause(e);
	}

	public SEPANotExistsException(String string) {
		super.initCause(new Exception(string));
	}
}
