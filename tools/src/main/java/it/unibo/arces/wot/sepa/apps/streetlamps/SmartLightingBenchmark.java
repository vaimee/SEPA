package it.unibo.arces.wot.sepa.apps.streetlamps;

import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.Producer;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

public abstract class SmartLightingBenchmark {
	// Benchmark definition
	private Vector<RoadPool> roadPoll = new Vector<RoadPool>();
	private Vector<Integer> roadSubscriptions = new Vector<Integer>();
	private Vector<Lamp> lampSubscriptions = new Vector<Lamp>();

	private Vector<RoadSubscription> roadSubs = new Vector<RoadSubscription>();
	private Vector<LampSubscription> lampSubs = new Vector<LampSubscription>();

	private Producer lampUpdater;
	private Producer roadUpdater;

	private static final Logger logger = LogManager.getLogger("SmartLightingBenchmark");

	private int lampNotifyN = 0;
	private int roadNotifyN = 0;

	public abstract void reset();

	public abstract void runExperiment();

	public abstract void dataset();

	public abstract void subscribe();

	// Data set
	protected int firstRoadIndex = 1;
	protected int nRoads = 0;

	static ApplicationProfile appProfile = null;

	private class RoadPool {
		private final int size;
		private final int number;
		private final int firstIndex;

		public RoadPool(int number, int size, int firstIndex) {
			this.size = size;
			this.number = number;
			this.firstIndex = firstIndex;
		}

		public int getSize() {
			return size;
		}

		public int getNumber() {
			return number;
		}

		public int getFirstIndex() {
			return firstIndex;
		}
	}

	private class Lamp {
		private final int road;
		private final int post;

		public Lamp(int road, int post) {
			this.road = road;
			this.post = post;
		}

		public int getRoad() {
			return road;
		}

		public int getPost() {
			return post;
		}
	}

	public void addLampSubscription(int roadIndex, int lampIndex) {
		lampSubscriptions.addElement(new Lamp(roadIndex, lampIndex));
	}

	public void addRoadSubscription(int roadIndex) {
		roadSubscriptions.addElement(roadIndex);
	}

	public int addRoad(int size, int index) {
		return addRoads(1, size, index);
	}

	public int addRoads(int number, int size, int firstIndex) {
		roadPoll.add(new RoadPool(number, size, firstIndex));
		return firstIndex + number;
	}

	public SmartLightingBenchmark() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		appProfile = new ApplicationProfile("LightingBenchmark.jsap");
		lampUpdater = new Producer(appProfile, "UPDATE_LAMP");
		roadUpdater = new Producer(appProfile, "UPDATE_ROAD");
	}

	private synchronized int incrementLampNotifies() {
		lampNotifyN++;
		return lampNotifyN;
	}

	private synchronized int incrementRoadNotifies() {
		roadNotifyN++;
		return roadNotifyN;
	}

	class LampSubscription extends Consumer implements Runnable {
		private String lampURI = "";
		private boolean running = true;
		private Bindings bindings = new Bindings();
		private Object sync = new Object();

		public LampSubscription(ApplicationProfile appProfile, int roadIndex, int lampIndex)
				throws SEPAProtocolException, SEPASecurityException {
			super(appProfile, "LAMP");
			lampURI = "bench:Lamp_" + roadIndex + "_" + lampIndex;
			bindings.addBinding("lamp", new RDFTermURI(lampURI));
		}

		public boolean subscribe() {
			long startTime = System.nanoTime();
			Response ret;

			ret = super.subscribe(bindings);

			long stopTime = System.nanoTime();
			logger.info("SUBSCRIBE LAMP " + lampURI + " " + (stopTime - startTime));

			return ret.getClass().equals(SubscribeResponse.class);
		}

		public void terminate() {
			synchronized (sync) {
				running = false;
				sync.notify();
			}
		}

		@Override
		public void run() {
			synchronized (sync) {
				running = true;
				while (running) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}

		@Override
		public void onResults(ARBindingsResults results) {
			incrementLampNotifies();
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
		public void onBrokenSocket() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			// TODO Auto-generated method stub
			
		}

	}

	class RoadSubscription extends Consumer implements Runnable {
		private String roadURI = "";
		private boolean running = true;
		Bindings bindings = new Bindings();
		private Object sync = new Object();

		public RoadSubscription(ApplicationProfile appProfile, int index)
				throws SEPAProtocolException, SEPASecurityException {
			super(appProfile, "ROAD");
			roadURI = "bench:Road_" + index;
			bindings.addBinding("?road", new RDFTermURI(roadURI));
		}

		public boolean subscribe() {
			long startTime = System.nanoTime();
			Response ret;

			ret = super.subscribe(bindings);

			long stopTime = System.nanoTime();
			logger.info("SUBSCRIBE ROAD " + roadURI + " " + (stopTime - startTime));

			return ret.getClass().equals(SubscribeResponse.class);
		}

		public void terminate() {
			synchronized (sync) {
				running = false;
				sync.notify();
			}
		}

		@Override
		public void run() {
			synchronized (sync) {
				running = true;
				while (running) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}

		@Override
		public void onResults(ARBindingsResults results) {
			incrementRoadNotifies();
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
		public void onBrokenSocket() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			// TODO Auto-generated method stub
			
		}
	}

	private boolean subscribeLamp(int roadIndex, int postIndex) throws SEPAProtocolException, SEPASecurityException {
		LampSubscription sub = new LampSubscription(appProfile, roadIndex, postIndex);
		new Thread(sub).start();
		lampSubs.add(sub);
		return sub.subscribe();
	}

	private boolean subscribeRoad(int roadIndex) throws SEPAProtocolException, SEPASecurityException {
		RoadSubscription sub = new RoadSubscription(appProfile, roadIndex);
		roadSubs.add(sub);
		new Thread(sub).start();
		return sub.subscribe();
	}

	private int populate(int nRoad, int nPost, int firstRoadIndex) throws SEPAProtocolException, SEPASecurityException {

		Producer road = new Producer(appProfile, "INSERT_ROAD");
		Producer addPost2Road = new Producer(appProfile, "ADD_POST");
		Producer sensor = new Producer(appProfile, "INSERT_SENSOR");
		Producer post = new Producer(appProfile, "INSERT_POST");
		Producer lamp = new Producer(appProfile, "INSERT_LAMP");
		Producer addSensor2post = new Producer(appProfile, "ADD_SENSOR");
		Producer addLamp2post = new Producer(appProfile, "ADD_LAMP");

		logger.debug("Number of roads: " + nRoad + " Posts/road: " + nPost + " First road index: " + firstRoadIndex);

		Bindings bindings = new Bindings();

		// int roadIndex = firstRoadIndex;

		for (int roadIndex = firstRoadIndex; roadIndex < firstRoadIndex + nRoad; roadIndex++) {
			// while (nRoad>0) {

			String roadURI = "bench:Road_" + roadIndex;

			bindings.addBinding("road", new RDFTermURI(roadURI));

			long startTime = System.nanoTime();
			Response ret = road.update(bindings);
			long stopTime = System.nanoTime();
			logger.info("INSERT ROAD " + roadURI + " " + (stopTime - startTime) + " 1");

			if (!ret.getClass().equals(UpdateResponse.class))
				return firstRoadIndex;

			// int postNumber = nPost;

			for (int postIndex = 1; postIndex < nPost + 1; postIndex++) {
				// while(postNumber>0) {
				// URI
				String postURI = "bench:Post_" + roadIndex + "_" + postIndex;
				String lampURI = "bench:Lamp_" + roadIndex + "_" + postIndex;
				String temparatureURI = "bench:Temperature_" + roadIndex + "_" + postIndex;
				String presenceURI = "bench:Presence_" + roadIndex + "_" + postIndex;

				bindings.addBinding("post", new RDFTermURI(postURI));
				bindings.addBinding("lamp", new RDFTermURI(lampURI));

				// New post
				startTime = System.nanoTime();
				ret = post.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT POST " + postURI + " " + (stopTime - startTime) + " 3");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				// Add post to road
				startTime = System.nanoTime();
				ret = addPost2Road.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT POST2ROAD " + postURI + " " + (stopTime - startTime) + " 1");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				// New lamp
				startTime = System.nanoTime();
				ret = lamp.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT LAMP " + lampURI + " " + (stopTime - startTime) + " 4");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				// Add lamp to post
				startTime = System.nanoTime();
				ret = addLamp2post.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT LAMP2POST " + lampURI + " " + (stopTime - startTime) + " 1");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				// New temperature sensor
				bindings.addBinding("sensor", new RDFTermURI(temparatureURI));
				bindings.addBinding("type", new RDFTermURI("bench:TEMPERATURE"));
				bindings.addBinding("unit", new RDFTermURI("bench:CELSIUS"));
				bindings.addBinding("value", new RDFTermLiteral("0"));

				startTime = System.nanoTime();
				ret = sensor.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT SENSOR " + temparatureURI + " " + (stopTime - startTime) + " 5");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				startTime = System.nanoTime();
				ret = addSensor2post.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT SENSOR2POST " + temparatureURI + " " + (stopTime - startTime) + " 1");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				// New presence sensor
				bindings.addBinding("sensor", new RDFTermURI(presenceURI));
				bindings.addBinding("type", new RDFTermURI("bench:PRESENCE"));
				bindings.addBinding("unit", new RDFTermURI("bench:BOOLEAN"));
				bindings.addBinding("value", new RDFTermLiteral("false"));

				startTime = System.nanoTime();
				ret = sensor.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT SENSOR " + presenceURI + " " + (stopTime - startTime) + " 5");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;

				startTime = System.nanoTime();
				ret = addSensor2post.update(bindings);
				stopTime = System.nanoTime();
				logger.info("INSERT SENSOR2POST " + presenceURI + " " + (stopTime - startTime) + " 1");
				if (!ret.getClass().equals(UpdateResponse.class))
					return firstRoadIndex;
			}
		}

		return firstRoadIndex + nRoad;
	}

	protected boolean updateLamp(int nRoad, int nLamp, Integer dimming) {
		String lampURI = "bench:Lamp_" + nRoad + "_" + nLamp;
		Bindings bindings = new Bindings();
		bindings.addBinding("lamp", new RDFTermURI(lampURI));
		bindings.addBinding("dimming", new RDFTermLiteral(dimming.toString()));

		long startTime = System.nanoTime();
		Response ret = lampUpdater.update(bindings);
		long stopTime = System.nanoTime();

		logger.info("UPDATE LAMP " + lampURI + " " + (stopTime - startTime));

		return ret.getClass().equals(UpdateResponse.class);
	}

	protected boolean updateRoad(int nRoad, Integer dimming) {
		String roadURI = "bench:Road_" + nRoad;
		Bindings bindings = new Bindings();
		bindings.addBinding("?road", new RDFTermURI(roadURI));
		bindings.addBinding("?dimming", new RDFTermLiteral(dimming.toString()));

		long startTime = System.nanoTime();
		Response ret = roadUpdater.update(bindings);
		long stopTime = System.nanoTime();

		logger.info("UPDATE ROAD " + roadURI + " " + (stopTime - startTime));

		return ret.getClass().equals(UpdateResponse.class);
	}

	private void load() throws SEPAProtocolException, SEPASecurityException {
		dataset();

		for (RoadPool road : roadPoll) {
			logger.debug("INSERT " + road.getNumber() + "x" + road.getSize() + " roads (" + road.getFirstIndex() + ":"
					+ (road.getFirstIndex() + road.getNumber() - 1) + ")");
			populate(road.getNumber(), road.getSize(), road.getFirstIndex());
		}
	}

	private void waitNotifications(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			logger.debug(e.getMessage());
		}

		for (LampSubscription sub : lampSubs) {

			sub.unsubscribe();

			sub.terminate();
		}
		for (RoadSubscription sub : roadSubs) {

			sub.unsubscribe();

			sub.terminate();
		}
	}

	private void activateSubscriptions() throws SEPAProtocolException, SEPASecurityException{
		subscribe();

		// SLAMP
		for (Lamp lamp : lampSubscriptions)
			subscribeLamp(lamp.getRoad(), lamp.getPost());

		// SROAD
		for (Integer index : roadSubscriptions)
			subscribeRoad(index);
	}

	public void run(boolean load, boolean reset, int delay) throws SEPAProtocolException, SEPASecurityException
			{
		if (load)
			load();
		if (reset)
			reset();
		activateSubscriptions();
		runExperiment();
		waitNotifications(delay);
	}
}
