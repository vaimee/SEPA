package com.vaimee.sepa.api.commons.security;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.logging.Logging;

/**
 * <pre>
* The Class SSLManager
* 
* ### Key and Certificate Storage ###
* 
* The Java platform provides for long-term persistent storage of cryptographic
* keys and certificates via key and certificate stores. Specifically, the
* java.security.KeyStore class represents a key store, a secure repository of
* cryptographic keys and/or trusted certificates (to be used, for example,
* during certification path validation), and the java.security.cert.CertStore
* class represents a certificate store, a public and potentially vast
* repository of unrelated and typically untrusted certificates. A CertStore may
* also store CRLs.
* 
* KeyStore and CertStore implementations are distinguished by types. The Java
* platform includes the standard PKCS11 and PKCS12 key store types (whose
* implementations are compliant with the corresponding PKCS specifications from
* RSA Security). It also contains a proprietary file-based key store type
* called JKS (which stands for "Java Key Store"), and a type called DKS
* ("Domain Key Store") which is a collection of keystores that are presented as
* a single logical keystore.
* 
* ### PKI Tools ###
* 
* There are two built-in tools for working with keys, certificates, and key
* stores:
* 
* keytool is used to create and manage key stores. It can
* 
* - Create public/private key pairs
* 
* - Display, import, and export X.509 v1, v2, and v3 certificates stored as
* files
* 
* - Create self-signed certificates
* 
* ### Secure Communication ###
* 
* The data that travels across a network can be accessed by someone who is not
* the intended recipient. When the data includes private information, such as
* passwords and credit card numbers, steps must be taken to make the data
* unintelligible to unauthorized parties. It is also important to ensure that
* you are sending the data to the appropriate party, and that the data has not
* been modified, either intentionally or unintentionally, during transport.
* 
* Cryptography forms the basis required for secure communication, and that is
* described in Section 4. The Java platform also provides API support and
* provider implementations for a number of standard secure communication
* protocols.
* 
* ### SSL/TLS ###
* 
* The Java platform provides APIs and an implementation of the SSL and TLS
* protocols that includes functionality for data encryption, message integrity,
* server authentication, and optional client authentication. Applications can
* use SSL/TLS to provide for the secure passage of data between two peers over
* any application protocol, such as HTTP on top of TCP/IP.
* 
* The javax.net.ssl.SSLSocket class represents a network socket that
* encapsulates SSL/TLS support on top of a normal stream socket
* (java.net.Socket). Some applications might want to use alternate data
* transport abstractions (e.g., New-I/O); the javax.net.ssl.SSLEngine class is
* available to produce and consume SSL/TLS packets.
* 
* The Java platform also includes APIs that support the notion of pluggable
* (provider-based) key managers and trust managers. A key manager is
* encapsulated by the javax.net.ssl.KeyManager class, and manages the keys used
* to perform authentication. A trust manager is encapsulated by the
* TrustManager class (in the same package), and makes decisions about who to
* trust based on certificates in the key store it manages.
* 
* The Java platform includes a built-in provider that implements the SSL/TLS
* protocols:
* 
* SSLv3 TLSv1 TLSv1.1 TLSv1.2
 * 
 * </pre>
 * 
 * @see HostnameVerifier
 */
public class SSLManager implements HostnameVerifier {
	private static final String[] protocols = { "TLSv1.2" };
	
	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			Logging.getLogger().debug("getAcceptedIssuers");
			return new X509Certificate[0];
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			Logging.getLogger().debug("checkClientTrusted");
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			Logging.getLogger().debug("checkServerTrusted");
		}
	} };
	
	@Override
	public boolean verify(String hostname, SSLSession session) {
		// TODO IMPORTANT Verify X.509 certificate
		Logging.getLogger().debug("*** Hostname always VERIFIED ***" + hostname + " SSLSession: " + session);

		return true;
	}

	public CloseableHttpClient getSSLHttpClientTrustAllCa(String protocol) throws SEPASecurityException  {
		// Trust own CA and all self-signed certificates and allow the specified
		// protocols
		LayeredConnectionSocketFactory sslsf;
		try {
			SSLContext ctx = SSLContext.getInstance(protocol);
			ctx.init(null, trustAllCerts, new java.security.SecureRandom());
			sslsf = new SSLConnectionSocketFactory(ctx, protocols, null, this);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			Logging.getLogger().error(e.getMessage());
			throw new SEPASecurityException(e.getMessage());
		}
		HttpClientBuilder clientFactory = HttpClients.custom().setSSLSocketFactory(sslsf);

		return clientFactory.build();
	}

	public CloseableHttpClient getSSLHttpClient(String jksName, String jksPassword) throws SEPASecurityException {
		// Trust own CA and all self-signed certificates and allow the specified
		// protocols
		LayeredConnectionSocketFactory sslsf;
		try {
			SSLContext ctx = SSLContexts.custom()
					.loadTrustMaterial(new File(jksName), jksPassword.toCharArray(), new TrustSelfSignedStrategy())
					.build();
			sslsf = new SSLConnectionSocketFactory(ctx, protocols, null, this);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException
				| IOException e) {
			Logging.getLogger().error(e.getMessage());
			throw new SEPASecurityException(e.getMessage());
		}
		HttpClientBuilder clientFactory = HttpClients.custom().setSSLSocketFactory(sslsf);

		return clientFactory.build();
	}

	public SSLContext getSSLContextTrustAllCa(String protocol) throws SEPASecurityException {
		SSLContext sc;
		try {
			sc = SSLContext.getInstance(protocol);
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new SEPASecurityException(e);
		}

		return sc;
	}

	public SSLContext getSSLContextFromJKS(String jksName, String jksPassword) {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts
					.custom()
					.loadKeyMaterial(new File(jksName), jksPassword.toCharArray(), jksPassword.toCharArray())
					.setProtocol("TLS")
					.build();
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | CertificateException |
                 IOException | UnrecoverableKeyException e) {
			Logging.getLogger().error("getSSLContextFromJKS jksName:"+jksName+" jksPassword:"+jksPassword+" error:"+e.getMessage());

		}
        return sslContext;
	}

}
