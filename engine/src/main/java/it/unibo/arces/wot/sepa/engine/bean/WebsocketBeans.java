package it.unibo.arces.wot.sepa.engine.bean;

public class WebsocketBeans {
	private long messages = 0;
	private long notAuthorized = 0;
	private long errors = 0;
	private long fragments = 0;
	
	private long keepAlive = 0;
	private long defaultKeepAlive = -1;

	public void reset() {
		fragments = 0;
		messages = 0;
		errors = 0;
		keepAlive = defaultKeepAlive;
		notAuthorized = 0;
	}

	public String getRequests(boolean secure) {
		if (!secure)
			return String.format("Messages: %d [Fragmented: %d Errors: %d]", messages,
					fragments, errors);
		else
			return String.format("Messages: %d [Fragmented: %d Errors: %d Not authorized: %d]",
					messages,
					fragments, errors,notAuthorized);
	}

	public void setKeepAlive(long period) {
		keepAlive = period;
		if (defaultKeepAlive == -1)
			defaultKeepAlive = period;
	}

	public long getKeepAlive() {
		return keepAlive;
	}

	public void onError() {
		errors++;
	}

	public void onFragmentedMessage() {
		fragments++;
	}

	public void onNotAuthorizedRequest() {
		notAuthorized++;
	}
	
	public void onMessage() {
		messages++;
	}
}
