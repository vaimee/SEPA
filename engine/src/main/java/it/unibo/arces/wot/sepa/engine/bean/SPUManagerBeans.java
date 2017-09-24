package it.unibo.arces.wot.sepa.engine.bean;

import java.time.Instant;

public class SPUManagerBeans {
	private static long requests = 0;

	private static float minTime = -1;
	private static float averageTime = -1;
	private static float maxTime = -1;
	private static float time = -1;

	private static long activeSPUs = 0;
	private static long maxActiveSPUs = 0;

	private static int keepalive = 5000;

	public static long getRequests() {
		return requests;
	}
	
	public static long getSPUs_current() {
		return activeSPUs;
	}

	public static long getSPUs_max() {
		return maxActiveSPUs;
	}
	
	public static float getSPUs_time() {
		return time;
	}
	
	public static String getSPUs_statistics(){
		return String.format("[%.0f %.0f %.0f]", minTime,averageTime,maxTime);
	}
	
	public static  void setActiveSPUs(long n) {
		activeSPUs = n;
		if (activeSPUs > maxActiveSPUs) maxActiveSPUs = activeSPUs;
	}
	
	public static void timings(Instant start, Instant stop) {
		//totalRequests++;
		requests++;
		time = stop.toEpochMilli() - start.toEpochMilli();

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
	}

	public static void setKeepalive(int keepAlivePeriod) {
		keepalive  = keepAlivePeriod;	
	}
	
	public static int getKeepalive(){
		return keepalive;
	}

	public static float getSPUs_time_min() {
		return minTime;
	}

	public static float getSPUs_time_max() {
		return maxTime;
	}

	public static float getSPUs_time_averaae() {
		return averageTime;
	}
}
