package it.unibo.arces.wot.sepa.engine.bean;

import java.time.Instant;

public class SPUManagerBeans {
	private static int requests = 0;
	//private static int totalRequests = 0;

	private static float minTime = -1;
	private static float averageTime = -1;
	private static float maxTime = -1;
	private static float time = -1;

	private static long activeSPUs = 0;
	private static long maxActiveSPUs = 0;

	public static String getActiveSPUs() {
		return String.format("%d [Max: %d]", activeSPUs, maxActiveSPUs);
	}

	public static String getTimings(){
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", time,minTime,averageTime,maxTime);
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
}
