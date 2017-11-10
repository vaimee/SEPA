package it.unibo.arces.wot.sepa.webthings.rfidreader;

import java.io.IOException;
import java.util.Set;

import it.unibo.arces.wot.framework.elements.Event;
import it.unibo.arces.wot.framework.interaction.EventListener;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

public class RFIDEventListener extends EventListener {

	public RFIDEventListener() throws SEPAPropertiesException  {
		super();
	}
	
	public static void main(String[] args) throws SEPAPropertiesException, SEPAProtocolException, SEPASecurityException, IOException {
			
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
