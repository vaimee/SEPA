package it.unibo.arces.wot.sepa.commons.exceptions;

public class SEPAPropertiesException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2906022687133673347L;

	public SEPAPropertiesException(Throwable e){
		super.initCause(e);
	}

}
