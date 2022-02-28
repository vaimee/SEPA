package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.ArrayList;
import java.util.HashMap;

public class LUTT {
	
	private HashMap<String,ArrayList<LUTTTriple>> lutt;
	private ArrayList<LUTTTriple> jolly_graph;

	
	public LUTT(ArrayList<LUTTTriple> jollyTriples,HashMap<String, ArrayList<LUTTTriple>> quads) {
		this.lutt = quads;
		this.jolly_graph = jollyTriples;
	}

	public HashMap<String, ArrayList<LUTTTriple>> getLutt() {
		return lutt;
	}

	public ArrayList<LUTTTriple> getJollyGraph() {
		return jolly_graph;
	}
	
	
}
