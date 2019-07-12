package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

import it.unibo.arces.wot.sepa.apps.mqtt.mappers.*;

public class MqttIotAgent {
	private static final Logger logger = LogManager.getLogger();
	
	private static void clearAll(JSAP app, SEPASecurityManager sm) throws SEPAProtocolException, SEPASecurityException,
			SEPAPropertiesException, SEPABindingsException, IOException {
		// Clear all
		Producer client = null;
		client = new Producer(app, "CLEAR_MQTT_GRAPH", sm);
		client.update();
		client.close();

		client = new Producer(app, "CLEAR_MQTT_MESSAGE_GRAPH", sm);
		client.update();
		client.close();

		client = new Producer(app, "CLEAR_CONTEXT_GRAPH", sm);
		client.update();
		client.close();

		client = new Producer(app, "CLEAR_OBSERVATION_GRAPH", sm);
		client.update();
		client.close();
	}

	private static void clearHistory(JSAP app, SEPASecurityManager sm) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, IOException {
		// Clear all
		Producer client = null;
		client = new Producer(app, "CLEAR_HISTORY_GRAPH", sm);
		client.update();
		client.close();
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("    java -jar MqttIoTAgent.jar");
		System.out.println("Options:");
		System.out.println("   -nolog: do not log observations");
		System.out.println("   -observations: init observations from observations.jsap");
		System.out.println("   -places: init places from places.jsap");
		System.out.println("   -clear: clear all (history excluded)");
		System.out.println("   -clear-history: clear history graph");
		System.out.println("   -adapters: init the adapters from adapters.jsap");
		System.out.println("   -mappings: init the mappings from mappings.jsap");
		System.out.println("   -init: clear and init all");
	}

	private static boolean doLog(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-nolog"))
				return false;
		return true;
	}

	private static boolean init(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-init"))
				return true;
		return false;
	}

	private static boolean initAdapters(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-adapters"))
				return true;
		return false;
	}

	private static boolean initMappings(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-mappings"))
				return true;
		return false;
	}

	private static boolean initObservations(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-observations"))
				return true;
		return false;
	}

	private static boolean initPlaces(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-places"))
				return true;
		return false;
	}

	private static boolean clear(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-clear"))
				return true;
		return false;
	}

	private static boolean clearHistory(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-clear-history"))
				return true;
		return false;
	}

	private static void addAdapters(JSAP app, SEPASecurityManager sm) throws IOException {
		logger.info("Parse adapters");

		app.read("adapters.jsap", true);

		Producer client = null;

		if (app.getExtendedData().has("adapters")) {
			JsonArray adapters = app.getExtendedData().getAsJsonObject("adapters").getAsJsonArray("mqtt");
			for (JsonElement adapter : adapters) {
				try {
					JsonObject adapt = adapter.getAsJsonObject();

					client = new Producer(app, "ADD_MQTT_BROKER", sm);

					String url = adapt.get("url").getAsString();
					String port = adapt.get("port").getAsString();

					String sslProtocol;
					if (adapt.get("sslProtocol") == null)
						sslProtocol = "";
					else
						sslProtocol = adapt.get("sslProtocol").getAsString();

					String caFile;
					if (adapt.get("caFile") == null)
						caFile = "";
					else
						caFile = adapt.get("caFile").getAsString();

					String clientId;
					if (adapt.get("clientId") == null)
						clientId = "";
					else
						clientId = adapt.get("clientId").getAsString();

					String user;
					if (adapt.get("username") == null)
						user = "";
					else
						user = adapt.get("username").getAsString();

					String password;
					if (adapt.get("password") == null)
						password = "";
					else
						password = adapt.get("password").getAsString();

					client.setUpdateBindingValue("url", new RDFTermLiteral(url));
					client.setUpdateBindingValue("port", new RDFTermLiteral(port, "xsd:integer"));

					client.setUpdateBindingValue("sslProtocol", new RDFTermLiteral(sslProtocol, "xsd:string"));
					client.setUpdateBindingValue("caFile", new RDFTermLiteral(caFile, "xsd:string"));

					client.setUpdateBindingValue("clientId", new RDFTermLiteral(clientId, "xsd:string"));

					client.setUpdateBindingValue("username", new RDFTermLiteral(user));
					client.setUpdateBindingValue("password", new RDFTermLiteral(password));

					client.update();
					client.close();

					client = new Producer(app, "ADD_TOPIC_TO_MQTT_BROKER", sm);
					client.setUpdateBindingValue("url", new RDFTermLiteral(url));
					client.setUpdateBindingValue("port", new RDFTermLiteral(port, "xsd:integer"));

					JsonArray topics = adapt.getAsJsonArray("topics");
					for (JsonElement topic : topics) {
						client.setUpdateBindingValue("topic", new RDFTermLiteral(topic.getAsString()));
						client.update();
					}
					client.close();
				} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException
						| SEPABindingsException e) {
					logger.error(e.getMessage());
					client.close();
					continue;
				}
			}
		}
	}

	private static void addMappings(JSAP app, SEPASecurityManager sm) throws FileNotFoundException, IOException {
		logger.info("Parse mappings");

		app.read("mappings.jsap", true);

		Producer client = null;

		if (app.getExtendedData().has("mappings")) {
			JsonObject mappings = app.getExtendedData().getAsJsonObject("mappings");

			try {
				client = new Producer(app, "ADD_MQTT_MAPPING", sm);

				for (Entry<String, JsonElement> topic : mappings.entrySet()) {
					client.setUpdateBindingValue("topic", new RDFTermLiteral(topic.getKey()));
					client.setUpdateBindingValue("observation", new RDFTermURI(topic.getValue().getAsString()));
					client.update();
				}
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException
					| SEPABindingsException e) {
				logger.error(e.getMessage());
				if (client != null)
					client.close();
			}
		}

		if (app.getExtendedData().has("mappers")) {
			JsonObject mappers = app.getExtendedData().getAsJsonObject("mappers");

			try {
				for (Entry<String, JsonElement> mapper : mappers.entrySet()) {
					JsonObject map = mapper.getValue().getAsJsonObject();

					String mapperUriString = mapper.getKey();

					JsonArray regex = null;
					if (map.has("regex")) {
						regex = map.getAsJsonArray("regex");
					}
					JsonArray topics = map.getAsJsonArray("topics");

					client = new Producer(app, "ADD_MQTT_MAPPER", sm);
					client.setUpdateBindingValue("mapper", new RDFTermURI(mapperUriString));
					client.setUpdateBindingValue("topic", new RDFTermLiteral(topics.get(0).getAsString()));
					client.update();
					client.close();

					if (topics.size() > 1) {
						client = new Producer(app, "ADD_TOPIC_TO_MQTT_MAPPER", sm);
						client.setUpdateBindingValue("mapper", new RDFTermURI(mapperUriString));
						for (int i = 1; i < topics.size(); i++) {
							client.setUpdateBindingValue("topic", new RDFTermLiteral(topics.get(i).getAsString()));
							client.update();
						}
						client.close();
					}

					if (regex != null) {
						client = new Producer(app, "ADD_REGEX_TO_MQTT_MAPPER", sm);
						client.setUpdateBindingValue("mapper", new RDFTermURI(mapperUriString));
						for (int i = 0; i < regex.size(); i++) {
							client.setUpdateBindingValue("regex", new RDFTermLiteral(regex.get(i).getAsString()));
							client.update();
						}
						client.close();
					}

				}
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException
					| SEPABindingsException e) {
				logger.error(e.getMessage());
				if (client != null)
					client.close();
			}
		}
	}

	private static void addPlaces(JSAP app, SEPASecurityManager sm) throws FileNotFoundException, IOException {
		logger.info("Parse places");

		app.read("places.jsap", true);

		Producer client = null;

		if (app.getExtendedData().has("places")) {
			JsonObject places = app.getExtendedData().get("places").getAsJsonObject();

			try {
				client = new Producer(app, "ADD_PLACE", sm);
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
				return;
			}

			logger.info("Add places");
			for (Entry<String, JsonElement> mapping : places.entrySet()) {
				String place = mapping.getKey();

				String name = mapping.getValue().getAsJsonObject().get("name").getAsString();
				float lat = mapping.getValue().getAsJsonObject().get("lat").getAsFloat();
				float lon = mapping.getValue().getAsJsonObject().get("lon").getAsFloat();

				try {
					client.setUpdateBindingValue("place", new RDFTermURI(place));
					client.setUpdateBindingValue("name", new RDFTermLiteral(name));
					client.setUpdateBindingValue("lat", new RDFTermLiteral(String.format("%f", lat)));
					client.setUpdateBindingValue("lon", new RDFTermLiteral(String.format("%f", lon)));
				} catch (SEPABindingsException e) {
					logger.error(e.getMessage());
					try {
						client.close();
					} catch (IOException e1) {
						logger.error(e1.getMessage());
					}
					return;
				}

				Response ret = null;
				try {
					ret = client.update();
				} catch (SEPASecurityException | SEPAPropertiesException | SEPAProtocolException | SEPABindingsException e) {
					logger.error(e.getMessage());
					try {
						client.close();
					} catch (IOException e1) {
						logger.error(e1.getMessage());
					}
					return;
				}
				if (ret.isError())
					logger.error(ret);

				if (mapping.getValue().getAsJsonObject().has("childs")) {
					Producer childs;
					try {
						childs = new Producer(app, "LINK_PLACES", sm);
					} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
						logger.error(e.getMessage());
						return;
					}
					JsonArray children = mapping.getValue().getAsJsonObject().get("childs").getAsJsonArray();
					for (JsonElement child : children) {
						String contained = child.getAsString();

						try {
							childs.setUpdateBindingValue("child", new RDFTermURI(contained));
							childs.setUpdateBindingValue("root", new RDFTermURI(place));
							ret = childs.update();
						} catch (SEPABindingsException | SEPASecurityException | SEPAProtocolException
								| SEPAPropertiesException e) {
							try {
								childs.close();
							} catch (IOException e1) {
								logger.error(e1.getMessage());
							}
							logger.error(e.getMessage());
							return;
						}

						if (ret.isError())
							logger.error(ret);
					}
					try {
						childs.close();
					} catch (IOException e) {
						logger.error(e.getMessage());
						return;
					}
				}
			}
		}

		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return;
		}
	}

	private static void addObservations(JSAP app, SEPASecurityManager sm) throws FileNotFoundException, IOException {
		logger.info("Parse observations");

		app.read("observations.jsap", true);

		Producer client = null;

		if (app.getExtendedData().has("observations")) {
			JsonObject mappings = app.getExtendedData().get("observations").getAsJsonObject();

			try {
				client = new Producer(app, "ADD_OBSERVATION", sm);
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
				return;
			}

			logger.info("Add observations");
			for (Entry<String, JsonElement> mapping : mappings.entrySet()) {
				String observation = mapping.getKey();

				String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
				String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
				String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
				String label = mapping.getValue().getAsJsonObject().get("label").getAsString();

				try {
					client.setUpdateBindingValue("observation", new RDFTermURI(observation));
					client.setUpdateBindingValue("comment", new RDFTermLiteral(comment));
					client.setUpdateBindingValue("label", new RDFTermLiteral(label));
					client.setUpdateBindingValue("location", new RDFTermURI(location));
					client.setUpdateBindingValue("unit", new RDFTermURI(unit));
				} catch (SEPABindingsException e) {
					logger.error(e.getMessage());
					try {
						client.close();
					} catch (IOException e1) {
						logger.error(e1.getMessage());
						return;
					}
					return;
				}

				Response ret = null;
				try {
					ret = client.update();
				} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException
						| SEPABindingsException e) {
					logger.error(e.getMessage());
					try {
						client.close();
					} catch (IOException e1) {
						logger.error(e1.getMessage());
						return;
					}
					return;
				}
				if (ret.isError())
					logger.error(ret);
			}

			try {
				client.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return;
			}
		}
	}

	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, IOException {

		printUsage();

		SEPASecurityManager sm = null;

		JSAP app = null;
		try {
			app = new JSAP("base.jsap");
		} catch (SEPAPropertiesException | SEPASecurityException e1) {
			logger.fatal("Exception on reading JSAP: " + e1.getMessage());
			System.exit(-1);
		}

		// Environmental variable for DOCKER
		Map<String, String> env = System.getenv();
		if (env.containsKey("MQTT_IOT_AGENT_SEPA_BROKER_HOST"))
			app.setHost(env.get("MQTT_IOT_AGENT_SEPA_BROKER_HOST"));

		// Clear history
		if (clearHistory(args))
			clearHistory(app, sm);

		// Init all
		if (init(args)) {
			clearAll(app, sm);

			try {
				addPlaces(app, sm);
			} catch (IOException e) {
				logger.warn("Exception on add places " + e.getMessage());
			}
			try {
				addObservations(app, sm);
			} catch (IOException e) {
				logger.warn("Exception on add observations " + e.getMessage());
			}
			try {
				addAdapters(app, sm);
			} catch (IOException e) {
				logger.warn("Exception on add adapters " + e.getMessage());
			}
			try {
				addMappings(app, sm);
			} catch (IOException e) {
				logger.warn("Exception on add topics " + e.getMessage());
			}
		} else {
			if (clear(args))
				clearAll(app, sm);

			if (initPlaces(args))
				try {
					addPlaces(app, sm);
				} catch (IOException e) {
					logger.warn("Exception on add places " + e.getMessage());
				}

			if (initObservations(args))
				try {
					addObservations(app, sm);
				} catch (IOException e) {
					logger.warn("Exception on add observations " + e.getMessage());
				}

			if (initAdapters(args))
				try {
					addAdapters(app, sm);
				} catch (IOException e) {
					logger.warn("Exception on add adapters " + e.getMessage());
				}

			if (initMappings(args))
				try {
					addMappings(app, sm);
				} catch (IOException e) {
					logger.warn("Exception on add topics " + e.getMessage());
				}
		}

		// MQTT Adapter Pool
		MqttAdapterPool agent = null;
		try {
			agent = new MqttAdapterPool(app, sm);
		} catch (SEPAProtocolException | IOException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException e1) {
			logger.fatal("Exception on creating AdapterPool " + e1.getMessage());
			System.exit(-1);
		}

		// History logging
		ObservationLogger logObservation = null;
		if (doLog(args)) {
			logger.info("Historical data logging enabled");
			logger.info("Create observation logger");
			try {
				logObservation = new ObservationLogger(app, sm);
				logObservation.subscribe(5000);
			} catch (SEPASecurityException | SEPAPropertiesException | SEPAProtocolException
					| SEPABindingsException e) {
				logger.fatal("Exception on creating ObservationLogger " + e.getMessage());
				System.exit(-1);
			}
		}

		// Mappers
		logger.info("Create mappers");
		DefaultMapper defaultMapper = null;
		try {
			defaultMapper = new DefaultMapper(app, sm);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException e1) {
			logger.error("Failed to create default mapper " + e1.getMessage());
		}

		GuaspariMapper jsonMapper = null;
		try {
			jsonMapper = new GuaspariMapper(app, sm);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException e1) {
			logger.error("Failed to create json mapper " + e1.getMessage());
		}

		WizzilabMapper wizziMapper = null;
		try {
			wizziMapper = new WizzilabMapper(app, sm);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException e1) {
			logger.error("Failed to create json mapper " + e1.getMessage());
		}

		// Started
		logger.info("Started");
		logger.info("Press any key to exit...");
		try {
			System.in.read();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		logger.info("Closing...");

		try {
			if (agent != null)
				agent.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		try {
			if (defaultMapper != null)
				defaultMapper.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		try {
			if (jsonMapper != null)
				jsonMapper.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		try {
			if (wizziMapper != null)
				wizziMapper.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		try {
			if (logObservation != null)
				logObservation.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		System.exit(1);
	}
}
