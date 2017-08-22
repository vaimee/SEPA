package it.unibo.arces.wot.sepa.engine.bean;

public class HTTPGateBeans {
	private String queryURL;
	private String updateURL;
	
	private  String registrationURL;
	private  String tokenRequestURL;
	
	public  void setQueryURL(String s) {
		queryURL = s;
	}
	public String getQueryURL() {
		return queryURL;
	}
	
	public  void setUpdateURL(String s) {
		updateURL = s;	
	}
	public String getUpdateURL() {
		return updateURL;
	}
	
	public  void setRegistrationURL(String string) {
		registrationURL = string;
	}
	public String getRegistrationURL() {
		return registrationURL;
	}
	
	public  void setTokenRequestURL(String string) {
		tokenRequestURL = string;
	}
	public String getTokenRequestURL() {
		return tokenRequestURL;
	}
}
