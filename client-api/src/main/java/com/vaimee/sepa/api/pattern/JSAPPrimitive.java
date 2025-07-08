package com.vaimee.sepa.api.pattern;

import java.util.HashMap;

import com.vaimee.sepa.api.commons.properties.GraphsProperties;
import com.vaimee.sepa.api.commons.properties.SPARQL11ProtocolProperties;

public abstract class JSAPPrimitive {
	public String sparql = null;
	public HashMap<String, ForcedBinding> forcedBindings = null;
	public SPARQL11ProtocolProperties sparql11protocol = null;
	public GraphsProperties graphs = null;
	
	public static class ForcedBinding {
		public String type = null;
		public String datatype = null;
		public String value = null;
		public String language = null;
	}
}
