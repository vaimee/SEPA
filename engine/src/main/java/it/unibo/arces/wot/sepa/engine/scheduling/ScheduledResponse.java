package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.response.Response;

public class ScheduledResponse {
	private int token = -1;
	private Response response = null;
	
	public ScheduledResponse(int token,Response response) {
		this.token = token;
		this.response = response;
	}
	
	public Response getResponse() {
		return response;
	}
	
	public int getToken() {
		return token;
	}
	
	@Override
	public String toString() {
		return "RESPONSE #"+token+" : "+response.toString();
	}
}
