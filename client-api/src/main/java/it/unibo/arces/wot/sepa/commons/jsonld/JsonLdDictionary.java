package it.unibo.arces.wot.sepa.commons.jsonld;

import java.util.HashMap;
import java.util.Set;

import it.unibo.arces.wot.sepa.commons.jsonld.context.JsonLdContext;

/**
 * The JsonLdDictionary is the definition of a dictionary used to contain arbitrary dictionary members which are the result of parsing a JSON Object.
 * */
public class JsonLdDictionary {

	private final HashMap<String,JsonLdContext> map = new HashMap<String,JsonLdContext>();
	
	public JsonLdContext getMember(String term) {		
		return map.get(term);
	}

	public Set<String> getKeys() {
		return map.keySet();
	}

	public void addMember(String key, JsonLdContext value) {
		map.put(key, value);
	}

}
