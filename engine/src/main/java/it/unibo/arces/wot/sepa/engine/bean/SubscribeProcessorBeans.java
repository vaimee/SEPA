package it.unibo.arces.wot.sepa.engine.bean;

public class SubscribeProcessorBeans {
	private static long requests = 0;

	private static float minTime = -1;
	private static float averageTime = -1;
	private static float maxTime = -1;
	private static float time = -1;

	private static long activeSPUs = 0;
	private static long maxActiveSPUs = 0;

	private static long subscribeRequests;
	private static long unsubscribeRequests;

	private static int SPUProcessingTimeout;

	private static long unitScale = 1000000;
	
	public static void scale_ms() {
		unitScale = 1000000;
	}
	
	public static void scale_us() {
		unitScale = 1000;
	}
	
	public static void scale_ns() {
		unitScale = 1;
	}
	
	public static String getUnitScale() {
		if (unitScale == 1) return "ns";
		else if (unitScale == 1000) return "us";
		return "ms";
	}
	
	public static long getUpdateRequests() {
		return requests;
	}
	
	public static long getSPUs_current() {
		return activeSPUs;
	}

	public static long getSPUs_max() {
		return maxActiveSPUs;
	}
	
	public static  void setActiveSPUs(long n) {
		activeSPUs = n;
		if (activeSPUs > maxActiveSPUs) maxActiveSPUs = activeSPUs;
	}
	
	public static void subscribeRequest() {
		subscribeRequests++;
	}
	
	public static void unsubscribeRequest() {
		unsubscribeRequests++;
	}
	
	public synchronized static void timings(long start, long stop) {
		requests++;
		time = stop - start;

		if (minTime == -1)
			minTime = time;
		else if (time < minTime)
			minTime = time;

		if (maxTime == -1)
			maxTime = time;
		else if (time > maxTime)
			maxTime = time;

		if (averageTime == -1)
			averageTime = time;
		else
			averageTime = ((averageTime * (requests - 1)) + time) / requests;
	}

	public static void reset() {
		requests = 0;
		minTime = -1;
		averageTime = -1;
		maxTime = -1;
		time = -1;
		
		subscribeRequests = 0;
		unsubscribeRequests = 0;
	}

	public static float getSPUs_time() {
		return time/unitScale;
	}
	
	public static float getSPUs_time_min() {
		return minTime/unitScale;
	}

	public static float getSPUs_time_max() {
		return maxTime/unitScale;
	}

	public static float getSPUs_time_averaae() {
		return averageTime/unitScale;
	}

	public static long getSubscribeRequests() {
		return subscribeRequests;
	}

	public static long getUnsubscribeRequests() {
		return unsubscribeRequests;
	}

	public static int getSPUProcessingTimeout() {
		return SPUProcessingTimeout;
	}
	
	public static void setSPUProcessingTimeout(int t) {
		SPUProcessingTimeout = t;
	}
}
