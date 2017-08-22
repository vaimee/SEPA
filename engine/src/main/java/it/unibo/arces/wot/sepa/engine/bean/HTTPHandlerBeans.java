package it.unibo.arces.wot.sepa.engine.bean;

public class HTTPHandlerBeans {
	private long timeout = 0;
	private long defaultTimeout = 0;
	private boolean firstTimeoutSetting = true;

	private long requests = 0;
	private long timeoutRequests = 0;

	private float corsTime = -1;
	private float corsAverageTime = -1;
	private float corsMinTime = -1;
	private float corsMaxTime = -1;

	private long parsedRequests = 0;
	private float parsingTime = -1;
	private float parsingAverageTime = -1;
	private float parsingMinTime = -1;
	private float parsingMaxTime = -1;

	private long validatedRequests = 0;
	private float validatingTime = -1;
	private float validatingAverageTime = -1;
	private float validatingMinTime = -1;
	private float validatingMaxTime = -1;

	private long authorizedRequests = 0;
	private float authorizingTime = -1;
	private float authorizingAverageTime = -1;
	private float authorizingMinTime = -1;
	private float authorizingMaxTime = -1;

	private long handledRequests = 0;
	private float requestHandlingTime = -1;
	private float requestHandlingAverageTime = -1;
	private float requestHandlingMinTime = -1;
	private float requestHandlingMaxTime = -1;

	public void reset() {
		timeout = defaultTimeout;

		requests = 0;

		corsTime = -1;
		corsAverageTime = -1;
		corsMinTime = -1;
		corsMaxTime = -1;

		parsedRequests = 0;
		parsingTime = -1;
		parsingAverageTime = -1;
		parsingMinTime = -1;
		parsingMaxTime = -1;

		validatedRequests = 0;
		validatingTime = -1;
		validatingAverageTime = -1;
		validatingMinTime = -1;
		validatingMaxTime = -1;

		authorizedRequests = 0;
		authorizingTime = -1;
		authorizingAverageTime = -1;
		authorizingMinTime = -1;
		authorizingMaxTime = -1;

		handledRequests = 0;
		requestHandlingTime = -1;
		requestHandlingAverageTime = -1;
		requestHandlingMinTime = -1;
		requestHandlingMaxTime = -1;
	}

	public void updateTimings(long start, long cors, long parsing, long validating, long authorizing, long stop) {
		requests++;

		if (parsing != -1) {
			parsedRequests++;
			parsingTime = parsing - cors;

			if (parsingMinTime == -1)
				parsingMinTime = parsingTime;
			else if (parsingTime < parsingMinTime)
				parsingMinTime = parsingTime;
			if (parsingMaxTime == -1)
				parsingMaxTime = parsingTime;
			else if (parsingTime > parsingMaxTime)
				parsingMaxTime = parsingTime;
			if (parsingAverageTime == -1)
				parsingAverageTime = parsingTime;
			else
				parsingAverageTime = ((parsingAverageTime * (parsedRequests - 1)) + parsingTime) / parsedRequests;
		}

		if (validating != -1) {
			validatedRequests++;
			validatingTime = validating - parsing;

			if (validatingMinTime == -1)
				validatingMinTime = validatingTime;
			else if (validatingTime < validatingMinTime)
				validatingMinTime = validatingTime;
			if (validatingMaxTime == -1)
				validatingMaxTime = validatingTime;
			else if (validatingTime > validatingMaxTime)
				validatingMaxTime = validatingTime;
			if (validatingAverageTime == -1)
				validatingAverageTime = validatingTime;
			else
				validatingAverageTime = ((validatingAverageTime * (validatedRequests - 1)) + validatingTime)
						/ validatedRequests;
		}

		if (authorizing != -1) {
			authorizedRequests++;
			authorizingTime = authorizing - validating;

			if (authorizingMinTime == -1)
				authorizingMinTime = authorizingTime;
			else if (authorizingTime < authorizingMinTime)
				authorizingMinTime = authorizingTime;
			if (authorizingMaxTime == -1)
				authorizingMaxTime = authorizingTime;
			else if (authorizingTime > authorizingMaxTime)
				authorizingMaxTime = authorizingTime;
			if (authorizingAverageTime == -1)
				authorizingAverageTime = authorizingTime;
			else
				authorizingAverageTime = ((authorizingAverageTime * (authorizedRequests - 1)) + authorizingTime)
						/ authorizedRequests;
		}

		if (stop != -1) {
			handledRequests++;
			requestHandlingTime = stop - start;

			if (requestHandlingMinTime == -1)
				requestHandlingMinTime = requestHandlingTime;
			else if (requestHandlingTime < requestHandlingMinTime)
				requestHandlingMinTime = requestHandlingTime;
			if (authorizingMaxTime == -1)
				requestHandlingMaxTime = requestHandlingTime;
			else if (requestHandlingTime > requestHandlingMaxTime)
				requestHandlingMaxTime = requestHandlingTime;
			if (requestHandlingAverageTime == -1)
				requestHandlingAverageTime = requestHandlingTime;
			else
				requestHandlingAverageTime = ((requestHandlingAverageTime * (handledRequests - 1))
						+ requestHandlingTime) / handledRequests;
		}

		corsTime = cors - start;

		if (corsMinTime == -1)
			corsMinTime = corsTime;
		else if (corsTime < corsMinTime)
			corsMinTime = corsTime;
		if (corsMaxTime == -1)
			corsMaxTime = corsTime;
		else if (corsTime > corsMaxTime)
			corsMaxTime = corsTime;
		if (corsAverageTime == -1)
			corsAverageTime = corsTime;
		else
			corsAverageTime = ((corsAverageTime * (requests - 1)) + corsTime) / requests;
	}

	public String getCORSTimings() {
		return String.format("%.3f ms [Min: %.3f Avg: %.3f Max: %.3f]", corsTime / 1000000,
				corsMinTime / 1000000, corsAverageTime / 1000000, corsMaxTime / 1000000);
	}

	public String getParsingTimings() {
		return String.format("%.3f ms [Min: %.3f Avg: %.3f Max: %.3f]", parsingTime / 1000000,
				 parsingMinTime / 1000000, parsingAverageTime / 1000000, parsingMaxTime / 1000000);
	}

	public String getValidatingTimings() {
		return String.format("%.3f ms [Min: %.3f Avg: %.3f Max: %.3f]", validatingTime / 1000000,
				validatingMinTime / 1000000, validatingAverageTime / 1000000,
				validatingMaxTime / 1000000);
	}

	public String getAuthorizingTimings() {
		return String.format("%.3f ms [Min: %.3f Avg: %.3f Max: %.3f]", authorizingTime / 1000000,
				authorizingMinTime / 1000000, authorizingAverageTime / 1000000,
				authorizingMaxTime / 1000000);
	}

	public String getHandlingTimings() {
		return String.format("%.3f ms [Min: %.3f Avg: %.3f Max: %.3f]", requestHandlingTime / 1000000,
				requestHandlingMinTime / 1000000, requestHandlingAverageTime / 1000000,
				requestHandlingMaxTime / 1000000);
	}

	public String getRequests() {
		return String.format("%d [Timeouts: %d Parsed: %d Validated: %d Authorized: %d Completed: %d]", requests, timeoutRequests,parsedRequests,validatedRequests,authorizedRequests,handledRequests);
	}

	public void setTimeout(long t) {
		timeout = t;
		if (firstTimeoutSetting) {
			defaultTimeout = t;
			firstTimeoutSetting = false;
		}
	}

	public long getTimeout() {
		return timeout;
	}

	public void timeoutRequest() {
		timeoutRequests++;
	}
}
