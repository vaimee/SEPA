package it.unibo.arces.wot.sepa.commons.properties;

import java.util.HashSet;
import java.util.Set;

public class GraphsProperties {
	public Set<String> default_graph_uri = new HashSet<String>();
	public Set<String> named_graph_uri = new HashSet<String>();
	public Set<String> using_graph_uri = new HashSet<String>();
	public Set<String> using_named_graph_uri = new HashSet<String>();
	
	public void merge(GraphsProperties temp) {
		if (temp == null) return;
		
		try {
			default_graph_uri.addAll(temp.default_graph_uri);
		} catch (Exception e) {

		}
		try {
			named_graph_uri.addAll(temp.named_graph_uri);
		} catch (Exception e) {

		}
		try {
			using_graph_uri.addAll(temp.using_graph_uri);
		} catch (Exception e) {

		}
		try {
			using_named_graph_uri.addAll(temp.using_named_graph_uri);
		} catch (Exception e) {

		}
		
	}

}
