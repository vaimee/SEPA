package it.unibo.arces.wot.sepa.commons.jsonld;

public class JsonLdError {
	JsonLdErrorCode code;
	String message;
	
	public JsonLdError(JsonLdErrorCode code,String message) {
		this.code = code;
		this.message = message;
	}
	
	public String toString() {
		return code + " : "+message;
	}
}
