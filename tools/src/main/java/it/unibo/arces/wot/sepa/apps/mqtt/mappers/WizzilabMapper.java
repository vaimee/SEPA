package it.unibo.arces.wot.sepa.apps.mqtt.mappers;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

/**
 
 Topics:
 - /applink/3BC51892/report/107/001BC50C700009CD
 - /applink/3BC51892/report/107/001BC50C700009BB
 
 Values:
 - Magnetic field (mgauss): "msg":{"mag_x":397,"mag_y":-743,"mag_z":232}
 - Accelerometer (mg): "msg":{"acc_x":397,"acc_y":-743,"acc_z":232}
 - Temperature (Celsius): "msg":{"tem1_val":2673}}
 - Temperature (Fahrenheit): "msg":{"tem2_val":7178} 
 - Pressure (mbar/100): "msg":{"pre_val":101365}
 - Humidity (%/100) : "msg":{"hum_val":5550}}


 */
public class WizzilabMapper extends MqttMapper {

	public WizzilabMapper(JSAP appProfile, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(appProfile, sm, "mqtt:WizzilabMapper");
	}

	@Override
	protected ArrayList<String[]> map(String topic, String value) {
		ArrayList<String[]> ret = new ArrayList<String[]>();

		JsonObject json = new JsonParser().parse(value).getAsJsonObject();

		if (json.has("msg")) {
			JsonObject obj = json.getAsJsonObject("msg");
			if (obj.has("tem1_val")) {
				topic += "/temperature";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					float n = obj.get("tem1_val").getAsFloat() / 100;
					String newValue = String.format("%.2f", n);
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("hum_val")) {
				topic += "/humidity";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					float n = obj.get("hum_val").getAsFloat() / 100;
					String newValue = String.format("%.2f", n);
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("pre_val")) {
				topic += "/pressure";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					float n = obj.get("pre_val").getAsFloat() / 100;
					String newValue = String.format("%.2f", n);
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("acc_x")) {
				topic += "/accX";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("acc_x").getAsInt());
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("acc_y")) {	
				topic += "/accY";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("acc_y").getAsInt());
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("acc_z")) {
				topic += "/accZ";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("acc_z").getAsInt());
					ret.add(new String[] { observation, newValue });
				}	
			}
			
			if (obj.has("mag_x")) {
				topic += "/magX";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("mag_x").getAsInt());
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("mag_y")) {	
				topic += "/magY";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("mag_y").getAsInt());
					ret.add(new String[] { observation, newValue });
				}
			}
			
			if (obj.has("mag_z")) {
				topic += "/magZ";
				String observation = topic2observation.get(topic);
				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
				} else {
					String newValue = String.format("%d", obj.get("mag_z").getAsInt());
					ret.add(new String[] { observation, newValue });
				}	
			}
		}

		return ret;
	}

}
