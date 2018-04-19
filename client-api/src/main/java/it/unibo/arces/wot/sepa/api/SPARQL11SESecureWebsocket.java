package it.unibo.arces.wot.sepa.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class SPARQL11SESecureWebsocket extends SPARQL11SEWebsocket {

	// load up the key store
	private String STORETYPE = "JKS";
	private String KEYSTORE = "sepa.jks";
	private String STOREPASSWORD = "sepa2017";
	private String KEYPASSWORD = "sepa2017";
	
	private Socket secureSocket = null;
	
	@Override
	public Response subscribe(String sparql,String alias)  {
		return new ErrorResponse(500,"Not implemented. Use the secure version.");
	}
	
	@Override
	public Response subscribe(String sparql) {
		return new ErrorResponse(500,"Not implemented. Use the secure version.");
	}
	
	public Response secureSubscribe(String sparql,String authorization,String alias) {
		return _subscribe(sparql,authorization,alias);
	}
	
	public Response secureSubscribe(String sparql,String authorization) {
		return _subscribe(sparql,authorization,null);
	}
	
	@Override
	public Response unsubscribe(String spuid) {
		return new ErrorResponse(500,"Not implemented. Use the secure version.");
	}
	
	public Response secureUnsubscribe(String spuid,String authorization) {
		return _unsubscribe(spuid,authorization);
	}
	
	@Override
	protected boolean connect() {
		if (!super.connect()) return false;
		client.setSocket(secureSocket);
		return true;
	}
	
	public SPARQL11SESecureWebsocket(String wsUrl, ISubscriptionHandler handler)
			throws SEPAProtocolException, SEPASecurityException {
		super(wsUrl, handler);

		KeyStore ks;
		try {
			ks = KeyStore.getInstance(STORETYPE);

			File kf = new File(KEYSTORE);
			ks.load(new FileInputStream(kf), STOREPASSWORD.toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, KEYPASSWORD.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			SSLContext sslContext = null;
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			SSLSocketFactory factory = sslContext.getSocketFactory();

			secureSocket = factory.createSocket();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
			throw new SEPASecurityException(e);
		}
	}

}
