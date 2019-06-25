package it.unibo.arces.wot.sepa.api.protocols.dtn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.dtn.JAL.BPSocket;
import it.unibo.dtn.JAL.Bundle;
import it.unibo.dtn.JAL.BundleEID;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALNotRegisteredException;
import it.unibo.dtn.JAL.exceptions.JALNullPointerException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALReceiveException;
import it.unibo.dtn.JAL.exceptions.JALReceptionInterruptedException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;
import it.unibo.dtn.JAL.exceptions.JALSendException;
import it.unibo.dtn.JAL.exceptions.JALTimeoutException;

public class DTNSubscriptionProtocol implements SubscriptionProtocol {

	private static final int DEMUXIPN = new Random(System.currentTimeMillis()).nextInt(1000) + 10000;
	private static final String DEMUXSTRING = "/sepa/subscription_" + DEMUXIPN;
	
	private BPSocket socket = null;
	private BundleEID destination;
	private ISubscriptionHandler handler = null;
	
	private Thread thread;
	
	public DTNSubscriptionProtocol(BundleEID destination) throws JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		this.socket = BPSocket.register(DEMUXSTRING, DEMUXIPN);
		this.destination = destination;
		this.thread = new Thread(new ReceiverThread(this), "DTN receiver thread");
		this.thread.setDaemon(true);
		this.thread.start();
	}
	
	@Override
	public void close() throws IOException {
		this.socket.unregister();
		this.thread.interrupt();
	}

	@Override
	public void setHandler(ISubscriptionHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("Handler is null");

		this.handler = handler;
	}
	
	protected ISubscriptionHandler getHandler() {
		return this.handler;
	}
	
	protected BPSocket getSocket() {
		return this.socket;
	}

	@Override
	public void enableSecurity(SEPASecurityManager sm) throws SEPASecurityException {
		// not enabled
	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		String requestString = request.toString();
		byte[] requestBytes = requestString.getBytes(StandardCharsets.UTF_8);

		Bundle bundle = new Bundle(this.destination, (int) request.getTimeout());
		bundle.setData(requestBytes);
		try {
			this.socket.send(bundle);
		} catch (NullPointerException | IllegalArgumentException | IllegalStateException | JALNullPointerException
				| JALNotRegisteredException | JALSendException e) {
			throw new SEPAProtocolException(e);
		}
	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		String requestString = request.toString();
		byte[] requestBytes = requestString.getBytes(StandardCharsets.UTF_8);

		Bundle bundle = new Bundle(this.destination, (int) request.getTimeout());
		bundle.setData(requestBytes);
		try {
			this.socket.send(bundle);
		} catch (NullPointerException | IllegalArgumentException | IllegalStateException | JALNullPointerException
				| JALNotRegisteredException | JALSendException e) {
			throw new SEPAProtocolException(e);
		}
	}
	
	private class ReceiverThread implements Runnable {
		
		private DTNSubscriptionProtocol subscriptionProtocol;
		
		public ReceiverThread(DTNSubscriptionProtocol subscriptionProtocol) {
			this.subscriptionProtocol = subscriptionProtocol;
		}
		
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				Bundle bundle = null;
				try {
					bundle = this.subscriptionProtocol.getSocket().receive();
				} catch (JALReceptionInterruptedException | JALTimeoutException e) {
					bundle = null;
				} catch (JALReceiveException e) {
					this.subscriptionProtocol.getHandler().onBrokenConnection();
				}
				
				if (bundle != null) {
					String responseString = new String(bundle.getData(), StandardCharsets.UTF_8);
					
					SubscriptionProtocol.onMessage(responseString, this.subscriptionProtocol.getHandler());
				}
			}
		}
		
	}
	
}
