package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;

public class InternalDiscardRequest extends InternalRequest {

    private final String original;
    private final ErrorResponse error;

    public InternalDiscardRequest(String original, ErrorResponse error,ClientCredentials credentials){
    	super(credentials);
    	
        this.original = original;
        this.error = error;
    }

    public String getOriginal() {
        return original;
    }

    public ErrorResponse getError() {
        return error;
    }
}
