package it.unibo.arces.wot.sepa.engine.bean;

import java.util.HashMap;

import org.apache.http.nio.protocol.HttpAsyncExchange;

import it.unibo.arces.wot.sepa.timing.Timings;

public class HTTPHandlerBeans {
	private long requests = 0;

	private long timeoutRequests = 0;
	private long CORSFailedRequests = 0;
	private long parsingFailedRequests = 0;
	private long validatingFailedRequests = 0;
	private long authorizingFailedRequests = 0;
	
	private long requestHandlingTime = -1;
	private float requestHandlingAverageTime = -1;
	private long requestHandlingMinTime = -1;
	private long requestHandlingMaxTime = -1;
	private float handledRequests = 0;

	private HashMap<HttpAsyncExchange,Long> timings = new HashMap<HttpAsyncExchange,Long>();
	
	public void reset() {
		 requests = 0;

		 timeoutRequests = 0;
		 CORSFailedRequests = 0;
		 parsingFailedRequests = 0;
		 validatingFailedRequests = 0;
		 authorizingFailedRequests = 0;
		
		 requestHandlingTime = -1;
		 requestHandlingAverageTime = -1;
		 requestHandlingMinTime = -1;
		 requestHandlingMaxTime = -1;
		 handledRequests = 0;
	}

	public long start(HttpAsyncExchange handler) {
		requests++;
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

	public long getHandlingTime() {
		return requestHandlingTime;
	}

	public long getHandlingMinTime() {
		return requestHandlingMinTime;
	}

	public float getHandlingAvgTime() {
		return requestHandlingAverageTime;
	}

	public long getHandlingMaxTime_ms() {
		return requestHandlingMaxTime;
	}
	
	public long getRequests() {
		return requests;
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
	
	public long getErrors_ValidatingFailed() {
		return validatingFailedRequests;
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
	
	public void validatingFailed() {
		validatingFailedRequests++;
	}
	
	public void authorizingFailed() {
		authorizingFailedRequests++;
	}
}
