package it.unibo.arces.wot.sepa.engine.dependability;

import com.nimbusds.jwt.SignedJWT;

public interface IClientAuthorization extends IAuthorizedIdentities,ICredentials,IJwt{

	SignedJWT getToken(String id);

}
