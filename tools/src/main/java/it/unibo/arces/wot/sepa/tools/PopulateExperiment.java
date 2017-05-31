package it.unibo.arces.wot.sepa.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

public class PopulateExperiment extends SmartLightingBenchmark {
	public PopulateExperiment() throws FileNotFoundException, NoSuchElementException, IOException {
		super();
	}

	protected String tag ="LampExp";
	protected static SmartLightingBenchmark benchmark = null;
	
	//Data set
	protected int roads[] = {100,100,100,10};
	protected int roadSizes[] = {10,25,50,100};
	
	//Road subscriptions
	protected int roadSubscriptionRoads[] = {};
	
	//Lamp subscriptions
	protected int lampSubscriptionRoads[][] ={};
	protected int lampSubscriptionLamps[][] ={};
	
	@Override
	public void reset() {
			
	}

	@Override
	public void runExperiment() {
				
	}

	@Override
	public void dataset() {
		//Data set creation
		int roadIndex = firstRoadIndex;	
		nRoads = 0;
		for (int i=0; i < roads.length; i++) {
			roadIndex = addRoads(roads[i],roadSizes[i],roadIndex);	
			nRoads = nRoads + roads[i];
		}
	}

	@Override
	public void subscribe() {
		
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException {
		benchmark = new PopulateExperiment();
		benchmark.run(true,true,5000);
	}
}
