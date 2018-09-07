package it.unibo.arces.wot.sepa.engine.bean;

public class UpdateProcessorBeans {

	private static long requests = 0;
	private static float min = -1;
	private static float average = -1;
	private static float max = -1;
	private static float current = -1;

	private static long timeout;

	private static long unitScale = 1000000;

	private static boolean reliable = true;
	
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
		if (unitScale == 1)
			return "ns";
		else if (unitScale == 1000)
			return "us";
		return "ms";
	}

	public synchronized static void timings(long start, long stop) {
		current = stop - start;

		requests++;

		if (min == -1)
			min = current;
		else if (current < min)
			min = current;

		if (max == -1)
			max = current;
		else if (current > max)
			max = current;

		if (average == -1)
			average = current;
		else
			average = ((average * (requests - 1)) + current) / requests;
	}

	public static void reset() {
		requests = 0;

		min = -1;
		average = -1;
		max = -1;
		current = -1;
	}

	public static float getCurrent() {
		return current/unitScale;
	}

	public static float getMax() {
		return max/unitScale;
	}

	public static float getMin() {
		return min/unitScale;
	}

	public static float getAverage() {
		return average/unitScale;
	}

	public static void setTimeout(long t) {
		timeout = t;
	}

	public static long getTimeout() {
		return timeout;
	}

	public static long getRequests() {
		return requests;
	}

	public static void setReilable(boolean updateReliable) {
		reliable = updateReliable;
	}
	
	public static boolean getReilable() {
		return reliable;
	}
}
