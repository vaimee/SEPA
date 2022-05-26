package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.ArrayList;
import java.util.HashMap;


public class FakeLUTT extends LUTT {
	


	public FakeLUTT() {
	}

	public FakeLUTT(ArrayList<LUTTTriple> jollyTriples,HashMap<String, ArrayList<LUTTTriple>> quads) {
	
	}
	
	@Override
	public HashMap<String, ArrayList<LUTTTriple>> getLutt() {
		return new HashMap<String, ArrayList<LUTTTriple>>();
	}
	
	@Override
	public ArrayList<LUTTTriple> getJollyGraph() {
		return new ArrayList<LUTTTriple>();
	}
	
	@Override
	public boolean hit(LUTT hitter) {
		return true;
	}
	

}
