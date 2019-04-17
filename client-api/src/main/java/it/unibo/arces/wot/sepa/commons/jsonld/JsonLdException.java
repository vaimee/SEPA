package it.unibo.arces.wot.sepa.commons.jsonld;

public class JsonLdException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4873374219300554563L;
	
	private JsonLdError error;
	
	public JsonLdException(JsonLdError error) {
		this.error = error;
	}
	
	@Override
	public String getMessage() {
		return error.toString();
	}
}
