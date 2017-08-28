package it.unibo.arces.wot.sepa.engine.bean;

public class WebsocketBeans {
	private long messages = 0;
	private long notAuthorized = 0;
	private long errors = 0;
	private long fragments = 0;

	public void reset() {
		fragments = 0;
		messages = 0;
		errors = 0;
		notAuthorized = 0;
	}

	public long getMessages(){
		return messages;
	}
	
	public long getFragmented(){
		return fragments;
	}
	
	public long getErrors(){
		return errors;
	}
	
	public long getNotAuthorized(){
		return notAuthorized;
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
