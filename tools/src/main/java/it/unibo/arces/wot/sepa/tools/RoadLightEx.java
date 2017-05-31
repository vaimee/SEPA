package it.unibo.arces.wot.sepa.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

public class RoadLightEx extends RoadExperiment {

	public RoadLightEx() throws FileNotFoundException, NoSuchElementException, IOException {
		super();
	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void runExperiment() {
		super.runExperiment();	
	}

	@Override
	public void dataset() {
		roads = new int[]{1,1,1,1};
		roadSizes = new int[]{10,25,50,100};
		super.dataset();	
	}

	@Override
	public void subscribe() {
		//Road subscriptions
		roadSubscriptionRoads = new int[]{1,2,3,4};
		
		//Lamp subscriptions
		lampSubscriptionRoads = new int[][]{{1,1},{2,2},{3,3},{4,4}};
		lampSubscriptionLamps = new int[][]{{1,10},{1,25},{1,50},{1,100}};
		super.subscribe();
	}
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException {
		RoadLightEx benchmark = new RoadLightEx();
		benchmark.run(true,true,5000);
	}

}
