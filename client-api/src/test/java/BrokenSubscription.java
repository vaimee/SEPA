import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.INotificationHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class BrokenSubscription extends Thread implements INotificationHandler {
	protected static Logger logger = LogManager.getLogger("BrokenSubscription");
	
	SPARQL11SEProperties properties;
	SPARQL11SEProtocol client;
	
	public static void main(String[] args) {
		BrokenSubscription test = null;
		try {
			test = new BrokenSubscription();
			test.start();
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | IllegalArgumentException
				| NoSuchElementException | NullPointerException | ClassCastException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			logger.error(e);
		}
		try {
			synchronized(test) {
				test.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			client.subscribe(new SubscribeRequest("select * where {?x ?y ?z}"));
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			logger.error(e);
		}
		while(true) {
			
		}
	}
	
	public BrokenSubscription() throws InvalidKeyException, IllegalArgumentException, FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException, InterruptedException {
		SPARQL11SEProperties properties = new SPARQL11SEProperties("test.jpar");
		client = new SPARQL11SEProtocol(properties,this);	
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.info("onSemanticEvent: "+notify);
	}

	@Override
	public void onSubscribeConfirm(SubscribeResponse response) {
		logger.info("onSubscribeConfirm: "+response);
		
	}

	@Override
	public void onUnsubscribeConfirm(UnsubscribeResponse response) {
		logger.info("onUnsubscribeConfirm: "+response);
		
	}

	@Override
	public void onPing() {
		logger.info("onPing");
		
	}

	@Override
	public void onBrokenSocket() {
		logger.info("onBrokenSocket");
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.info("onError: "+errorResponse);
		
	}
}
