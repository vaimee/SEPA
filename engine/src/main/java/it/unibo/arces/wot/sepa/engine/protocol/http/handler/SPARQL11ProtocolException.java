package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

public class SPARQL11ProtocolException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1400541672372477641L;
	private final int code;
    private final String body;

    SPARQL11ProtocolException(int code, String body){
        this.code = code;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }
}
