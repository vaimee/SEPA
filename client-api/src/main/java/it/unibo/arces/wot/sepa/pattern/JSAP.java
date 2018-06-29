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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
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
		}
		catch(Exception e) {
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
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol").getAsJsonObject().get("host").getAsString();
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
			switch(jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
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
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol").getAsJsonObject().get("host").getAsString();
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
			switch(jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
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

}
