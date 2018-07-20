package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;

public class SchedulerBeans {

	private static long outOfTokenRequests = 0;
	private static long maxPendingsRequests = 0;
	private static long pendingRequests = 0;

	private static long errors = 0;
	private static long scheduledRequests = 0;
	private static long totalUpdateRequests = 0;
	private static long totalQueryRequests = 0;
	private static long totalSubscribeRequests = 0;
	private static long totalUnsubscribeRequests = 0;
	
	private static int queueSize;

	public static long getErrors() {
		return errors;
	}

	public static long getQueue_Pending() {
		return pendingRequests;
	}

	public static long getQueue_Max() {
		return maxPendingsRequests;
	}

	public static long getQueue_OutOfToken() {
		return outOfTokenRequests;
	}

	public static String getStatistics() {
		return String.format(
				"Updates %d Queries %d Subscribes %d Unsubscribes %d",
				totalUpdateRequests, totalQueryRequests, totalSubscribeRequests, totalUnsubscribeRequests);
	}

	public static void reset() {
		 outOfTokenRequests = 0;
		 maxPendingsRequests = 0;
		 pendingRequests = 0;

		 errors = 0;
		 scheduledRequests = 0;
		 totalUpdateRequests = 0;
		 totalQueryRequests = 0;
		 totalSubscribeRequests = 0;
		 totalUnsubscribeRequests = 0;
	}

	public static void tokenLeft(int size) {
		pendingRequests =  queueSize - size;
		if (pendingRequests > maxPendingsRequests)
			maxPendingsRequests = pendingRequests;

	}

	public static void newRequest(InternalRequest req,boolean scheduled) {	
		if (scheduled)
			scheduledRequests++;
		else
			outOfTokenRequests++;
		
		if (req.isUpdateRequest()) totalUpdateRequests++;
		else if (req.isQueryRequest()) totalQueryRequests++;
		else if (req.isSubscribeRequest()) totalSubscribeRequests++;
		else if (req.isUnsubscribeRequest()) totalUnsubscribeRequests++;
	}

	public static long getScheduledRequests() {
		return scheduledRequests;
	}

	public static void setQueueSize(int schedulingQueueSize) {
		queueSize = schedulingQueueSize;	
	}
	
	public static int getQueueSize() {
		return queueSize;	
	}
}
