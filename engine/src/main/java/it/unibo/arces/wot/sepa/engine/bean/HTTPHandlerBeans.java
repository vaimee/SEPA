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

import java.util.HashMap;

import org.apache.http.nio.protocol.HttpAsyncExchange;

import it.unibo.arces.wot.sepa.timing.Timings;

public class HTTPHandlerBeans {
	private long timeoutRequests = 0;
	
	private long CORSFailedRequests = 0;
	private long parsingFailedRequests = 0;
	private long authorizingFailedRequests = 0;
	
	private long requestHandlingTime = -1;
	private float requestHandlingAverageTime = -1;
	private long requestHandlingMinTime = -1;
	private long requestHandlingMaxTime = -1;
	private long handledRequests = 0;

	private int outOfTokens;
	
	private HashMap<HttpAsyncExchange,Long> timings = new HashMap<HttpAsyncExchange,Long>();
	
	public void reset() {
		 timeoutRequests = 0;
		 
		 CORSFailedRequests = 0;
		 parsingFailedRequests = 0;
		 authorizingFailedRequests = 0;
		
		 requestHandlingTime = -1;
		 requestHandlingAverageTime = -1;
		 requestHandlingMinTime = -1;
		 requestHandlingMaxTime = -1;
		 handledRequests = 0;
		 
		 outOfTokens = 0;
	}

	public synchronized long start(HttpAsyncExchange handler) {
		long start = Timings.getTime();
		timings.put(handler, start );
		return start;
	}
	
	public synchronized long stop(HttpAsyncExchange handler) {
		handledRequests++;
		
		if (timings.get(handler) == null) return 0;
		
		requestHandlingTime = Timings.getTime() - timings.get(handler);
		
		timings.remove(handler);

		if (requestHandlingMinTime == -1)
			requestHandlingMinTime = requestHandlingTime;
		else if (requestHandlingTime < requestHandlingMinTime)
			requestHandlingMinTime = requestHandlingTime;
		
		if (requestHandlingMaxTime == -1)
			requestHandlingMaxTime = requestHandlingTime;
		else if (requestHandlingTime > requestHandlingMaxTime)
			requestHandlingMaxTime = requestHandlingTime;
		
		if (requestHandlingAverageTime == -1)
			requestHandlingAverageTime = requestHandlingTime;
		else
			requestHandlingAverageTime = ((requestHandlingAverageTime * (handledRequests - 1)) + requestHandlingTime)
					/ handledRequests;
		
		return requestHandlingTime;
	}

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
	
	public long getHandlingTime() {
		return requestHandlingTime/unitScale;
	}

	public long getHandlingMinTime() {
		return requestHandlingMinTime/unitScale;
	}

	public float getHandlingAvgTime() {
		return requestHandlingAverageTime/unitScale;
	}

	public long getHandlingMaxTime_ms() {
		return requestHandlingMaxTime/unitScale;
	}
	
	public long getRequests() {
		return handledRequests;
	}

	public long getErrors_Timeout() {
		return timeoutRequests;
	}

	public long getErrors_CORSFailed() {
		return CORSFailedRequests;
	}
	
	public long getErrors_ParsingFailed() {
		return parsingFailedRequests;
	}
	
	public long getErrors_AuthorizingFailed() {
		return authorizingFailedRequests;
	}
	
	public void timeoutRequest() {
		timeoutRequests++;
	}
	
	public void corsFailed() {
		CORSFailedRequests++;
	}
	
	public void parsingFailed() {
		parsingFailedRequests++;
	}
	
	public void authorizingFailed() {
		authorizingFailedRequests++;
	}

	public void outOfTokens() {
		outOfTokens++;
	}
	
	public long getErrors_OutOfTokens() {
		return outOfTokens;
	}
}
