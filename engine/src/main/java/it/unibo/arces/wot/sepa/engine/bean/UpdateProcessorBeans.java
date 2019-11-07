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

package it.unibo.arces.wot.sepa.engine.bean;

public class UpdateProcessorBeans {

	private static long requests = 0;
	private static float min = -1;
	private static float average = -1;
	private static float max = -1;
	private static float current = -1;

	private static long timeout = 5000;
	private static int nRetry = 3;
	
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
	
	public static void setTimeoutNRetry(int n) {
		nRetry = n;
	}

	public static int getTimeoutNRetry() {
		return nRetry;
	}
}
