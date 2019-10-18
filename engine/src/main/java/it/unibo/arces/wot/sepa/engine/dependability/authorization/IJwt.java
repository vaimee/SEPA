package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.util.Date;

import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IJwt {
	void addToken(String id,SignedJWT claims) throws SEPASecurityException;
	boolean containsToken(String id) throws SEPASecurityException;
	SignedJWT getToken(String uid) throws SEPASecurityException;
	
	Date getTokenExpiringDate(String id) throws SEPASecurityException;
	long getTokenExpiringPeriod(String id) throws SEPASecurityException;
	void setTokenExpiringPeriod(String id, long period) throws SEPASecurityException;
	
	void setDeviceExpiringPeriod(long period) throws SEPASecurityException;
	long getDeviceExpiringPeriod() throws SEPASecurityException;
	void setApplicationExpiringPeriod(long period) throws SEPASecurityException;
	long getApplicationExpiringPeriod() throws SEPASecurityException;
	void setUserExpiringPeriod(long period) throws SEPASecurityException;
	long getUserExpiringPeriod() throws SEPASecurityException;
	void setDefaultExpiringPeriod(long period) throws SEPASecurityException;
	long getDefaultExpiringPeriod() throws SEPASecurityException;
	
	String getIssuer() throws SEPASecurityException;
	void setIssuer(String is) throws SEPASecurityException;
	String getHttpsAudience() throws SEPASecurityException;
	void setHttpsAudience(String audience) throws SEPASecurityException;
	String getWssAudience() throws SEPASecurityException;
	void setWssAudience(String audience) throws SEPASecurityException;
	String getSubject() throws SEPASecurityException;
	void setSubject(String sub) throws SEPASecurityException;	
}
