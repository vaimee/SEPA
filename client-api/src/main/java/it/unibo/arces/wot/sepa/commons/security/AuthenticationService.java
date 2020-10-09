package it.unibo.arces.wot.sepa.commons.security;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public abstract class AuthenticationService implements Closeable {
	protected static final Logger logger = LogManager.getLogger();

	protected CloseableHttpClient httpClient;
	protected SSLContext ctx;
	protected AuthenticationProperties oauthProperties;
	
	public AuthenticationService(AuthenticationProperties oauthProperties,String jksName,String jksPassword) throws SEPASecurityException {		
		if (!oauthProperties.trustAll()) {
			File f = new File(jksName);
			if (!f.exists() || f.isDirectory())
				throw new SEPASecurityException(jksName + " not found");
			httpClient = new SSLManager().getSSLHttpClient(jksName, jksPassword);
		}
		else httpClient = new SSLManager().getSSLHttpClientTrustAllCa(oauthProperties.getSSLProtocol());
		
		if (!oauthProperties.trustAll()) {
			File f = new File(jksName);
			if (!f.exists() || f.isDirectory())
				throw new SEPASecurityException(jksName + " not found");
			ctx = new SSLManager().getSSLContextFromJKS(jksName, jksPassword);
		}
		else ctx = new SSLManager().getSSLContextTrustAllCa(oauthProperties.getSSLProtocol());
		
		this.oauthProperties = oauthProperties;
	}
	
	public final CloseableHttpClient getSSLHttpClient() {return httpClient;}
	
	public SSLContext getSSLContext()  {
		return ctx;
	}
	
	abstract Response register(String identity,int timeout) throws SEPASecurityException;
	abstract Response requestToken(String authorization,int timeout);
	
	@Override
	public void close() throws IOException {
		httpClient.close();
	}
}
