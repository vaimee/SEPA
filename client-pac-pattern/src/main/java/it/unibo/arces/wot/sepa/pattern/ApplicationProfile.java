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
//import java.util.NoSuchElementException;
import java.util.Set;

//import javax.crypto.BadPaddingException;
//import javax.crypto.IllegalBlockSizeException;
//import javax.crypto.NoSuchPaddingException;

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
 * SAP file example *
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

	protected Logger logger = LogManager.getLogger("JSAP");

	public JsonObject getExtendedData() {
		if (jsap.get("extended") == null)
			return null;
		return jsap.get("extended").getAsJsonObject();
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
		JsonElement elem = null;
		if ((elem = jsap.get("updates")) != null)
			if ((elem = elem.getAsJsonObject().get(updateID)) != null)
				if ((elem = elem.getAsJsonObject().get("sparql")) != null)
					return elem.getAsString();
		return null;
	}

	public String subscribe(String subscribeID) {
		JsonElement elem = null;
		if ((elem = jsap.get("queries")) != null)
			if ((elem = elem.getAsJsonObject().get(subscribeID)) != null)
				if ((elem = elem.getAsJsonObject().get("sparql")) != null)
					return elem.getAsString();
		return null;
	}

	public Set<String> getUpdateIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if ((elem = jsap.get("updates")) != null)
			for (Entry<String, JsonElement> key : elem.getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		return ret;
	}

	public Set<String> getSubscribeIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if ((elem = jsap.get("queries")) != null)
			for (Entry<String, JsonElement> key : elem.getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
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
		JsonElement elem;
		Bindings ret = new Bindings();
		if ((elem = jsap.get("updates")) != null)
			if ((elem = elem.getAsJsonObject().get(selectedValue)) != null)
				if ((elem = elem.getAsJsonObject().get("forcedBindings")) != null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;

						if (value.get("type") != null) {
							if (value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							} else {
								bindingValue = new RDFTermLiteral(value.get("value").getAsString());
							}
						}
						ret.addBinding(binding.getKey(), bindingValue);
					}
				}
		return ret;
	}

	public Bindings subscribeBindings(String selectedValue) {
		JsonElement elem;
		Bindings ret = new Bindings();
		if ((elem = jsap.get("queries")) != null)
			if ((elem = elem.getAsJsonObject().get(selectedValue)) != null)
				if ((elem = elem.getAsJsonObject().get("forcedBindings")) != null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;

						if (value.get("type") != null) {
							if (value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							} else {
								bindingValue = new RDFTermLiteral(value.get("value").getAsString());
							}
						}
						ret.addBinding(binding.getKey(), bindingValue);
					}
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
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if ((elem = jsap.get("namespaces")) != null)
			for (Entry<String, JsonElement> key : elem.getAsJsonObject().entrySet()) {
				ret.add(key.getKey());
			}
		return ret;
	}

	public String getNamespaceURI(String prefix) {
		JsonElement elem;
		String ret = null;
		if ((elem = jsap.get("namespaces")) != null)
			if ((elem = elem.getAsJsonObject().get(prefix)) != null)
				return elem.getAsString();
		return ret;
	}

	public String getFileName() {
		return propertiesFile;
	}

	public String printParameters() {
		return jsap.toString();
	}
}
