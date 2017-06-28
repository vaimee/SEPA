package it.unibo.arces.wot.sepa.framework.discovery;

import java.util.HashSet;

import it.unibo.arces.wot.sepa.framework.Context;

public class Thing extends DiscoveredElement {
	
	private String uri;
	private Context context;
	private HashSet<Property> properties;
	
	public Thing(String uri) {
		this.uri = uri;
	}
	
	public boolean equals(Thing eq) {return this.getURI().equals(eq.getURI());}
	
	public String getURI() {return uri;}
	
	public HashSet<Property> getProperties() {return properties;}
	
	public Context getContext() {return context;}
	
	public void setContext(Context context) {this.context=context;}
	
	public void addProperties(Property property) {properties.add(property);}
}
