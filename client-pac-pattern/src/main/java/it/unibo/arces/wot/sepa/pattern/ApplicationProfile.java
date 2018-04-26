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

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
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
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/subscribe"
			}
		},
		"security": {
			"register": "/oauth/register",
			"tokenRequest": "/oauth/token",
			"securePath": "/secure",
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
						"value" : ""}
					 ,
					"variable_2" : {
						"type" : "literal" ,
						"value" : ""}
					 ,
					"variable_N" : {
						"type" : "uri" ,
						"value" : ""}
				}
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
						"value" : ""}
					 ,
					"variable_2" : {
						"type" : "literal" ,
						"value" : ""}
					 ,
					"variable_N" : {
						"type" : "uri" ,
						"value" : ""}
				}
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
	}

	public ApplicationProfile(String propertiesFile, byte[] secret) throws SEPAPropertiesException {
		super(propertiesFile, secret);
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

	/**
	 * <pre>
	 * "UPDATE_1" : {
			"sparql" : "..." ,
			"forcedBindings" : {
				"variable_1" : {
					"type" : "literal" ,
					"value" : ""}
				 ,
				"variable_2" : {
					"type" : "literal" ,
					"value" : ""}
				 ,
				"variable_N" : {
					"type" : "uri" ,
					"value" : ""}
			}
		}
	 * </pre>
	 */
	public String update(String updateID) {
		try {
			return jsap.get("updates").getAsJsonObject().get(updateID).getAsJsonObject().get("sparql").getAsString();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	public String subscribe(String subscribeID) {
		try {
			return jsap.get("queries").getAsJsonObject().get(subscribeID).getAsJsonObject().get("sparql").getAsString();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	public Set<String> getUpdateIds() {
		HashSet<String> ret = new HashSet<String>();

		try {
			for (Entry<String, JsonElement> key : jsap.get("updates").getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return ret;
	}

	public Set<String> getSubscribeIds() {
		HashSet<String> ret = new HashSet<String>();

		try {
			for (Entry<String, JsonElement> key : jsap.get("queries").getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return ret;
	}

	/**
	 * <pre>
	 * "forcedBindings" : {
					"variable_1" : {
						"type" : "literal" ,
						"value" : ""}
					 ,
					"variable_2" : {
						"type" : "literal" ,
						"value" : ""}
					 ,
					"variable_N" : {
						"type" : "uri" ,
						"value" : ""}
				}
	 * </pre>
	 */
	public Bindings updateBindings(String selectedValue) {
		Bindings ret = new Bindings();

		try {
			for (Entry<String, JsonElement> binding : jsap.get("updates").getAsJsonObject().get(selectedValue)
					.getAsJsonObject().get("forcedBindings").getAsJsonObject().entrySet()) {
				RDFTerm bindingValue = null;
				if (binding.getValue().getAsJsonObject().get("type").getAsString().equals("uri")) {
					bindingValue = new RDFTermURI(binding.getValue().getAsJsonObject().get("value").getAsString());
				} else {
					bindingValue = new RDFTermLiteral(binding.getValue().getAsJsonObject().get("value").getAsString());
				}
				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return ret;
	}

	public Bindings subscribeBindings(String selectedValue) {
		Bindings ret = new Bindings();

		try {
			for (Entry<String, JsonElement> binding : jsap.get("queries").getAsJsonObject().get(selectedValue)
					.getAsJsonObject().get("forcedBindings").getAsJsonObject().entrySet()) {
				RDFTerm bindingValue = null;
				if (binding.getValue().getAsJsonObject().get("type").getAsString().equals("uri")) {
					bindingValue = new RDFTermURI(binding.getValue().getAsJsonObject().get("value").getAsString());
				} else {
					bindingValue = new RDFTermLiteral(binding.getValue().getAsJsonObject().get("value").getAsString());
				}
				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.error(e.getMessage());
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
}
