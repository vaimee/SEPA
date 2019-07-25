package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import java.util.UUID;

import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.gates.Gate;

public class Subscriber {
	private final SPU spu;
	private final EventHandler handler;
	
	// Subscriber Identifier
	private final String sid;
	
	private int sequence = 0;
	
	public Subscriber(SPU spu,EventHandler handler) {
		this.spu = spu;
		this.handler = handler;
		
		sid = "sepa://subscription/" + UUID.randomUUID().toString();
	}
	
	public int nextSequence() {
		sequence++;
		return sequence;
	}
	
	public EventHandler getHandler() {
		return handler;
	}
	
	public String getSID() {
		return sid;
	}
	
	public String getGID() {
		return ((Gate)handler).getGID();
	}
	
	public SPU getSPU() {
		return spu;
	}
	
}
