package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.timing.Timings;

public class WebsocketBeans {
	private static long messages = 0;
	private static long notAuthorized = 0;
	private static long errors = 0;
	private static long fragments = 0;

	private static long subscribeHandlingTime = -1;
	private static float subscribeHandlingAverageTime = -1;
	private static long subscribeHandlingMinTime = -1;
	private static long subscribeHandlingMaxTime = -1;
	private static long handledSubscribes = 0;
	
	private static long unsubscribeHandlingTime = -1;
	private static float unsubscribeHandlingAverageTime = -1;
	private static long unsubscribeHandlingMinTime = -1;
	private static long unsubscribeHandlingMaxTime = -1;
	private static long handledunsubscribes = 0;
	
	
	public static long unsubscribeTimings(long start) {
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
		
	public static void reset() {
		fragments = 0;
		messages = 0;
		errors = 0;
		notAuthorized = 0;
		
		subscribeHandlingTime = -1;
		subscribeHandlingAverageTime = -1;
		subscribeHandlingMinTime = -1;
		subscribeHandlingMaxTime = -1;
		handledSubscribes = 0;
		
		unsubscribeHandlingTime = -1;
		unsubscribeHandlingAverageTime = -1;
		unsubscribeHandlingMinTime = -1;
		unsubscribeHandlingMaxTime = -1;
		handledunsubscribes = 0;
	}

	public static long getMessages(){
		return messages;
	}
	
	public static long getFragmented(){
		return fragments;
	}
	
	public static long getErrors(){
		return errors;
	}
	
	public static long getNotAuthorized(){
		return notAuthorized;
	}

	public static void onError() {
		errors++;
	}

	public static void onFragmentedMessage() {
		fragments++;
	}

	public static void onNotAuthorizedRequest() {
		notAuthorized++;
	}
	
	public static void onMessage() {
		messages++;
	}
}
