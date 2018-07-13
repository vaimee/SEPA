package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.timing.Timings;

public class WebsocketBeans {
	private long messages = 0;
	private long notAuthorized = 0;
	private long errors = 0;
	private long fragments = 0;

	private long subscribeHandlingTime = -1;
	private float subscribeHandlingAverageTime = -1;
	private long subscribeHandlingMinTime = -1;
	private long subscribeHandlingMaxTime = -1;
	private long handledSubscribes = 0;
	
	private long unsubscribeHandlingTime = -1;
	private float unsubscribeHandlingAverageTime = -1;
	private long unsubscribeHandlingMinTime = -1;
	private long unsubscribeHandlingMaxTime = -1;
	private long handledunsubscribes = 0;
	
	
	public long unsubscribeTimings(long start) {
		handledunsubscribes++;
				
		unsubscribeHandlingTime = Timings.getTime() - start;

		if (unsubscribeHandlingMinTime == -1)
			unsubscribeHandlingMinTime = unsubscribeHandlingTime;
		else if (unsubscribeHandlingTime < unsubscribeHandlingMinTime)
			unsubscribeHandlingMinTime = unsubscribeHandlingTime;
		
		if (unsubscribeHandlingMaxTime == -1)
			unsubscribeHandlingMaxTime = unsubscribeHandlingTime;
		else if (unsubscribeHandlingTime > unsubscribeHandlingMaxTime)
			unsubscribeHandlingMaxTime = unsubscribeHandlingTime;
		
		if (unsubscribeHandlingAverageTime == -1)
			unsubscribeHandlingAverageTime = unsubscribeHandlingTime;
		else
			unsubscribeHandlingAverageTime = ((unsubscribeHandlingAverageTime * (handledunsubscribes - 1)) + unsubscribeHandlingTime)
					/ handledunsubscribes;
		
		return unsubscribeHandlingTime;
	}
	
	public long subscribeTimings(long start) {
		handledSubscribes++;
		
		subscribeHandlingTime = Timings.getTime() - start;

		if (subscribeHandlingMinTime == -1)
			subscribeHandlingMinTime = subscribeHandlingTime;
		else if (subscribeHandlingTime < subscribeHandlingMinTime)
			subscribeHandlingMinTime = subscribeHandlingTime;
		
		if (subscribeHandlingMaxTime == -1)
			subscribeHandlingMaxTime = subscribeHandlingTime;
		else if (subscribeHandlingTime > subscribeHandlingMaxTime)
			subscribeHandlingMaxTime = subscribeHandlingTime;
		
		if (subscribeHandlingAverageTime == -1)
			subscribeHandlingAverageTime = subscribeHandlingTime;
		else
			subscribeHandlingAverageTime = ((subscribeHandlingAverageTime * (handledSubscribes - 1)) + subscribeHandlingTime)
					/ handledSubscribes;
		
		return subscribeHandlingTime;
	}
		
	public void reset() {
		fragments = 0;
		messages = 0;
		errors = 0;
		notAuthorized = 0;
	}

	public long getMessages(){
		return messages;
	}
	
	public long getFragmented(){
		return fragments;
	}
	
	public long getErrors(){
		return errors;
	}
	
	public long getNotAuthorized(){
		return notAuthorized;
	}

	public void onError() {
		errors++;
	}

	public void onFragmentedMessage() {
		fragments++;
	}

	public void onNotAuthorizedRequest() {
		notAuthorized++;
	}
	
	public void onMessage() {
		messages++;
	}
}
