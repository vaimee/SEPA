package it.unibo.arces.wot.sepa.apps.mqtt.mappers;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public abstract class MqttMapper extends GenericClient implements ISubscriptionHandler {
	protected static final Logger logger = LogManager.getLogger();

	// Topics mapping
	protected HashMap<String, String> topic2observation = new HashMap<String, String>();
	protected final HashMap<String,String> aliases = new HashMap<String,String>();
	
	protected ArrayList<String> registeredTopics = new ArrayList<String>();
	protected ArrayList<String> topics = new ArrayList<String>();
	
	protected final String mapperUri;
	
	protected ArrayList<Pattern> patterns = new ArrayList<Pattern>();
	protected HashMap<String, Pattern> patternsMap = new HashMap<String,Pattern>();
	
	public MqttMapper(JSAP appProfile, SEPASecurityManager sm,String uri)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(appProfile, sm);
		
		subscribe("MQTT_MAPPINGS", null,null, this, 5000,"mappings");
		subscribe("MQTT_MESSAGES", null, null,this, 5000,"message");
		subscribe("MQTT_MAPPERS_TOPICS", null, null,this, 5000,"registeredTopics");
		
		mapperUri = uri;
		
		if (mapperUri != null) {
			Bindings fb = new Bindings();
			fb.addBinding("mapper", new RDFTermURI(mapperUri));
			subscribe("MQTT_MAPPER", null, fb,this, 5000,"mapper");	
		}
	}
	
	protected abstract ArrayList<String[]> map(String topic,String value);
	
	private ArrayList<String[]> mapInternal(String topic,String value) {
		
		if (mapperUri == null) {
			if (registeredTopics.contains(topic)) return null;
		}
		else {
			if (!topics.contains(topic)) return null;
		}
		
		return map(topic, value);
	}
	
	@Override
	public final void onSemanticEvent(Notification notify) {
		ARBindingsResults results = notify.getARBindingsResults();

		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();
	
		if (aliases.get(notify.getSpuid()).equals("message")) {
			for (Bindings bindings : added.getBindings()) {
				// ?topic ?value ?broker
				String topic = bindings.getValue("topic");
				String value = bindings.getValue("value");

				if (value == null || topic == null)
					continue;
				if (value.equals("NaN"))
					continue;

				ArrayList<String[]> observations = mapInternal(topic, value);
				
				if (observations == null) continue;

				for (String[] observation : observations) {
					Bindings fb = new Bindings();
					fb.addBinding("observation", new RDFTermURI(observation[0]));
					fb.addBinding("value", new RDFTermLiteral(observation[1],
							appProfile.getUpdateBindings("UPDATE_OBSERVATION_VALUE").getDatatype("value")));
					
					OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);			
					fb.addBinding("timestamp", new RDFTermLiteral(utc.toString()));
					
					try {
						update("UPDATE_OBSERVATION_VALUE", fb, 5000);
					} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException | SEPAProtocolException e) {
						logger.error(e.getMessage());
					}
				}
			}	
		}
		else if (aliases.get(notify.getSpuid()).equals("mappings")) {
			for (Bindings bindings : removed.getBindings()) {
				topic2observation.remove(bindings.getValue("topic"));
			}
			for (Bindings bindings : added.getBindings()) {
				topic2observation.put(bindings.getValue("topic"), bindings.getValue("observation"));
			}	
		}
		else if (aliases.get(notify.getSpuid()).equals("registeredTopics")) {
			for (Bindings bindings : removed.getBindings()) {
				registeredTopics.remove(bindings.getValue("topic"));
			}
			
			for (Bindings bindings : added.getBindings()) {
				if (registeredTopics.contains(bindings.getValue("topic"))) continue;
				registeredTopics.add(bindings.getValue("topic"));
			}
		
		}
		else if (aliases.get(notify.getSpuid()).equals("mapper")) {
			for (Bindings bindings : removed.getBindings()) {
				topics.remove(bindings.getValue("topic"));
				
				if (bindings.getValue("regex") != null) {
					patterns.remove(patternsMap.get(bindings.getValue("regex")));
					patternsMap.remove(bindings.getValue("regex"));
				}
			}
			
			for (Bindings bindings : added.getBindings()) {
				if (topics.contains(bindings.getValue("topic"))) continue;
				
				topics.add(bindings.getValue("topic"));
				if (bindings.getValue("regex") != null) {
					Pattern p = Pattern.compile(bindings.getValue("regex"));
					patterns.add(p);
					patternsMap.put(bindings.getValue("regex"),p);	
				}
			}
		}
	}

	@Override
	public void onBrokenConnection() {
		logger.error("Broken socket!");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.info("Subscribed. SPUID: "+spuid+ " alias: "+alias);
		if (alias != null) aliases.put(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.info("Unsubscribed. SPUID: "+spuid);
	}

}
