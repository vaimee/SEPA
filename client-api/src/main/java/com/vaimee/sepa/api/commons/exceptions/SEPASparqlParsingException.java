package com.vaimee.sepa.api.commons.exceptions;

public class SEPASparqlParsingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1675662127572029663L;
	
	private String message = null;
	
	public SEPASparqlParsingException(Throwable e){
		super.initCause(e);
	}

	public SEPASparqlParsingException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}
}
