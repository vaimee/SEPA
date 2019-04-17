/* This class implements a JSON parser of an JSAP (JSON Application Profile) file
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.pattern;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermBNode;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

/**
 * JSAP file example
 * 
 * <pre>
 * {
 "host" : "...",
 "sparql11protocol": {
 		"host":"...", 	(optional)
		"protocol": "http | https",
		"port": ...,					(optional)
		"query": {
			"path": "...",
			"method": "GET | POST | URL_ENCODED_POST",
			"format": "JSON | XML | CSV"
		},
		"update": {
			"path": "...",
			"method": "POST | URL_ENCODED_POST",
			"format": "JSON | HTML"
		}
	},
  	"sparql11seprotocol": {
  	    "host":"...", 	(optional)
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
			    "host":"...", 	(optional)
				"port": ...,
				"path": "..."
			},
			"wss": {
			     "host":"...", 	(optional)
				"port": ...,
				"path": "..."
			}
		}
	},
	"oauth": {
		"enable" : false,
		"register": "https://localhost:8443/oauth/register",
		"tokenRequest": "https://localhost:8443/oauth/token"
	},
	"graphs": {
		"default-graph-uri ": "...",
		"named-graph-uri": "...",
		"using-graph-uri": "...",
		"using-named-graph-uri": "..."
	},	
	"extended" :{<Application specific extended data>},
	"namespaces" : {
			"chat" : "http://wot.arces.unibo.it/chat#" ,
			"rdf" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#"},
	"updates" : {
			"UPDATE_1" : {
				"sparql" : "..." ,
				"forcedBindings" : {
					"variable_1" : {
						"type" : "literal" ,
						"datatype": "xsd datatype" (optional. Default: xsd:string)
						"value" : "..."} (optional)
					 ,
					"variable_2" : {
						"type" : "uri",
						"value" : "..."}
					 ,
					"variable_N" : {
						"type" : "bNode",
						"value" : "..."
					}
				},
				"sparql11protocol" :{...} (optional)
				"authentication" : {...} (optional)
				"graphs": { (optional)
					"using-graph-uri": "...", (optional)
					"using-named-graph-uri": ..." (optional)
				},
			}
			 ,
			"UPDATE_N" : {
				"sparql" : "..."
			}
		}
		 ,
		"queries" : {
			"QUERY_1" : {
				"sparql" : "..." ,
				"forcedBindings" : {
					"variable_1" : {
						"type" : "literal" ,
						"datatype": "xsd datatype" (optional, default: xsd:string)
						"value" : "..."}	(optional)
					 ,
					"variable_2" : {
						"type" : "uri",
						"value" : "..."} (optional)
					 ,
					"variable_N" : {
						"type" : "bNode",
						"value" : "..."} (optional)
				},
				"sparql11protocol" :{...} (optional),
				"sparql11seprotocol" :{...} (optional)
				"authentication" : {...} (optional)
				"graphs": { (optional)
					"default-graph-uri ": "...", (optional)
					"named-graph-uri": "...", (optional)
				},
			}
			 ,
			"QUERY_N" : {
				"sparql" : "..."
			}
		}
		}}
 * </pre>
 */
public class JSAP extends SPARQL11SEProperties {
	protected static ArrayList<String> numbersOrBoolean = new ArrayList<String>();
	
	protected String prefixes = "";

	public JSAP(String propertiesFile) throws SEPAPropertiesException, SEPASecurityException {
		super(propertiesFile);

		if (!jsap.has("namespaces"))
			jsap.add("namespaces", new JsonObject());

		if (!jsap.get("namespaces").getAsJsonObject().has("rdf"))
			jsap.get("namespaces").getAsJsonObject().add("rdf",
					new JsonPrimitive("http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		if (!jsap.get("namespaces").getAsJsonObject().has("rdfs"))
			jsap.get("namespaces").getAsJsonObject().add("rdfs",
					new JsonPrimitive("http://www.w3.org/2000/01/rdf-schema#"));
		if (!jsap.get("namespaces").getAsJsonObject().has("owl"))
			jsap.get("namespaces").getAsJsonObject().add("owl", new JsonPrimitive("http://www.w3.org/2002/07/owl#"));
		if (!jsap.get("namespaces").getAsJsonObject().has("xsd"))
			jsap.get("namespaces").getAsJsonObject().add("xsd", new JsonPrimitive("http://www.w3.org/2001/XMLSchema#"));

		numbersOrBoolean.add("xsd:integer");
		numbersOrBoolean.add("xsd:decimal");
		numbersOrBoolean.add("xsd:double");
		numbersOrBoolean.add("xsd:boolean");

		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#integer");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#decimal");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#double");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#boolean");
		
		// Prefixes and namespaces
		Set<String> appPrefixes = getPrefixes();
		for (String prefix : appPrefixes) {
			prefixes += "PREFIX " + prefix + ":<" + getNamespaceURI(prefix) + "> ";
		}
	}

	protected Logger logger = LogManager.getLogger();

	public AuthenticationProperties getAuthenticationProperties() {
		try {
			return new AuthenticationProperties(propertiesFile);
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			return null;
		}
	}

	public boolean isSecure() {
		try {
			return jsap.get("oauth").getAsJsonObject().get("enable").getAsBoolean();
		} catch (Exception e) {
			return false;
		}
	}

	public JsonObject getExtendedData() {
		try {
			return jsap.get("extended").getAsJsonObject();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return null;
	}

	/*
	 * UPDATE
	 */
	public String getSPARQLUpdate(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql").getAsString();
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		return null;
	}

	public String getUpdateHost(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("host").getAsString();
		} catch (Exception e) {
			try {
				return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("host").getAsString();
			} catch (Exception e1) {

			}
		}

		return super.getDefaultHost();
	}

	public String getUpdateAcceptHeader(String id) {
		try {
			if (jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("update").getAsJsonObject().get("format").getAsString().equals("JSON"))
				return "application/json";
			else
				return "application/html";
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}

		return super.getUpdateAcceptHeader();
	}

	public HTTPMethod getUpdateMethod(String id) {
		try {
			switch (jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("update").getAsJsonObject().get("method").getAsString()) {
			case "URL_ENCODED_POST":
				return HTTPMethod.URL_ENCODED_POST;
			case "POST":
				return HTTPMethod.POST;
			case "GET":
				// Virtuoso PATCH
				return HTTPMethod.GET;
			}
		} catch (Exception e) {
		}

		return super.getUpdateMethod();
	}

	public String getUpdateProtocolScheme(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("protocol").getAsString();
		} catch (Exception e) {
		}

		return super.getDefaultProtocolScheme();
	}

	public String getUpdatePath(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("update").getAsJsonObject().get("path").getAsString();
		} catch (Exception e) {
		}

		return super.getUpdatePath();
	}

	public int getUpdatePort(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
		}

		return super.getDefaultPort();
	}

	public String getUsingNamedGraphURI(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("graphs").getAsJsonObject()
					.get("using-named-graph-uri").getAsString();
		} catch (Exception e) {
		}

		return super.getUsingNamedGraphURI();
	}

	public String getUsingGraphURI(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("graphs").getAsJsonObject()
					.get("using-graph-uri").getAsString();
		} catch (Exception e) {
		}

		return super.getUsingGraphURI();
	}

	/*
	 * QUERY
	 */
	public String getSPARQLQuery(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql").getAsString();
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		return null;
	}

	public String getQueryHost(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("host").getAsString();
		} catch (Exception e) {
			try {
				return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("host").getAsString();
			} catch (Exception e1) {

			}
		}

		return super.getDefaultHost();
	}

	public String getQueryProtocolScheme(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("protocol").getAsString();
		} catch (Exception e) {
		}

		return super.getDefaultProtocolScheme();
	}

	public int getQueryPort(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
		}

		return super.getDefaultPort();
	}

	public String getQueryPath(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("query").getAsJsonObject().get("path").getAsString();
		} catch (Exception e) {
		}

		return super.getDefaultQueryPath();
	}

	public HTTPMethod getQueryMethod(String id) {
		try {
			switch (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("query").getAsJsonObject().get("method").getAsString()) {
			case "URL_ENCODED_POST":
				return HTTPMethod.URL_ENCODED_POST;
			case "POST":
				return HTTPMethod.POST;
			case "GET":
				return HTTPMethod.GET;
			}
		} catch (Exception e) {
		}

		return super.getUpdateMethod();
	}

	public String getQueryFormat(String id) {
		try {
			switch (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("format").getAsString()) {
			case "JSON":
				return "application/sparql-results+json";
			case "XML":
				return "application/sparql-results+xml";
			case "CSV":
				return "text/csv";
			default:
				return "application/sparql-results+json";
			}
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}

		return super.getQueryAcceptHeader();
	}

	public String getNamedGraphURI(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("graphs").getAsJsonObject()
					.get("named-graph-uri").getAsString();
		} catch (Exception e) {
		}

		return super.getNamedGraphURI();
	}

	public String getDefaultGraphURI(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("graphs").getAsJsonObject()
					.get("default-graph-uri").getAsString();
		} catch (Exception e) {
		}

		return super.getDefaultGraphURI();
	}

	/*
	 * SUBSCRIBE
	 */

	public String getSubscribeHost(String id) {
		String protocol = null;
		try {
			protocol = jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("protocol").getAsString();
		} catch (Exception e) {

			try {
				return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
						.getAsJsonObject().get("host").getAsString();
			} catch (Exception e1) {
				return super.getDefaultHost();
			}
		}

		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("availableProtocols").getAsJsonObject().get(protocol).getAsJsonObject()
					.get("host").getAsString();
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}

		return super.getDefaultHost();
	}

	public int getSubscribePort(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get(jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
							.getAsJsonObject().get("protocol").getAsString())
					.getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
		}

		return super.getSubscribePort();
	}

	public String getSubscribePath(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get(jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
							.getAsJsonObject().get("protocol").getAsString())
					.getAsJsonObject().get("path").getAsString();
		} catch (Exception e) {
		}

		return super.getSubscribePath();
	}

	public SubscriptionProtocol getSubscribeProtocol(String id) {
		try {
			if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("protocol").getAsString().equals("ws"))
				return SubscriptionProtocol.WS;

			if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11seprotocol")
					.getAsJsonObject().get("protocol").getAsString().equals("wss"))
				return SubscriptionProtocol.WSS;
		} catch (Exception e1) {
		}

		return super.getSubscriptionProtocol();
	}

	public Set<String> getUpdateIds() {
		HashSet<String> ret = new HashSet<String>();

		try {
			for (Entry<String, JsonElement> key : jsap.get("updates").getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}

		return ret;
	}

	public Set<String> getQueryIds() {
		HashSet<String> ret = new HashSet<String>();

		try {
			for (Entry<String, JsonElement> key : jsap.get("queries").getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}

		return ret;
	}

	/**
	 * <pre>
	 * "forcedBindings" : {
					"variable_1" : {
						"type" : "literal",
						"datatype" : "xsd:short"}
					 ,
					"variable_2" : {
						"type" : "uri"}
					 ,
					 "variable_3" : {
					 	"type": "literal",
					 	"datatype" : "xsd:string",
					 	"language" : "it"},
					 ...
					"variable_N" : {
						"type" : "literal",
						"datatype" : "xsd:dateTime" ,
						"value" : "1985-08-03T01:02:03Z"}
				}
	 * </pre>
	 */
	public Bindings getUpdateBindings(String id) throws IllegalArgumentException {
		if (!jsap.get("updates").getAsJsonObject().has(id))
			throw new IllegalArgumentException("Update ID not found: " + id);

		Bindings ret = new Bindings();

		if (!jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().has("forcedBindings"))
			return ret;

		try {
			for (Entry<String, JsonElement> binding : jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject()
					.get("forcedBindings").getAsJsonObject().entrySet()) {

				if (!binding.getValue().getAsJsonObject().has("type")) {
					logger.error("JSAP missing binding type: " + binding);
					continue;
				}

				RDFTerm bindingValue = null;
				String value = null;
				if (binding.getValue().getAsJsonObject().has("value"))
					value = binding.getValue().getAsJsonObject().get("value").getAsString();

				switch (binding.getValue().getAsJsonObject().get("type").getAsString()) {
				case "literal":
					String datatype = null;
					if (binding.getValue().getAsJsonObject().has("datatype"))
						datatype = binding.getValue().getAsJsonObject().get("datatype").getAsString();

					String language = null;
					if (binding.getValue().getAsJsonObject().has("language"))
						language = binding.getValue().getAsJsonObject().get("language").getAsString();

					bindingValue = new RDFTermLiteral(value, datatype, language);
					break;
				case "uri":
					bindingValue = new RDFTermURI(value);
					break;
				case "bnode":
					bindingValue = new RDFTermBNode(value);
					break;
				default:
					logger.error("JSAP unknown type: " + binding);
					continue;
				}

				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
		}

		return ret;
	}

	public Bindings getQueryBindings(String id) throws IllegalArgumentException {
		if (!jsap.get("queries").getAsJsonObject().has(id))
			throw new IllegalArgumentException("Query ID not found");

		Bindings ret = new Bindings();

		if (!jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().has("forcedBindings"))
			return ret;

		try {
			for (Entry<String, JsonElement> binding : jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject()
					.get("forcedBindings").getAsJsonObject().entrySet()) {

				RDFTerm bindingValue = null;
				String value = null;
				if (binding.getValue().getAsJsonObject().has("value"))
					value = binding.getValue().getAsJsonObject().get("value").getAsString();

				switch (binding.getValue().getAsJsonObject().get("type").getAsString()) {
				case "literal":
					String datatype = null;
					if (binding.getValue().getAsJsonObject().has("datatype"))
						datatype = binding.getValue().getAsJsonObject().get("datatype").getAsString();

					String language = null;
					if (binding.getValue().getAsJsonObject().has("language"))
						language = binding.getValue().getAsJsonObject().get("language").getAsString();

					bindingValue = new RDFTermLiteral(value, datatype, language);
					break;
				case "uri":
					bindingValue = new RDFTermURI(value);
					break;
				case "bnode":
					bindingValue = new RDFTermBNode(value);
					break;
				default:
					logger.error("JSAP unknown type: " + binding);
					continue;
				}

				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
		}

		return ret;
	}

	/**
	 * <pre>
	 * "namespaces" : { 
	 	"iot":"http://www.arces.unibo.it/iot#",
	 	"rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"},
	 * </pre>
	 */

	public Set<String> getPrefixes() {
		HashSet<String> ret = new HashSet<String>();

		try {
			for (Entry<String, JsonElement> key : jsap.get("namespaces").getAsJsonObject().entrySet())
				ret.add(key.getKey());
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		return ret;
	}

	public String getNamespaceURI(String prefix) {
		try {
			return jsap.get("namespaces").getAsJsonObject().get(prefix).getAsString();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	public String getFileName() {
		return propertiesFile;
	}

	public String printParameters() {
		return jsap.toString();
	}

	public String getUpdateUrl(String id) {
		String port = "";
		if (getUpdatePort(id) != -1)
			port = ":" + getUpdatePort(id);
		return getUpdateProtocolScheme(id) + "://" + getUpdateHost(id) + port + getUpdatePath(id);
	}

	public String getQueryUrl(String id) {
		String port = "";
		if (getQueryPort(id) != -1)
			port = ":" + getQueryPort(id);
		return getQueryProtocolScheme(id) + "://" + getQueryHost(id) + port + getQueryPath(id);
	}

	public String getSubscribeUrl(String id) {
		String scheme = "";
		String port = "";

		SubscriptionProtocol prot = getSubscribeProtocol(id);

		if (prot.equals(SubscriptionProtocol.WS))
			scheme = "ws";
		else if (prot.equals(SubscriptionProtocol.WSS))
			scheme = "wss";

		if (getSubscribePort(id) != -1)
			port = ":" + getSubscribePort(id);

		return scheme + "://" + getSubscribeHost(id) + port + getSubscribePath();
	}

	/*
	 * The grammar of SPARQL 1.1 Update is specified here: https://www.w3.org/TR/sparql11-query/#rUpdate
	 * 
	 * A request MAY include multiple primitives separated by ";" (see Update [29])
	 * 
	 * 
[29]  	Update	  ::=  	Prologue ( Update1 ( ';' Update )? )?
	 * */
	
	public String addPrefixesAndReplaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
		return prefixes + replaceBindings(sparql, bindings);
		
//		if (!sparql.contains(";")) return prefixes + replaceBindings(sparql, bindings);
//		
//		String[] update = sparql.split(";");
//		
//		//TODO ";" may belong to a literal. To be checked.
//		String multipleUpdates = "";
//		for (int i=0; i < update.length ; i++) {
//			if (!update[i].trim().toUpperCase().startsWith("INSERT") && !update[i].trim().toUpperCase().startsWith("DELETE")) continue;
//			if (i != update.length -1 ) multipleUpdates += prefixes + replaceBindings(update[i], bindings) + ";";
//			else multipleUpdates += replaceBindings(update[i], bindings) ;
//		}
//		return multipleUpdates;
	}

	private final String replaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
		if (bindings == null || sparql == null)
			return sparql;

		String replacedSparql = String.format("%s", sparql);
		String selectPattern = "";

		if (sparql.toUpperCase().contains("SELECT")) {
			selectPattern = replacedSparql.substring(0, sparql.indexOf('{'));
			replacedSparql = replacedSparql.substring(sparql.indexOf('{'), replacedSparql.length());
		}
		for (String var : bindings.getVariables()) {
			String value = bindings.getValue(var);
			if (value == null)
				continue;

			/*
			 * 4.1.2 Syntax for Literals
			 * 
			 * The general syntax for literals is a string (enclosed in either double
			 * quotes, "...", or single quotes, '...'), with either an optional language tag
			 * (introduced by @) or an optional datatype IRI or prefixed name (introduced by
			 * ^^).
			 * 
			 * As a convenience, integers can be written directly (without quotation marks
			 * and an explicit datatype IRI) and are interpreted as typed literals of
			 * datatype xsd:integer; decimal numbers for which there is '.' in the number
			 * but no exponent are interpreted as xsd:decimal; and numbers with exponents
			 * are interpreted as xsd:double. Values of type xsd:boolean can also be written
			 * as true or false.
			 * 
			 * To facilitate writing literal values which themselves contain quotation marks
			 * or which are long and contain newline characters, SPARQL provides an
			 * additional quoting construct in which literals are enclosed in three single-
			 * or double-quotation marks.
			 * 
			 * Examples of literal syntax in SPARQL include:
			 * 
			 * - "chat" - 'chat'@fr with language tag "fr" -
			 * "xyz"^^<http://example.org/ns/userDatatype> - "abc"^^appNS:appDataType -
			 * '''The librarian said, "Perhaps you would enjoy 'War and Peace'."''' - 1,
			 * which is the same as "1"^^xsd:integer - 1.3, which is the same as
			 * "1.3"^^xsd:decimal - 1.300, which is the same as "1.300"^^xsd:decimal -
			 * 1.0e6, which is the same as "1.0e6"^^xsd:double - true, which is the same as
			 * "true"^^xsd:boolean - false, which is the same as "false"^^xsd:boolean
			 * 
			 * Tokens matching the productions INTEGER, DECIMAL, DOUBLE and BooleanLiteral
			 * are equivalent to a typed literal with the lexical value of the token and the
			 * corresponding datatype (xsd:integer, xsd:decimal, xsd:double, xsd:boolean).
			 */

			if (bindings.isLiteral(var)) {
				String datatype = bindings.getDatatype(var);
				String lang = bindings.getLanguage(var);

				// Not a number or boolean
				if (!numbersOrBoolean.contains(datatype)) {
					value = "'" + value + "'";

					// Check if datatype is a qname or not
					URI uri = null;
					try {
						uri = new URI(datatype);
					} catch (URISyntaxException e) {
						logger.error(e.getMessage());
					}

					if (uri != null) {
						if (uri.getSchemeSpecificPart().startsWith("/"))
							datatype = "<" + datatype + ">";
					}

					if (lang != null)
						value += "@" + bindings.getLanguage(var);
					else
						value += "^^" + datatype;
				}
			} else if (bindings.isURI(var)) {
				// See https://www.w3.org/TR/rdf-sparql-query/#QSynIRI
				// https://docs.oracle.com/javase/7/docs/api/java/net/URI.html

				// [scheme:]scheme-specific-part[#fragment]
				// An absolute URI specifies a scheme; a URI that is not absolute is said to be
				// relative.
				// URIs are also classified according to whether they are opaque or
				// hierarchical.

				// An opaque URI is an absolute URI whose scheme-specific part does not begin
				// with a slash character ('/').
				// Opaque URIs are not subject to further parsing.

				// A hierarchical URI is either an absolute URI whose scheme-specific part
				// begins with a slash character,
				// or a relative URI, that is, a URI that does not specify a scheme.
				// A hierarchical URI is subject to further parsing according to the syntax
				// [scheme:][//authority][path][?query][#fragment]

				URI uri = null;
				try {
					uri = new URI(value);
				} catch (URISyntaxException e) {
					logger.error(e.getMessage());
				}

				if (uri != null) {
					if (uri.getSchemeSpecificPart().startsWith("/"))
						value = "<" + value + ">";
				}
			} else {
				// A blank node
				logger.trace("Blank node: " + value);
			}
			// Matching variables
			/*
			 * [108] Var ::= VAR1 | VAR2 [143] VAR1 ::= '?' VARNAME [144] VAR2 ::= '$'
			 * VARNAME [164] PN_CHARS_BASE ::= [A-Z] | [a-z] | [#x00C0-#x00D6] |
			 * [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] | [#x037F-#x1FFF] |
			 * [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] |
			 * [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF] [165] PN_CHARS_U ::=
			 * PN_CHARS_BASE | '_' [166] VARNAME ::= ( PN_CHARS_U | [0-9] ) ( PN_CHARS_U |
			 * [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040] )*
			 */
			int start = 0;
			while (start != -1) {
				int index = replacedSparql.indexOf("?" + var, start);
				if (index == -1)
					index = replacedSparql.indexOf("$" + var, start);
				if (index != -1) {
					start = index + 1;
					int unicode = replacedSparql.codePointAt(index + var.length() + 1);
					if (!isValidVarChar(unicode)) {
						replacedSparql = replacedSparql.substring(0, index) + value
								+ replacedSparql.substring(index + var.length() + 1);
					}
				} else
					start = index;
			}

			selectPattern = selectPattern.replace("?" + var, "");
		}

		return selectPattern + replacedSparql;
	}

	private boolean isValidVarChar(int c) {
		return ((c == '_') || (c == 0x00B7) || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040)
				|| ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9')
				|| (0x00C0 <= c && c <= 0x00D6) || (0x00D8 <= c && c <= 0x00F6) || (0x00F8 <= c && c <= 0x02FF)
				|| (0x0370 <= c && c <= 0x037D) || (0x037F <= c && c <= 0x1FFF) || (0x200C <= c && c <= 0x200D)
				|| (0x2070 <= c && c <= 0x218F) || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
				|| (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD) || (0x10000 <= c && c <= 0xEFFFF));
	}
}
