package it.unibo.arces.wot.sepa.apps.mqtt;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class TopicMapper extends Consumer {
	// Topics mapping
	private HashMap<String, String> topic2observation = new HashMap<String, String>();
		
	public TopicMapper(JSAP appProfile, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, "OBSERVATIONS_TOPICS", sm);
	}
	
	public String[] map(String topic, String value) throws Exception {
		// Check if value can be parsed with regex
		// e.g. pepoli:6lowpan:network = | ID: NODO1 | Temperature: 24.60 | Humidity:
		// 35.40 | Pressure: 1016.46
		
		String newValue = value;
		String newTopic = topic;
		
		if (appProfile.getExtendedData().get("regexTopics").getAsJsonObject().get(topic) != null) {
			JsonArray arr = appProfile.getExtendedData().get("regexTopics").getAsJsonObject().get(topic)
					.getAsJsonArray();
			
			for (JsonElement regex : arr) {
				Pattern p = Pattern.compile(regex.getAsString());
				Matcher m = p.matcher(value);
			
				if (m.matches()) {
					newTopic = topic.replace(":", "/");

					for (int i = 1; i < m.groupCount(); i++) {
						if (!m.group(i).equals(m.group("value")))
							newTopic += "/" + m.group(i);
					}

					newValue = m.group("value");
				}
			}
		}

		// Check if value can be parsed with JSON
		// e.g. {"moistureValue":3247, "nodeId":"device3",
		// "timestamp":"2017-11-15T10:00:02.123028089Z"}

		else if (appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic) != null) {
			String idMember = appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic)
					.getAsJsonObject().get("id").getAsString();
			String valueMember = appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic)
					.getAsJsonObject().get("value").getAsString();

			JsonObject json = new JsonParser().parse(value).getAsJsonObject();
			String topicSuffix = json.get(idMember).getAsString();

			newValue = json.get(valueMember).getAsString();
			newTopic = topic + "/" + topicSuffix;
		}

		String observation = topic2observation.get(newTopic);
		
		if (observation == null) return null;
		
		return new String[] {observation,newValue};
		
	}

	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			topic2observation.put(bindings.getValue("topic"), bindings.getValue("observation"));
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			topic2observation.remove(bindings.getValue("topic"));
		}
	}

	@Override
	public void onBrokenConnection() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribe(String spuid) {
		// TODO Auto-generated method stub
		
	}

}
