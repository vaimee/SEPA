package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.ArrayList;
import java.util.HashMap;


public class LUTT {
	
	private HashMap<String,ArrayList<LUTTTriple>> lutt;
	private ArrayList<LUTTTriple> jolly_graph;

	public LUTT() {
		this.lutt = new HashMap<String,ArrayList<LUTTTriple>>();
		this.jolly_graph = new ArrayList<LUTTTriple>();
	}

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
	
	public boolean hit(LUTT hitter) {
		
		//this		--> 1
		//hitter	-->	2
		
		if(jolly_graph.size()>0) {
			//[JOLLY GRAPH]1 vs [JOLLY GRAPH]2
			if(hitter.jolly_graph.size()>0) {
				for (LUTTTriple luttTriple : hitter.jolly_graph) {
					if(jolly_graph.contains(luttTriple)) {
						return true;
					}
				}
			}
			//[JOLLY GRAPH]1 vs [GRAPHS]2
			for (String graph : hitter.lutt.keySet()) {
				ArrayList<LUTTTriple> hitterList = hitter.lutt.get(graph);
				for (LUTTTriple luttTriple : hitterList) {
					if(jolly_graph.contains(luttTriple)) {
						return true;
					}
				}
			}
		}
		//[JOLLY GRAPH]2 vs [GRAPHS]1
		if(hitter.jolly_graph.size()>0) {
			for (String graph : lutt.keySet()) {
				ArrayList<LUTTTriple> hitterList = lutt.get(graph);
				for (LUTTTriple luttTriple : hitterList) {
					if(hitter.jolly_graph.contains(luttTriple)) {
						return true;
					}
				}
			}
		}
		//then looking for quads-> [GRAPHS]1 vs [GRAPHS]2
		for (String graph : lutt.keySet()) {
			if(hitter.lutt.containsKey(graph)) {
				ArrayList<LUTTTriple> hitterTriples =hitter.lutt.get(graph);
				ArrayList<LUTTTriple> triples = lutt.get(graph);
				for (LUTTTriple luttTriple : triples) {
					if(hitterTriples.contains(luttTriple)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	

}
