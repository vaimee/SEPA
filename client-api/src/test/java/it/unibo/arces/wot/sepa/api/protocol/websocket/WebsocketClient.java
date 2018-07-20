package it.unibo.arces.wot.sepa.api.protocol.websocket;

import static org.junit.Assert.assertFalse;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebsocketClient implements Runnable {
	private WebsocketSubscriptionProtocol client = null;
	private SubscribeRequest request = null;
	
	public WebsocketClient(String host,int port,String path,SEPASecurityManager sm,SubscribeRequest request,ISubscriptionHandler handler) throws SEPAProtocolException {
		if (sm != null) client = new WebsocketSubscriptionProtocol(host, port,
				path, sm, handler);
		else client = new WebsocketSubscriptionProtocol(host, port,
				path, handler);
		this.request = request;
	}
	
		
	public void run() {
		try {
			client.subscribe(request);
		} catch (SEPAProtocolException e1) {
			assertFalse(e1.getMessage(),true);
		}

		try {
			Thread.sleep((long) (500+500*Math.random()));
		} catch (InterruptedException e) {

		}
		
		client.close();	
	} 
}
