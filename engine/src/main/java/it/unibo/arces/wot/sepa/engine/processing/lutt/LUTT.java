package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.ArrayList;
import java.util.HashMap;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;

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
	
	public boolean hit(LUTT hitter) {
		//first of all looking for jolly
		if(jolly_graph.size()>0) {
			if(hitter.jolly_graph.size()>0) {
				for (LUTTTriple luttTriple : hitter.jolly_graph) {
					if(jolly_graph.contains(luttTriple)) {
						return true;
					}
				}
				for (String graph : hitter.lutt.keySet()) {
					ArrayList<LUTTTriple> hitterList = hitter.lutt.get(graph);
					for (LUTTTriple luttTriple : hitterList) {
						if(jolly_graph.contains(luttTriple)) {
							return true;
						}
					}
				}
			}
		}
		//then looking for quads
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
