package it.unibo.arces.wot.sepa.engine.bean;

import java.time.Duration;
import java.util.Date;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

public class EngineBeans {
	private Date startDate = null;
	private EngineProperties properties = null;
	
	public EngineBeans(EngineProperties properties) {
		startDate = new Date();
		this.properties = properties;
	}
	public String getUpTime() {
		return startDate.toString() + " " + Duration.between(startDate.toInstant(), new Date().toInstant()).toString();
	}
	
	public String getProperties() {
		return properties.toString();
	}
}
