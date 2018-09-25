package it.unibo.arces.wot.sepa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
 
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
 
@ClientEndpoint
public class ConnectTesterTyrus {
 
	protected Logger logger = LogManager.getLogger();
 
    @OnMessage
    public String onMessage(String message, Session session) {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        try {
            logger.info("Received ...." + message);
            String userInput = bufferRead.readLine();
            return userInput;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
 
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
    }

	@OnOpen
	public void onOpen(Session session, EndpointConfig arg1) {
		logger.info("Connected ... " + session.getId());
		
	}
	
	@OnError
    public void onError(Session session, Throwable t) {
        t.printStackTrace();
    }
}