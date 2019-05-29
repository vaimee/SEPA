package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.dtn.JAL.BPSocket;
import it.unibo.dtn.JAL.Bundle;
import it.unibo.dtn.JAL.BundleEID;
import it.unibo.dtn.JAL.exceptions.JALNotRegisteredException;
import it.unibo.dtn.JAL.exceptions.JALReceiveException;
import it.unibo.dtn.JAL.exceptions.JALReceptionInterruptedException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;
import it.unibo.dtn.JAL.exceptions.JALTimeoutException;
import it.unibo.dtn.JAL.exceptions.JALUnregisterException;

public class DtnSubscriptionServer implements Runnable {
	
	private static final String DEMUXSTRING = "/sepa/subscription";
	private static final int DEMUXIPN = 10;

	// Logging
	private static final Logger logger = LogManager.getLogger();
	
	private Map<BundleEID, DtnSubscriptionGate> map = new HashMap<>();
	
	private BPSocket socket;
	private Scheduler scheduler;
	
	public DtnSubscriptionServer(Scheduler scheduler) {
		this.scheduler = scheduler;
		try {
			this.socket = BPSocket.register(DEMUXSTRING, DEMUXIPN);
			logger.info("Opened DTN socket on demux string = " + DEMUXSTRING + " and demux ipn = " + DEMUXIPN);
		} catch (JALRegisterException e) {
			logger.error("Error on registering DTN socket. Error message: " + e.getMessage());
		}
	}
	
	@Override
	public void run() {
		boolean running = true;
		
		while (running) {
			Bundle bundle = null;
			try {
				bundle = this.socket.receive();
			} catch ( JALReceptionInterruptedException e) {
				logger.debug("Reception interrupted on receiving from DTN socket. Message: " + e.getMessage());
			} catch (JALTimeoutException e) {
				logger.debug("Timeout on receiving from DTN socket. Message: " + e.getMessage());
			} catch (JALNotRegisteredException e) {
				logger.error("Error on receiving from DTN socket, the socket was not registered. Message: " + e.getMessage());
				running = false;
			} catch (JALReceiveException e) {
				logger.error("Error on receiving from DTN socket. Message: " + e.getMessage());
				running = false;
			}
			
			if (bundle != null) {
				final byte[] data = bundle.getData();
				if (data != null) {
					final ByteBuffer buffer = ByteBuffer.wrap(data);
					
					// XXX Uncomment to enable request header
					//DtnRequestHeader requestHeader = DtnRequestHeader.getRequestHeader(buffer);
					
					DtnSubscriptionGate gate = this.map.get(bundle.getSource());
					if (gate == null) {
						gate = new DtnSubscriptionGate(this.scheduler, this.socket, bundle);
						this.map.put(bundle.getSource(), gate);
					}
					
					try {
						gate.onMessage(new String(buffer.array(), StandardCharsets.UTF_8));
					} catch (SEPAProtocolException e) {
						logger.error("Error on handling message. Message: " + e.getMessage());
					}
				}
			}
		} // while
		
		try {
			this.socket.unregister();
		} catch (JALNotRegisteredException | JALUnregisterException e) {
			logger.error("Error on unregistering DTN socket. Message: " + e.getMessage());
		}
	}
	
	public void start() {
		Thread t = new Thread(this, "Dtn subscription server at " + this.socket.getLocalEID());
		t.setDaemon(true);
		t.start();
	}
	
}
