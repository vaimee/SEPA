package it.unibo.arces.wot.sepa.engine.bean;

public class HTTPHandlerBeans {
	private long requests = 0;

	private long timeoutRequests = 0;
	private long CORSFailedRequests = 0;
	private long parsingFailedRequests = 0;
	private long validatingFailedRequests = 0;
	private long authorizingFailedRequests = 0;
	
	private float requestHandlingTime = -1;
	private float requestHandlingAverageTime = -1;
	private float requestHandlingMinTime = -1;
	private float requestHandlingMaxTime = -1;
	private float handledRequests = 0;

	public void reset() {
		handledRequests = 0;

		requestHandlingTime = -1;
		requestHandlingAverageTime = -1;
		requestHandlingMinTime = -1;
		requestHandlingMaxTime = -1;
	}

	public void timings(long start_ns, long stop_ns) {

		handledRequests++;
		requestHandlingTime = stop_ns - start_ns;

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

	}

	public float getHandlingTime_ms() {
		return requestHandlingTime / 1000000;
	}

	public float getHandlingMinTime_ms() {
		return requestHandlingMinTime / 1000000;
	}

	public float getHandlingAvgTime_ms() {
		return requestHandlingAverageTime / 1000000;
	}

	public float getHandlingMaxTime_ms() {
		return requestHandlingMaxTime / 1000000;
	}

	public void newRequest() {
		requests++;
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
