package it.unibo.arces.wot.sepa.webthings.rfidreader;
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
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.framework.Event;
import it.unibo.arces.wot.sepa.framework.interaction.EventListener;

public class RFIDEventListener extends EventListener {

	public RFIDEventListener() throws InvalidKeyException, FileNotFoundException, NoSuchElementException,
			IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		super();
	}
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException, URISyntaxException, InvalidKeyException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, InterruptedException {
			
		RFIDEventListener thing = new RFIDEventListener();
		
		thing.startListeningForEvent("wot:Ping");
		thing.startListeningForEvent("wot:TagsPollChanged");
		//thing.startListeningForEvent("wot:RFIDReader", "wot:Ping");
		//thing.startListeningForEvent("wot:RFIDReader", "wot:TagsPollChanged");
		
		System.out.println("Press x to exit...");
		
		while(System.in.read()!='x'){}
		
		System.out.println("RFID Event Listener stopped");;
	}

	@Override
	public void onEvent(Set<Event> events) {
		for (Event e: events) System.out.println(e);
		
	}

	@Override
	public void onConnectionStatus(Boolean on) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionError(ErrorResponse error) {
		// TODO Auto-generated method stub
		
	}

	
}
