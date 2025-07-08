package com.vaimee.sepa.engine.dependability.authorization;

import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;

public interface ISecurityManager {
	public Response register(String uid);
	public Response getToken(String encodedCredentials);
	public ClientAuthorization validateToken(String accessToken);
}
