package it.unibo.arces.wot.sepa.engine.protocol.tcp;

import java.io.DataOutputStream;
import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class ResponseTCPHandler implements EventHandler{
	
	DataOutputStream outSock = null;
	
	public ResponseTCPHandler(DataOutputStream outSock){
		//
		this.outSock = outSock;
	}
	
	@Override
	public void sendResponse(Response response) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("RISPOSTA:\n\n" + response.toString());
		outSock.writeUTF(response.toString());	
	}

	@Override
	public void notifyEvent(Notification notify) throws IOException {
		// TODO Auto-generated method stub
		outSock.writeUTF(notify.toString());
	}

	@Override
	public void sendPing(Ping ping) throws IOException {
		// TODO Auto-generated method stub
		
	}

	


	

}
