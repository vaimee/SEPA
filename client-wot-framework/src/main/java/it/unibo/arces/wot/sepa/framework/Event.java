package it.unibo.arces.wot.sepa.framework;

public class Event {
	private String event;
	private String thing;
	private String timestamp;
	private String value;
	
	public Event(String event,String thing,String timestamp,String value) {
		this.event = event;
		this.thing = thing;
		this.timestamp = timestamp;
		this.value = value;
	}
	
	public Event(String event,String thing,String timestamp) {
		this(event,thing,timestamp,null);
	}
	
	public String getEvent() {
		return event;
	}
	
	public String getThing() {
		return thing;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return "Web Thing: "+thing+" ==> event *"+event+"* @ "+timestamp+ " with value: "+value;
	}
	
	@Override
	public boolean equals(Object e) {
		if (!e.getClass().equals(Event.class)) return false;
		Event e1 = (Event) e ;
		return e1.getEvent().equals(this.getEvent());
	}
}
