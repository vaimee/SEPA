/* This class implements the TLS 1.0 security mechanism 
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.commons.protocol;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.security.*;
import java.security.cert.*;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.glassfish.tyrus.client.SslEngineConfigurator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpsConfigurator;


/**
 * The Class SSLSecurityManager.
 */
public class SSLSecurityManager {
	
	/** The KeyManagerFactory used to generate keys. */
	private KeyManagerFactory kmf;
	
	/** The TrustManagerFactory. */
	private TrustManagerFactory tmf;
	
	/** The SSL context. */
	private SSLContext sslContext;
	
	/** The protocol (e.g., SSL,TLSv1,...). */
	private String protocol =  "TLSv1";
	
	/** The JAVA key store. */
	//JKS
	private KeyStore keyStore = null;
	
	/** The HTTPS config. */
	//HTTPS
	private SEPAHttpsConfigurator httpsConfig;
	
	/** The WSS config. */
	//WSS
	private SEPAWssConfigurator wssConfig;
	
	/** The logger. */
	private Logger logger = LogManager.getLogger("SecurityManager");
	
	/**
	 * The Class SEPAWssConfigurator.
	 */
	public class SEPAWssConfigurator extends SslEngineConfigurator {
		
		/**
		 * Instantiates a new SEPA Secure Websocket configurator.
		 *
		 * @param sslContext the SSL context
		 * @param client if true, a client configuration is created
		 * @param hostVerificationEnabled if true, the configuration allows to verify the hostname in the SSL certificate
		 * @param hostnameVerifier allows to specify a custom hostname verifier. If null, the default hostname verification is used (based on the SSL certificate)
		 */
		public SEPAWssConfigurator(SSLContext sslContext,boolean client,boolean hostVerificationEnabled,HostnameVerifier hostnameVerifier) throws IllegalArgumentException {
			super(sslContext,client,false,false);
			if (sslContext == null) throw new IllegalArgumentException();
			
			this.setHostVerificationEnabled(hostVerificationEnabled);
			if (hostVerificationEnabled) {
				if (hostnameVerifier != null) this.setHostnameVerifier(hostnameVerifier);
			}
		}
		
		/**
		 * Instantiates a new SEPA Secure Websocket configurator.
		 *
		 * @param sslContext the SSL context
		 * @param client must be true to create a client configuration
		 */
		public SEPAWssConfigurator(SSLContext sslContext,boolean client) throws IllegalArgumentException {
			super(sslContext,client,false,false);
			if (sslContext == null) throw new IllegalArgumentException();
		}
	}
	
	/**
	 * The Class SEPAHttpsConfigurator.
	 */
	public class SEPAHttpsConfigurator extends HttpsConfigurator {

		/**
		 * Instantiates a new SEPA HTTPS configurator.
		 *
		 * @param sslContext the SSL context
		 */
		public SEPAHttpsConfigurator(SSLContext sslContext) throws IllegalArgumentException {
			super(sslContext);
			if (sslContext == null) throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Instantiates a new SSL security manager.
	 *
	 * @param keystoreFileName the keystore file name
	 * @param keystorePwd the keystore password
	 * @param keyAlias the key alias
	 * @param keyPwd the key password
	 * @param certificate the X.509 certificate
	 * @param client if true, a client configuration is created
	 * @param hostVerificationEnabled if true, the configuration allows to verify the hostname in the SSL certificate
	 * @param hostnameVerifier allows to specify a custom hostname verifier. If null, the default hostname verification is used (based on the SSL certificate)
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	/*
	 * Instantiates a new SSL security manager.
	 *
	 * @param keystoreFileName the keystore file name
	 * @param keystorePwd the keystore password
	 * @param keyAlias the key alias
	 * @param keyPwd the key password
	 * @param certificate the X.509 certificate
	 *  @param client if true, a client configuration is created
	 * @param hostVerificationEnabled if true, the configuration allows to verify the hostname in the SSL certificate
	 * @param hostnameVerifier allows to specify a custom hostname verifier. If null, the default hostname verification is used (based on the SSL certificate)
	 */
	public SSLSecurityManager(String keystoreFileName,String keystorePwd,String keyAlias,String keyPwd,String certificate,boolean client,boolean hostVerificationEnabled,HostnameVerifier hostnameVerifier) throws IllegalArgumentException {
		
		// Load certificate
		if (!loadCertificate(keystoreFileName,keystorePwd,keyPwd,keyAlias,certificate)) {
			logger.error("Failed to load SSL/TLS certificate");
			return;
		}
		
		// Create SSL context 
		try {
			sslContext = SSLContext.getInstance(protocol);
		} catch (NoSuchAlgorithmException e) {
			 logger.error(e.getMessage());
			return;
		}	
		try {
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			 logger.error(e.getMessage());
			return;
		}
		
		httpsConfig = new SEPAHttpsConfigurator(sslContext);
		
		wssConfig =  new SEPAWssConfigurator(sslContext,client,hostVerificationEnabled,hostnameVerifier);
	}
	
	/**
	 * Gets the HTTPS configurator.
	 *
	 * @return the HTTPS configurator
	 */
	public SEPAHttpsConfigurator getHttpsConfigurator(){
		return httpsConfig;
	}
	
	/**
	 * Gets the Secure Websocket configurator.
	 *
	 * @return the Secure Websocket configurator
	 */
	public SEPAWssConfigurator getWssConfigurator() {
		return wssConfig;
	}
	
	/**
	 * Load certificate.
	 *
	 * @param keystoreFilename the keystore filename
	 * @param storePassword the store password
	 * @param keyPassword the key password
	 * @param key the key
	 * @param certificate the certificate
	 * @return true, if the certificate is successfully loaded
	 */
	private boolean loadCertificate(String keystoreFilename,String storePassword,String keyPassword,String key,String certificate) {
		// Open or create the JKS
		if (!openKeyStore(keystoreFilename, storePassword, keyPassword, key, certificate)) {
			logger.error("Keystore "+keystoreFilename+ " can not be opened or created");
			return false;
		}		
		
		// Setup the key manager factory
		try {
			kmf = KeyManagerFactory.getInstance("SunX509");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			kmf.init(keyStore, keyPassword.toCharArray());
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Setup the trust manager factory
		try {
			tmf = TrustManagerFactory.getInstance("SunX509");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			tmf.init(keyStore);
		} catch (KeyStoreException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		return true;
	}

	/**
	 * Open key store.
	 *
	 * @param keystoreFilename the keystore filename
	 * @param storePassword the store password
	 * @param keyPassword the key password
	 * @param keyAlias the key alias
	 * @param certificate the certificate
	 * @return true, if the certificate is successfully loaded
	 */
	
	private boolean openKeyStore(String keystoreFilename,String storePassword,String keyPassword,String keyAlias,String certificate) {
		// JKS instance
		try {
			keyStore = KeyStore.getInstance("JKS");
		} catch (KeyStoreException e) {
			logger.fatal(e.getMessage());
			return false;
		}
		
		// Load keystore
		try {
			keyStore.load(new FileInputStream(keystoreFilename),storePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error(e.getMessage());	    
		    		     
		    try {
		    	// Create new JKS
		    	keyStore.load(null,null);

		    	X509Certificate[] chain = new X509Certificate[1];

		    	// Create key and certificate

				KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
				gen.initialize(1024);
				final KeyPair keyPair = gen.genKeyPair();

				Date startDate = new Date();     // time from which certificate is valid

				Calendar c = Calendar.getInstance();
				c.setTime(startDate);
				c.add(Calendar.YEAR, 1);

				Date expiryDate = c.getTime();      // time after which certificate is not valid


				X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
				/*
		    	 * RFC 1779 or RFC 2253 style
		    	 *
		    	commonName - common name of a person, e.g. "Vivette Davis"
		    	organizationUnit - small organization name, e.g. "Purchasing"
		    	organizationName - large organization name, e.g. "Onizuka, Inc."
		    	localityName - locality (city) name, e.g. "Palo Alto"
		    	stateName - state name, e.g. "California"
		    	country - two letter country code, e.g. "CH"

		    	https://www.ietf.org/rfc/rfc3280.txt

		    	Subject Alternative Name
		    	https://tools.ietf.org/html/rfc5280#section-4.2.1.6

		    	*/
				X500Principal              dnName = new X500Principal("CN=Luca Roffia," +
						"OU=Web of Things Research Group," +
						"O=ARCES - University of Bologna," +
						"L=Bologna," +
						"ST=Italy," +
						"C=IT");

				certGen.setIssuerDN(dnName);
				certGen.setNotBefore(startDate);
				certGen.setNotAfter(expiryDate);
				certGen.setSubjectDN(dnName);                       // note: same as issuer
				certGen.setPublicKey(keyPair.getPublic());
				certGen.setSignatureAlgorithm("SHA1WithRSA");
				X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");


			    chain[0]=cert;
		    	keyStore.setKeyEntry(keyAlias, keyPair.getPrivate(), keyPassword.toCharArray(), chain);
		    	
		    	// Set certificate entry	
				keyStore.setCertificateEntry(certificate, cert);
				
				// Save JKS
				keyStore.store(new FileOutputStream(keystoreFilename), storePassword.toCharArray());
			} catch (KeyStoreException | InvalidKeyException | CertificateException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | IOException e1) {
				logger.error(e1.getMessage());
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Gets the RSA Key from the keystore.
	 *
	 * @param keyAlias the key alias
	 * @param keyPwd the key password
	 * @return the RSAKey
	 * 
	 * @see RSAKey
	 */
	public RSAKey getJWK(String keyAlias,String keyPwd) {
		RSAKey jwk = null;
		try {
			jwk = RSAKey.load(keyStore, keyAlias, keyPwd.toCharArray());
		} catch (KeyStoreException | JOSEException e) {
			logger.error(e.getMessage());
			return null;
		}
		return jwk;
	}
}
