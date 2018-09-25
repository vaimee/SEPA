package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ResponseListingOfNotification {
	
	
	@SerializedName(value="@context")
	private String context;
	@SerializedName(value="@id")
	private String id;
	private List<String> contains;
	
	public ResponseListingOfNotification(){
		contains = new ArrayList<String>();
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getContains() {
		return contains;
	}

	public void setContains(List<String> contains) {
		this.contains = contains;
	}
	
}
