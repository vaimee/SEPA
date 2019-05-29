package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.dtn.JAL.BPSocket;
import it.unibo.dtn.JAL.Bundle;
import it.unibo.dtn.JAL.exceptions.JALNotRegisteredException;
import it.unibo.dtn.JAL.exceptions.JALNullPointerException;
import it.unibo.dtn.JAL.exceptions.JALReceiveException;
import it.unibo.dtn.JAL.exceptions.JALReceptionInterruptedException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;
import it.unibo.dtn.JAL.exceptions.JALSendException;
import it.unibo.dtn.JAL.exceptions.JALTimeoutException;
import it.unibo.dtn.JAL.exceptions.JALUnregisterException;

public abstract class DtnGate implements Runnable {
	
	// Logging
	private static final Logger logger = LogManager.getLogger();
	
	private Scheduler scheduler;
	
	private BPSocket socket;
	
	/**
	 * Creates a Dtn gate listening on demux indicated in demuxString and demuxIPN
	 * @param scheduler the scheduler
	 * @param demuxString the demuxString
	 * @param demuxIPN the demuxIPN
	 */
	protected DtnGate(Scheduler scheduler, String demuxString, int demuxIPN) {
		this.scheduler = scheduler;
		try {
			this.socket = BPSocket.register(demuxString, demuxIPN);
			logger.info("Opened DTN socket on : " + this.socket.getLocalEID());
		} catch (JALRegisterException e) {
			logger.error("Error on registering DTN socket. Error message: " + e.getMessage());
		}
	}

	/**
	 * Gets the internal request depending on the type of request
	 * @param sparql the sparql string
	 * @param defaultGraphUri the default pgraph uri
	 * @param namedGraphUri the named graph uri
	 * @return the Internal request correspondent
	 */
	protected abstract InternalRequest getInternalRequest(String sparql, String defaultGraphUri, String namedGraphUri);
	
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
					
					final String sparql = new String(buffer.array(), StandardCharsets.UTF_8);
					final String defaultGraphUri = null;
					final String namedGraphUri = null;

					final InternalRequest request = this.getInternalRequest(sparql, defaultGraphUri, namedGraphUri);
					final ResponseHandler handler = new HandlerDtn(bundle, this.socket);
					this.scheduler.schedule(request, handler);
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
		Thread t = new Thread(this, "Dtn gate at " + this.socket.getLocalEID());
		t.setDaemon(true);
		t.start();
	}
	
}

class HandlerDtn implements ResponseHandler {
	// Logging
	private static final Logger logger = LogManager.getLogger();
	
	private Bundle bundle;
	private BPSocket socket;
	
	public HandlerDtn(Bundle bundle, BPSocket socket) {
		this.socket = socket;
		this.bundle = bundle;
	}

	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		final String responseString = response.toString();
		
		logger.debug("Response string that is going to be sent via DTN socket : " + responseString);
		
		final DtnResponseHeader responseHeader = new DtnResponseHeader(bundle.getCreationTimestamp()); // Prepare the response header
		final Bundle responseBundle = new Bundle(this.bundle.getSource());
		final ByteBuffer buffer = ByteBuffer.allocate(responseHeader.getHeaderSize() + responseString.length());
		responseHeader.insertHeaderInByteBuffer(buffer); // Put the response header in buffer
		buffer.put(StandardCharsets.UTF_8.encode(responseString).array());
		
		responseBundle.setData(buffer.array());
		
		try {
			this.socket.send(responseBundle);
		} catch (NullPointerException | IllegalArgumentException | IllegalStateException | JALNullPointerException
				| JALNotRegisteredException | JALSendException e) {
			logger.error("Error on sending bundle. Error message: " + e.getMessage());
		}
		
		logger.debug("Bundle sent correctly");
	}

}
