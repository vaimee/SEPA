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
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MqttIoTAgent extends Consumer {
	
	public MqttIoTAgent(JSAP appProfile, String subscribeID, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, subscribeID, sm);
		// TODO Auto-generated constructor stub
	}

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
		System.out.println("   -observations: insert observations");
		System.out.println("   -places: insert places");
		System.out.println("   -clear: clear observations and places");
	}

	private static boolean doLog(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-nolog"))
				return false;
		return true;
	}

	private static boolean insertObservations(String[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-observations"))
				return true;
		return false;
	}

	private static boolean insertPlaces(String[] args) {
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

	public static void main(String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(-1);
		}

		SEPASecurityManager sm = null;

		JSAP app = null;
		try {
			app = new JSAP(args[0]);
		} catch (SEPAPropertiesException | SEPASecurityException e1) {
			logger.error(e1.getMessage());
			return;
		}
		
		if (app.isSecure())
			try {
				sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017", app.getAuthenticationProperties());
			} catch (SEPASecurityException e1) {
				logger.error(e1.getMessage());
				return;
			}

		if (clear(args)) {
			// Clear all
			try {
				client = new Producer(app, "DELETE_ALL", sm);
				client.update();
				client.close();
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | IOException | SEPABindingsException e) {
				logger.error(e.getMessage());
				return;
			}	
		}

		if (insertPlaces(args)) {
			logger.info("Parse places");
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
						return;
					}
					
					Response ret = null;
					try {
						ret = client.update();
					} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
						logger.error(e.getMessage());
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
							} catch (SEPABindingsException | SEPASecurityException | IOException | SEPAPropertiesException e) {
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

				try {
					client.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
					return;
				}
			}
		}

		if (insertObservations(args)) {
			logger.info("Parse semantic mappings");
			if (app.getExtendedData().has("semantic-mappings")) {
				JsonObject mappings = app.getExtendedData().get("semantic-mappings").getAsJsonObject();

				try {
					client = new Producer(app, "ADD_OBSERVATION", sm);
				} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
					logger.error(e.getMessage());
					return;
				}

				logger.info("Add observations");
				for (Entry<String, JsonElement> mapping : mappings.entrySet()) {
					String topic = mapping.getKey();

					String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
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
						client.setUpdateBindingValue("topic", new RDFTermLiteral(topic));
					} catch (SEPABindingsException e) {
						logger.error(e.getMessage());
						return;
					}
					

					Response ret = null;
					try {
						ret = client.update();
					} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
						logger.error(e.getMessage());
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
		
		// Setup and exit
		if (insertObservations(args) || insertPlaces(args) || clear(args)) System.exit(1);

		if (doLog(args)) {
			logger.info("Historical data logging enabled");
			logger.info("Create observation logger");
			try {
				logObservation = new ObservationLogger(app, sm);
				logObservation.subscribe(5000);
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | IOException | SEPABindingsException e) {
				logger.error(e.getMessage());
				return;
			}
			
		}

		logger.info("Create observation updater");
		try {
			observation = new MqttObservationUpdater(app, sm);
			observation.subscribe(5000);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | IOException
				| SEPABindingsException e1) {
			logger.error(e1.getMessage());
			return;
		}
		

		logger.info("Create adapters");
		if (app.getExtendedData().get("adapters").getAsJsonObject().has("mqtt")) {
			for (JsonElement arg : app.getExtendedData().get("adapters").getAsJsonObject().get("mqtt")
					.getAsJsonArray()) {
				try {
					logger.info("Creating adapter: " + arg.getAsJsonObject());
					adapters.add(new MqttAdapter(app, sm, arg.getAsJsonObject(), false));
				} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | MqttException e) {
					logger.warn(e.getMessage());
				}		
			}
		}
		if (app.getExtendedData().get("adapters").getAsJsonObject().has("simulator")) {
			for (JsonElement arg : app.getExtendedData().get("adapters").getAsJsonObject().get("simulator")
					.getAsJsonArray()) {
				try {
					logger.info("Creating simulator: " + arg.getAsJsonObject());
					adapters.add(new MqttAdapter(app, sm, arg.getAsJsonObject(), true));
				} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | MqttException e) {
					logger.warn(e.getMessage());
				}
				
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
			try {
				observation.close();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		if (logObservation != null)
			try {
				logObservation.close();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		for (MqttAdapter adapter : adapters)
			try {
				adapter.close();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}

		System.exit(1);
	}

	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
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
