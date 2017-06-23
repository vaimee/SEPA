package it.unibo.arces.wot.sepa.framework.discovery;

import java.util.HashSet;

public class Things extends DiscoveryContent {
	public class Content {
		private String thing;
		private String type;
		
		public Content(String thing,String type) {
			this.thing = thing;
			this.type = type;
		}
		
		public String getType(){return type;}
		
		public String getThing(){return thing;}
	}
	
	private HashSet<Content> things = new HashSet<Content>();
	
	public void addThing(String thing,String type) {
		things.add(new Content(thing,type));
	}
}
