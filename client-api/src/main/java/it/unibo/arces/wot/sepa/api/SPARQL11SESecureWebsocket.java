package it.unibo.arces.wot.sepa.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

public class SPARQL11SESecureWebsocket extends SPARQL11SEWebsocket {

	// load up the key store
	private String STORETYPE = "JKS";
	private String KEYSTORE = "sepa.jks";
	private String STOREPASSWORD = "sepa2017";
	private String KEYPASSWORD = "sepa2017";
	
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
			// sslContext.init( null, null, null ); // will use java's default key and trust
			// store which is sufficient unless you deal with self-signed certificates

			SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory)
																		// SSLSocketFactory.getDefault();

			client.setSocket(factory.createSocket());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
			throw new SEPASecurityException(e);
		}
	}

}
