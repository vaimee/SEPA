package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPABindingsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8836990995757328932L;

	public SEPABindingsException(Throwable e){
		super.initCause(e);
	}

	public SEPABindingsException(String string) {
		super.initCause(new Exception(string));
	}

}
