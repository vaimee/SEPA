package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.Date;

import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IJwt {
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException;

	public void setTokenExpiringPeriod(String id, long period) throws SEPASecurityException;

	public String getIssuer() throws SEPASecurityException;

	public void setIssuer(String is) throws SEPASecurityException;

	public String getHttpsAudience() throws SEPASecurityException;

	public void setHttpsAudience(String audience) throws SEPASecurityException;

	public String getWssAudience() throws SEPASecurityException;

	public void setWssAudience(String audience) throws SEPASecurityException;

	public String getSubject() throws SEPASecurityException;

	public void setSubject(String sub) throws SEPASecurityException;
	
	public void addToken(String id,SignedJWT claims) throws SEPASecurityException;
	
	public boolean containsToken(String id) throws SEPASecurityException;
	
	public Date getExpiringTime(String id) throws SEPASecurityException;
}
