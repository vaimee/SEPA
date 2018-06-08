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

import java.util.Base64;
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
 * "host" : "localhost" ,
 	"sparql11protocol": {
 		"host":"override default host", 	(optional)
		"protocol": "http",
		"port": 8000,					(optional)
		"query": {
			"path": "/query",
			"method": "GET | POST | URL_ENCODED_POST",
			"format": "JSON | XML | CSV"
		},
		"update": {
			"path": "/update",
			"method": "POST | URL_ENCODED_POST",
			"format": "JSON | HTML"
		}
	},
  	"sparql11seprotocol": {
  	    "host":"override default host", 	(optional)
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
			    "host":"override default host", 	(optional)
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
			     "host":"override default host", 	(optional)
				"port": 9443,
				"path": "/secure/subscribe"
			}
		},
		"security": {
			"register": "https://localhost:8443/oauth/register",
			"tokenRequest": "https://localhost:8443/oauth/token",
			"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
			"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
			"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
			"expires": "04/5tRBT5n/VJ0XQASgs/w==",
			"type": "XPrHEX2xHy+5IuXHPHigMw=="
		},
	"graphs": {
		"default-graph-uri ": "http://default",
		"named-graph-uri": "http://default",
		"using-graph-uri": "http://default",
		"using-named-graph-uri": "http://default"
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
						"datatype": "xsd datatype"
						"value" : "..."}
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
						"type" : "xsd data type" ,
						"value" : ""}
					 ,
					"variable_2" : {
						"type" : "xsd data type" ,
						"value" : ""}
					 ,
					"variable_N" : {
						"type" : "xsd data type" ,
						"value" : ""}
				},
				"sparql11protocol" :{...} (optional),
				"sparql11seprotocol" :{...} (optional)
			}
			 ,
			"QUERY_N" : {
				"sparql" : "..."
			}
		}
		}}
 * </pre>
 */
public class ApplicationProfile extends SPARQL11SEProperties {
	public ApplicationProfile(String propertiesFile) throws SEPAPropertiesException {
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

	public ApplicationProfile(String propertiesFile, byte[] secret) throws SEPAPropertiesException {
		super(propertiesFile, secret);

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
	public boolean isAuthenticationRequiredForUpdate(String id) {
		try {
			return jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().has("authentication");
		} catch (Exception e) {
			logger.debug(e.getMessage());
			try {
				return jsap.has("authentication");
			} catch (Exception e1) {
				logger.debug(e1.getMessage());
				return false;
			}
		}
	}

	public String getUpdateAuthorizationHeader(String id) {
		try {
			if (jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().has("authentication")) {
				if (jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
						.getAsJsonObject().has("basic")) {
					String user = jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
							.getAsJsonObject().get("basic").getAsJsonObject().get("user").getAsString();
					String pass = jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
							.getAsJsonObject().get("basic").getAsJsonObject().get("pass").getAsString();
					byte[] buf = Base64.getEncoder().encode((user + ":" + pass).getBytes("UTF-8"));
					return "Basic " + new String(buf, "UTF-8");
				}
			} else if (jsap.has("authentication")) {
				if (jsap.get("authentication").getAsJsonObject().has("basic")) {
					String user = jsap.get("authentication").getAsJsonObject().get("basic").getAsJsonObject()
							.get("user").getAsString();
					String pass = jsap.get("authentication").getAsJsonObject().get("basic").getAsJsonObject()
							.get("pass").getAsString();
					byte[] buf = Base64.getEncoder().encode((user + ":" + pass).getBytes("UTF-8"));
					return "Basic " + new String(buf, "UTF-8");
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return "";
		}
		return "";
	}

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
			if (jsap.get("updates").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("update").getAsJsonObject().get("method").getAsString()
					.equals("URL_ENCODED_POST"))
				return HTTPMethod.URL_ENCODED_POST;
			return HTTPMethod.POST;
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
	public boolean isAuthenticationRequiredForQuery(String id) {
		try {
			return jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().has("authentication");
		} catch (Exception e) {
			logger.debug(e.getMessage());
			try {
				return jsap.has("authentication");
			} catch (Exception e1) {
				logger.debug(e1.getMessage());
				return false;
			}
		}
	}

	public String getQueryAuthorizationHeader(String id) {
		try {
			if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().has("authentication")) {
				if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
						.getAsJsonObject().has("basic")) {
					String user = jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
							.getAsJsonObject().get("basic").getAsJsonObject().get("user").getAsString();
					String pass = jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("authentication")
							.getAsJsonObject().get("basic").getAsJsonObject().get("pass").getAsString();
					byte[] buf = Base64.getEncoder().encode((user + ":" + pass).getBytes("UTF-8"));
					return "Basic " + new String(buf, "UTF-8");
				}
			} else if (jsap.has("authentication")) {
				if (jsap.get("authentication").getAsJsonObject().has("basic")) {
					String user = jsap.get("authentication").getAsJsonObject().get("basic").getAsJsonObject()
							.get("user").getAsString();
					String pass = jsap.get("authentication").getAsJsonObject().get("basic").getAsJsonObject()
							.get("pass").getAsString();
					byte[] buf = Base64.getEncoder().encode((user + ":" + pass).getBytes("UTF-8"));
					return "Basic " + new String(buf, "UTF-8");
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return "";
		}
		return "";
	}

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
			if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("query").getAsJsonObject().get("method").getAsString()
					.equals("URL_ENCODED_POST"))
				return HTTPMethod.URL_ENCODED_POST;
			else if (jsap.get("queries").getAsJsonObject().get(id).getAsJsonObject().get("sparql11protocol")
					.getAsJsonObject().get("query").getAsJsonObject().get("method").getAsString().equals("GET"))
				return HTTPMethod.GET;
			return HTTPMethod.POST;
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
			throw new IllegalArgumentException("Update ID not found");

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

				switch (binding.getValue().getAsJsonObject().get("datatype").getAsString()) {
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

				String datatype = null;
				if (binding.getValue().getAsJsonObject().has("type"))
					datatype = binding.getValue().getAsJsonObject().get("type").getAsString();

				if (datatype.equals("xsd:anyURI")) {
					bindingValue = new RDFTermURI(value);
				} else {

					String language = null;
					if (binding.getValue().getAsJsonObject().has("language"))
						language = binding.getValue().getAsJsonObject().get("language").getAsString();

					bindingValue = new RDFTermLiteral(value, datatype, language);
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
		return propertiesFile.getName();
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
