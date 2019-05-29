package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.engine.gates.Gate;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.dtn.JAL.BPSocket;
import it.unibo.dtn.JAL.Bundle;
import it.unibo.dtn.JAL.exceptions.JALNotRegisteredException;
import it.unibo.dtn.JAL.exceptions.JALNullPointerException;
import it.unibo.dtn.JAL.exceptions.JALSendException;

public class DtnSubscriptionGate extends Gate {
	
	// Logging
	private static final Logger logger = LogManager.getLogger();
	
	private Bundle bundle;
	private BPSocket socket;

	public DtnSubscriptionGate(Scheduler scheduler, BPSocket socket, Bundle bundle) {
		super(scheduler);
		this.bundle = bundle;
		this.socket = socket;
	}

	@Override
	public void send(String response) throws SEPAProtocolException {
		Bundle bundle = new Bundle(this.bundle.getSource());
		byte[] responseData = StandardCharsets.UTF_8.encode(response).array();
		
		DtnResponseHeader header = new DtnResponseHeader(this.bundle.getCreationTimestamp());
		ByteBuffer buffer = ByteBuffer.allocate(header.getHeaderSize() + responseData.length);
		header.insertHeaderInByteBuffer(buffer);
		buffer.put(responseData);
		
		try {
			this.socket.send(bundle);
		} catch (NullPointerException | IllegalArgumentException | IllegalStateException | JALNullPointerException
				| JALNotRegisteredException | JALSendException e) {
			logger.error("Error on sending bundle. Message: " + e.getMessage());
		}
	}

}
