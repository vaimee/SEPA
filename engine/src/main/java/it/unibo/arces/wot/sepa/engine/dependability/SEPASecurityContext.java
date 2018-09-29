package it.unibo.arces.wot.sepa.engine.dependability;

import com.nimbusds.jose.proc.SecurityContext;

/**
Security context. Provides additional information necessary for processing a JOSE object.
Example context information:

Identifier of the message producer (e.g. OpenID Connect issuer) to retrieve its public key to verify the JWS signature.
Indicator whether the message was received over a secure channel (e.g. TLS/SSL) which is essential for processing unsecured (plain) JOSE objects.
*/
 class SEPASecurityContext implements SecurityContext {
	
}
