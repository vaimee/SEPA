package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MqttIoTAgent {
	private static final Logger logger = LogManager.getLogger();

	private static Producer client = null;
	private static ObservationLogger logObservation = null;
	private static MqttObservationUpdater observation = null;
	private static ArrayList<MqttAdapter> adapters = new ArrayList<MqttAdapter>();

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("   java MqttIoTAgent.jar <jsap>");
		System.out.println("Options:");
		System.out.println("   -nolog: do not log observations");
	}

	private static boolean doLog(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-nolog"))
				return false;
		return true;
	}

	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException,
			IOException, MqttException, SEPABindingsException {
		if (args.length < 1) {
			printUsage();
			System.exit(-1);
		}

		SEPASecurityManager sm = null;

		JSAP app = new JSAP(args[0]);
		if (app.isSecure())
			sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017", app.getAuthenticationProperties());

		// if (doClear(args)) {
		// // Clear all
		// client = new Producer(app, "DELETE_ALL", sm);
		// client.update();
		// client.close();
		//
		// client = new Producer(app, "DELETE_ALL_MESSAGES", sm);
		// client.update();
		// client.close();
		//
		// client = new Producer(app, "DELETE_ALL_LOGS", sm);
		// client.update();
		// client.close();
		// }

		logger.info("Parse places");
		if (app.getExtendedData().has("places")) {
			JsonObject places = app.getExtendedData().get("places").getAsJsonObject();

			client = new Producer(app, "ADD_PLACE", sm);

			logger.info("Add places");
			for (Entry<String, JsonElement> mapping : places.entrySet()) {
				String place = mapping.getKey();

				String name = mapping.getValue().getAsJsonObject().get("name").getAsString();
				float lat = mapping.getValue().getAsJsonObject().get("lat").getAsFloat();
				float lon = mapping.getValue().getAsJsonObject().get("lon").getAsFloat();

				client.setUpdateBindingValue("place", new RDFTermURI(place));
				client.setUpdateBindingValue("name", new RDFTermLiteral(name));
				client.setUpdateBindingValue("lat", new RDFTermLiteral(String.format("%f", lat)));
				client.setUpdateBindingValue("lon", new RDFTermLiteral(String.format("%f", lon)));
				Response ret = client.update();
				if (ret.isError())
					logger.error(ret);

				if (mapping.getValue().getAsJsonObject().has("childs")) {
					Producer childs = new Producer(app, "LINK_PLACES", sm);
					JsonArray children = mapping.getValue().getAsJsonObject().get("childs").getAsJsonArray();
					for (JsonElement child : children) {
						String contained = child.getAsString();

						childs.setUpdateBindingValue("child", new RDFTermURI(contained));
						childs.setUpdateBindingValue("root", new RDFTermURI(place));
						ret = childs.update();
						if (ret.isError())
							logger.error(ret);
					}
					childs.close();
				}
			}

			client.close();
		}

		logger.info("Parse semantic mappings");
		if (app.getExtendedData().has("semantic-mappings")) {
			JsonObject mappings = app.getExtendedData().get("semantic-mappings").getAsJsonObject();

			client = new Producer(app, "ADD_OBSERVATION", sm);

			logger.info("Add observations");
			for (Entry<String, JsonElement> mapping : mappings.entrySet()) {
				String topic = mapping.getKey();

				String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
				String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
				String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
				String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
				String label = mapping.getValue().getAsJsonObject().get("label").getAsString();

				client.setUpdateBindingValue("observation", new RDFTermURI(observation));
				client.setUpdateBindingValue("comment", new RDFTermLiteral(comment));
				client.setUpdateBindingValue("label", new RDFTermLiteral(label));
				client.setUpdateBindingValue("location", new RDFTermURI(location));
				client.setUpdateBindingValue("unit", new RDFTermURI(unit));
				client.setUpdateBindingValue("topic", new RDFTermLiteral(topic));

				Response ret = client.update();
				if (ret.isError())
					logger.error(ret);
			}

			client.close();
		}

		if (doLog(args)) {
			logger.info("Create observation logger");
			logObservation = new ObservationLogger(app, sm);
			logObservation.subscribe(5000);
		}

		logger.info("Create observation updater");
		observation = new MqttObservationUpdater(app, sm);
		observation.subscribe(5000);

		logger.info("Create adapters");
		if (app.getExtendedData().get("adapters").getAsJsonObject().has("mqtt")) {
			for (JsonElement arg : app.getExtendedData().get("adapters").getAsJsonObject().get("mqtt")
					.getAsJsonArray()) {
				adapters.add(new MqttAdapter(app, sm, arg.getAsJsonObject(), false));
				logger.info("Adapter: "+arg.getAsJsonObject());
			}
		}
		if (app.getExtendedData().get("adapters").getAsJsonObject().has("simulator")) {
			for (JsonElement arg : app.getExtendedData().get("adapters").getAsJsonObject().get("simulator")
					.getAsJsonArray()) {
				adapters.add(new MqttAdapter(app, sm, arg.getAsJsonObject(), true));
				logger.info("Simulator: "+arg.getAsJsonObject());
			}
		}

		logger.info("Started");
		logger.info("Press any key to exit...");
		try {
			System.in.read();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		logger.info("Closing...");

		if (observation != null)
			observation.close();
		if (logObservation != null)
			logObservation.close();
		for (MqttAdapter adapter : adapters)
			adapter.close();

		System.exit(1);
	}
}
