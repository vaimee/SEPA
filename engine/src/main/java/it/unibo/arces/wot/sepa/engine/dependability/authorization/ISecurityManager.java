package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;

public interface ISecurityManager {
	public Response register(String uid);
	public Response getToken(String encodedCredentials);
	public ClientAuthorization validateToken(String accessToken);
}
