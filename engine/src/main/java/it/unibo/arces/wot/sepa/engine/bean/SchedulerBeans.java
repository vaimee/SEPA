package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

public class SchedulerBeans {
	private static long updates = 0;
	private static long queries = 0;
	private static long subscribes = 0;
	private static long unsubscribes = 0;
	
	private static long outOfTokenRequests = 0;
	private static long maxPendingsRequests = 0;
	private static long pendingRequests = 0;
	
	private static long errors = 0;	
	private static long totalRequests = 0;
	private static long totalUpdateRequests = 0;
	private static long totalQueryRequests = 0;
	private static long totalSubscribeRequests = 0;
	private static long totalUnsubscribeRequests = 0;
	
	private static float minUnsubscribeTime = -1;
	private static float minSubscribeTime = -1;
	private static float minUpdateTime = -1;
	private static float minQueryTime = -1;
	
	private static float maxUnsubscribeTime = -1;
	private static float maxSubscribeTime = -1;
	private static float maxQueryTime = -1;
	private static float maxUpdateTime = -1;
	
	private static float meanUnsubscribeTime = -1;
	private static float meanSubscribeTime = -1;
	private static float meanQueryTime = -1;
	private static float meanUpdateTime = -1;
	
	private static float queryTime = -1;
	private static float updateTime = -1;
	private static float subscribeTime = -1;
	private static float unsubscribeTime = -1;
	
	public static String getRequests() {
		return String.format("%d [Updates: %d Queries: %d Subscribes: %d Unsubscribes: %d Errors: %d]", totalRequests,totalUpdateRequests,totalQueryRequests,totalSubscribeRequests,totalUnsubscribeRequests,errors);
	}

	public static String getPendingRequests() {
		return String.format("%d [Max: %d OutOfTokens: %d]", pendingRequests,maxPendingsRequests,outOfTokenRequests);
	}

	public static String getUpdateTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", updateTime,minUpdateTime,meanUpdateTime,maxUpdateTime);
	}

	public static String getQueryTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", queryTime,minQueryTime,meanQueryTime,maxQueryTime);
	}

	public static String getSubscribeTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]",subscribeTime, minSubscribeTime,meanSubscribeTime,maxSubscribeTime);
	}

	public static String getUnsubscribeTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", unsubscribeTime,minUnsubscribeTime,meanUnsubscribeTime,maxUnsubscribeTime);
	}
	
	public static void reset() {
		updates = 0;
		queries = 0;
		subscribes = 0;
		unsubscribes = 0;

		minUnsubscribeTime = -1;
		minSubscribeTime = -1;
		minUpdateTime = -1;
		minQueryTime = -1;

		maxUnsubscribeTime = -1;
		maxSubscribeTime = -1;
		maxQueryTime = -1;
		maxUpdateTime = -1;

		meanUpdateTime = -1;
		meanUnsubscribeTime = -1;
		meanSubscribeTime = -1;
		meanQueryTime = -1;
	}

	public static void updateCounters(ScheduledRequest request) {
		long ms = System.currentTimeMillis() - request.getScheduledTime();
		
		if (request.getRequest().getClass().equals(SubscribeRequest.class)) {
			subscribeTime = ms;
			subscribes++;
			totalSubscribeRequests++;

			if (minSubscribeTime == -1)
				minSubscribeTime = ms;
			else if (ms < minSubscribeTime)
				minSubscribeTime = ms;

			if (maxSubscribeTime == -1)
				maxSubscribeTime = ms;
			else if (ms > maxSubscribeTime)
				maxSubscribeTime = ms;

			if (meanSubscribeTime == -1)
				meanSubscribeTime = ms;
			else {
				meanSubscribeTime = (meanSubscribeTime * (subscribes - 1) + ms) / subscribes;
			}
		} else if (request.getRequest().getClass().equals(QueryRequest.class)) {
			queryTime = ms;
			queries++;
			totalQueryRequests++;

			if (minQueryTime == -1)
				minQueryTime = ms;
			else if (ms < minQueryTime)
				minQueryTime = ms;

			if (maxQueryTime == -1)
				maxQueryTime = ms;
			else if (ms > maxQueryTime)
				maxQueryTime = ms;

			if (meanQueryTime == -1)
				meanQueryTime = ms;
			else {
				meanQueryTime = (meanQueryTime * (queries - 1) + ms) / queries;
			}
		} else if (request.getRequest().getClass().equals(UpdateRequest.class)) {
			updateTime = ms;
			updates++;
			totalUpdateRequests++;

			if (minUpdateTime == -1)
				minUpdateTime = ms;
			else if (ms < minUpdateTime)
				minUpdateTime = ms;

			if (maxUpdateTime == -1)
				maxUpdateTime = ms;
			else if (ms > maxUpdateTime)
				maxUpdateTime = ms;

			if (meanUpdateTime == -1)
				meanUpdateTime = ms;
			else {
				meanUpdateTime = (meanUpdateTime * (updates - 1) + ms) / updates;
			}
		} else if (request.getRequest().getClass().equals(UnsubscribeRequest.class)) {
			unsubscribeTime = ms;
			unsubscribes++;
			totalUnsubscribeRequests++;

			if (minUnsubscribeTime == -1)
				minUnsubscribeTime = ms;
			else if (ms < minUnsubscribeTime)
				minUnsubscribeTime = ms;

			if (maxUnsubscribeTime == -1)
				maxUnsubscribeTime = ms;
			else if (ms > maxUnsubscribeTime)
				maxUnsubscribeTime = ms;

			if (meanUnsubscribeTime == -1)
				meanUnsubscribeTime = ms;
			else {
				meanUnsubscribeTime = (meanUnsubscribeTime * (unsubscribes - 1) + ms) / unsubscribes;
			}
		}
	}

	public static void updateQueueSize(int schedulingQueueSize, int size) {
		pendingRequests= schedulingQueueSize - size;
		if (pendingRequests > maxPendingsRequests)
			maxPendingsRequests = pendingRequests;
		
	}

	public static void newRequest(boolean outOfTokens) {
		totalRequests++;
		if (outOfTokens) outOfTokenRequests++;
		
	}
}
