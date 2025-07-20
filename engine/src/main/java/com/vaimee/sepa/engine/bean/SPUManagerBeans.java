/* This class belongs to the JMX classes used for the remote monitoring of the engine
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.engine.bean;

public class SPUManagerBeans {
	private static long[] updateRequests = {0,0};
	private static float[] minTime = {-1,-1};
	private static float[] averageTime = {-1,-1};
	private static float[] maxTime = {-1,-1};
	private static float[] time = {-1,-1};
	private static long activeSPUs = 0;
	private static long maxActiveSPUs = 0;
	private static long subscribeRequests = 0;
	private static long unsubscribeRequests = 0;
	private static long SPUProcessingTimeout = 5000;
	private static long unitScale = 1000000;
	private static int subscribers = 0;
	private static long subscribers_max = 0;
	private static long filteringRequests = 0;
	private static float filteringTime;
	private static float filteringMinTime;
	private static float filteringMaxTime;
	private static float filteringAverageTime;
	private static long preProcessingExceptions;
	private static long postProcessingExceptions;
	private static long notifyExceptions;
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
	public static long getPostProcessingUpdateRequests() {
		return updateRequests[1];
	}
	public static long getPreProcessingUpdateRequests() {
		return updateRequests[0];
	}
	public synchronized static long getSPUs_current() {
		return activeSPUs;
	}
	public static long getSPUs_max() {
		return maxActiveSPUs;
	}
	public synchronized static  void setActiveSPUs(long n) {
		activeSPUs = n;
		if (activeSPUs > maxActiveSPUs) maxActiveSPUs = activeSPUs;
	}
	public static void subscribeRequest() {
		subscribeRequests++;
	}
	public static void unsubscribeRequest() {
		unsubscribeRequests++;
	}
	public static void preProcessingException() {
		preProcessingExceptions++;
	}
	public static void postProcessingException() {
		postProcessingExceptions++;
	}
	public static void notifyException() {
		notifyExceptions++;
	}
	public synchronized static void preProcessingTimings(long start, long stop) {
		updateRequests[0]++;
		time[0] = stop - start;

		if (minTime[0] == -1)
			minTime[0] = time[0];
		else if (time[0] < minTime[0])
			minTime[0] = time[0];

		if (maxTime[0] == -1)
			maxTime[0] = time[0];
		else if (time[0] > maxTime[0])
			maxTime[0] = time[0];

		if (averageTime[0] == -1)
			averageTime[0] = time[0];
		else
			averageTime[0] = ((averageTime[0] * (updateRequests[0] - 1)) + time[0]) / updateRequests[0];
	}
	public synchronized static void postProcessingTimings(long start, long stop) {
		updateRequests[1]++;
		time[1] = stop - start;

		if (minTime[1] == -1)
			minTime[1] = time[1];
		else if (time[1] < minTime[1])
			minTime[1] = time[1];

		if (maxTime[1] == -1)
			maxTime[1] = time[1];
		else if (time[1] > maxTime[1])
			maxTime[1] = time[1];

		if (averageTime[1] == -1)
			averageTime[1] = time[1];
		else
			averageTime[1] = ((averageTime[1] * (updateRequests[1] - 1)) + time[1]) / updateRequests[1];
	}

	public static void reset() {
		updateRequests[0] = 0;
		updateRequests[1] = 0;
		
		minTime[0] = -1;
		minTime[1] = -1;
		averageTime[0] = -1;
		averageTime[1] = -1;
		maxTime[0] = -1;
		maxTime[1] = -1;
		time[0] = -1;
		time[1] = -1;
		
		subscribeRequests = 0;
		unsubscribeRequests = 0;
		subscribers = 0;
		subscribers_max = 0;
		
		filteringTime = -1;
		filteringMinTime = -1;
		filteringMaxTime = -1;
		filteringAverageTime = -1;
		
		maxActiveSPUs = 0;
		
		preProcessingExceptions = 0;
		postProcessingExceptions = 0;
		notifyExceptions = 0;
	}

	public static float getPreProcessing_SPUs_time() {
		return time[0]/unitScale;
	}
	
	public static float getPreProcessing_SPUs_time_min() {
		return minTime[0]/unitScale;
	}

	public static float getPreProcessing_SPUs_time_max() {
		return maxTime[0]/unitScale;
	}

	public static float getPreProcessing_SPUs_time_average() {
		return averageTime[0]/unitScale;
	}
	
	public static float getPostProcessing_SPUs_time() {
		return time[1]/unitScale;
	}
	
	public static float getPostProcessing_SPUs_time_min() {
		return minTime[1]/unitScale;
	}

	public static float getPostProcessing_SPUs_time_max() {
		return maxTime[1]/unitScale;
	}

	public static float getPostProcessing_SPUs_time_average() {
		return averageTime[1]/unitScale;
	}

	public static long getSubscribeRequests() {
		return subscribeRequests;
	}

	public static long getUnsubscribeRequests() {
		return unsubscribeRequests;
	}

	public static long getSPUProcessingTimeout() {
		return SPUProcessingTimeout;
	}
	
	public static void setSPUProcessingTimeout(long t) {
		SPUProcessingTimeout = t;
	}

	public synchronized static void addSubscriber() {
		subscribers++;
		if (subscribers > subscribers_max) subscribers_max = subscribers;
	}
	
	public synchronized static void removeSubscriber() {
		subscribers--;
	}
	
	public synchronized static int getSubscribers() {
		return subscribers;
	}

	public static long getSubscribersMax() {
		return subscribers_max;
	}

	public synchronized static void filteringTimings(long start, long stop) {
		filteringRequests++;
		filteringTime = stop - start;

		if (filteringMinTime == -1)
			filteringMinTime = filteringTime;
		else if (filteringTime < filteringMinTime)
			filteringMinTime = filteringTime;

		if (filteringMaxTime == -1)
			filteringMaxTime = filteringTime;
		else if (filteringTime > filteringMaxTime)
			filteringMaxTime = filteringTime;

		if (filteringAverageTime == -1)
			filteringAverageTime = filteringTime;
		else
			filteringAverageTime = ((filteringAverageTime * (filteringRequests - 1)) + filteringTime) / filteringRequests;	
	}
	
	public static float getFiltering_time() {
		return filteringTime/unitScale;
	}
	
	public static float getFiltering_time_min() {
		return filteringMinTime/unitScale;
	}

	public static float getFiltering_time_max() {
		return filteringMaxTime/unitScale;
	}

	public static float getFiltering_time_average() {
		return filteringAverageTime/unitScale;
	}

	public static long getPreProcessingExceptions() {
		return preProcessingExceptions;
	}

	public static long getPostProcessingExceptions() {
		return postProcessingExceptions;
	}

	public static long getNotifyExceptions() {
		return notifyExceptions;
	}
}
