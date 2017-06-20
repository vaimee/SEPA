package it.unibo.arces.wot.sepa.tools;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

public class MQTTAdapter extends Producer implements MqttCallback {
	private MqttClient mqttClient;
	private String serverURI = "ssl://giove.mars:8883";
	
	private static String clientID = "MQTTAdapter";
	
	private String[] topicsFilter = {"arces/servers/#"};
	static MQTTAdapter adapter;
	private boolean created = false;
	
	public static HashMap<String,String> debugHash = new HashMap<String,String>();
 	
	private static final Logger logger = LogManager.getLogger("MQTTAdapter");
	
	public static class SslUtil {

	    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
	                                                    final String password) {
	        try {

	            /**
	             * Add BouncyCastle as a Security Provider
	             */
	            Security.addProvider(new BouncyCastleProvider());

	            JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");

	            /**
	             * Load Certificate Authority (CA) certificate
	             */
	            PEMParser reader = new PEMParser(new FileReader(caCrtFile));
	            X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
	            reader.close();

	            X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);

	            /**
	             * Load client certificate
	             */
	            reader = new PEMParser(new FileReader(crtFile));
	            X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
	            reader.close();

	            X509Certificate cert = certificateConverter.getCertificate(certHolder);

	            /**
	             * Load client private key
	             */
	            reader = new PEMParser(new FileReader(keyFile));
	            Object keyObject = reader.readObject();
	            reader.close();

	            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
	            JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

	            KeyPair key;

	            if (keyObject instanceof PEMEncryptedKeyPair) {
	                key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
	            } else {
	                key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
	            }

	            /**
	             * CA certificate is used to authenticate server
	             */
	            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	            caKeyStore.load(null, null);
	            caKeyStore.setCertificateEntry("ca-certificate", caCert);

	            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
	                    TrustManagerFactory.getDefaultAlgorithm());
	            trustManagerFactory.init(caKeyStore);

	            /**
	             * Client key and certificates are sent to server so it can authenticate the client
	             */
	            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	            clientKeyStore.load(null, null);
	            clientKeyStore.setCertificateEntry("certificate", cert);
	            clientKeyStore.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
	                    new Certificate[]{cert});

	            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
	                    KeyManagerFactory.getDefaultAlgorithm());
	            keyManagerFactory.init(clientKeyStore, password.toCharArray());

	            /**
	             * Create SSL socket factory
	             */
	            SSLContext context = SSLContext.getInstance("TLSv1.2");
	            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

	            /**
	             * Return the newly created socket factory object
	             */
	            return context.getSocketFactory();

	        } catch (Exception e) {
	        	logger.error(e.getMessage());
	        }

	        return null;
	    }
	}
	
	public MQTTAdapter(ApplicationProfile appProfile, String updateID) throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile, updateID);
		
		try 
		{
			mqttClient = new MqttClient(serverURI,clientID);
		} 
		catch (MqttException e) {
			logger.error("Failed to create MQTT client "+e.getMessage());
			return ;
		}
		
		try 
		{
			MqttConnectOptions options = new MqttConnectOptions();
			SSLSocketFactory ssl = SslUtil.getSocketFactory("/usr/local/mosquitto-certs/ca.crt", "/usr/local/mosquitto-certs/mml.crt", "/usr/local/mosquitto-certs/mml.key", "");
			if (ssl == null) {
				logger.error("SSL security option creation failed");
			}
			else options.setSocketFactory(ssl);
			mqttClient.connect(options);
		} 
		catch (MqttException e) {
			logger.error("Failed to connect "+e.getMessage());
			return ;
		}
		
		mqttClient.setCallback(this);
		
		try 
		{
			mqttClient.subscribe(topicsFilter);
		} 
		catch (MqttException e) {
			logger.fatal("Failed to subscribe "+e.getMessage());
			return ;
		}
		
		String topics = "";
		for (int i=0; i < topicsFilter.length;i++) topics += "\""+ topicsFilter[i] + "\" ";
		
		logger.info("MQTT client "+clientID+" subscribed to "+serverURI+" Topic filter "+topics);
	
		created = true;
	}

	@Override
	public void connectionLost(Throwable arg0) {
		
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage value) throws Exception {
		logger.debug(topic+ " "+value.toString());
		
		String node = "iot:"+topic.replace('/', '_');
		String temperature = value.toString();
		Bindings bindings = new Bindings();
		bindings.addBinding("node", new RDFTermURI(node));
		bindings.addBinding("value", new RDFTermLiteral(temperature));
		adapter.update(bindings);
		
		if (debugHash.containsKey(node)) {
			if (!debugHash.get(node).equals(temperature)) {
				logger.debug(topic+ " "+debugHash.get(node)+"-->"+temperature.toString());	
			}
		}
		
		debugHash.put(node, temperature);
	}
	
	public boolean join() {
		return created;
	}

	public static void main(String[] args) {
		//SEPALogger.loadSettings();
		
		ApplicationProfile profile = null;
		try {
			profile = new ApplicationProfile("MQTTAdapter.jsap");
		} catch (NoSuchElementException | IOException | InvalidKeyException | IllegalArgumentException | NullPointerException | ClassCastException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		try {
			adapter = new MQTTAdapter(profile,"UPDATE");
		} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException
				| NoSuchAlgorithmException | CertificateException | IOException | URISyntaxException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		if (!adapter.join()) return;
		
		logger.info("Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}
}
