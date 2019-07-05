package it.unibo.arces.wot.sepa.apps.mqtt.mappers;

import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

/**
 * Guaspari soil moisture sensors
 * 
 * application/1/device/1bc0f73caf72d467/rx
 * {"applicationID":"1","applicationName":"Guaspari","deviceName":"EndNode1002","devEUI":"1bc0f73caf72d467","txInfo":{"frequency":916400000,"dr":4},"adr":false,"fCnt":4119,"fPort":1,"data":"U3wxOTA1MTMwNzEwfEl8MTAwMnxIMXwzMzh8SDJ8MzMxfEgzfDMzMQ=="}
 * application/1/device/754366e02ff23515/rx
 * {"applicationID":"1","applicationName":"Guaspari","deviceName":"EndNode1001","devEUI":"754366e02ff23515","txInfo":{"frequency":915200000,"dr":4},"adr":false,"fCnt":3338,"fPort":1,"data":"U3wxOTA2MTIxMzMwfEl8MTAwMXxIMXwzMzZ8SDJ8MzMzfEgzfDMzMg=="}
 * 
 * mosquitto_sub --host 177.104.61.17 --port 1883 --verbose --topic '#'
 * 
 * 1) 10 mins ==> moisture S|1905130630|I|1002|H1|338|H2|331|H3|331
 * S|1905270910|I|1001|H1|338|H2|334|H3|333
 * 
 * 2) 1 hour ==> temperature S|timestamp|I|<ID>|T0|<hw temperature>|
 * 
 * S|1905270900|I|1001|T0|358|B|4094
 * 
 * mV ==> soil moisture °C ==> temperature
 * 
 * {"applicationID":"1","applicationName":"Guaspari","deviceName":"EndNode1002","devEUI":"1bc0f73caf72d467","txInfo":{"frequency":916400000,"dr":4},"adr":false,"fCnt":8836,"fPort":1,"data":"U3wxOTA2MjQwMDIwfEl8MTAwMnxIMXwzMzh8SDJ8MzMzfEgzfDMzMQ=="}
 */
public class GuaspariMapper extends MqttMapper {

	public GuaspariMapper(JSAP appProfile, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(appProfile, sm, "mqtt:GuaspariMapper");
	}
	
	@Override
	protected ArrayList<String[]> map(String topic, String value) {
		ArrayList<String[]> ret = new ArrayList<String[]>();

		JsonObject json = new JsonParser().parse(value).getAsJsonObject();

		// Topic
		String newTopic = topic + "/" + json.get("deviceName").getAsString();

		// Payload
		byte[] decoded = Base64.getDecoder().decode(json.get("data").getAsString());
		String payload = new String(decoded);

		for (Pattern p : patterns) {
			Matcher m = p.matcher(payload);

			if (!m.matches())
				continue;

			int i = 1;

			try {
				while (m.group("value" + i) != null) {
					String observation = topic2observation.get(newTopic + "/" + m.group("id" + i));
					if (observation == null) {
						logger.warn("Topic NOT found: " + newTopic + "/" + m.group("id" + i));
					} else {
						String newValue = m.group("value" + i);
						ret.add(new String[] { observation, newValue });
					}
					i++;
				}
			} catch (IllegalArgumentException e1) {
				logger.debug("No more values: " + e1.getMessage());
			}
		}

		return ret;
	}

}
