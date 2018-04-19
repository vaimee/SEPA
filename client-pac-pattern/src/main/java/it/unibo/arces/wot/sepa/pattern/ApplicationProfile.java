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

/** SAP file example
 * * <pre>
 * {"parameters" : {
		"host" : "localhost" ,
		"ports" : {
			"http" : 8000 ,
			"https" : 8443 ,
			"ws" : 9000 ,
			"wss" : 9443}
		 ,
		"paths" : {
			"query" : "/query" ,
			"update" : "/update" ,
			"subscribe" : "/subscribe" ,
			"register" : "/oauth/register" ,
			"tokenRequest" : "/oauth/token" ,
			"securePath" : "/secure"}
		 ,
		"methods" : {
			"query" : "POST" ,
			"update" : "URL_ENCODED_POST"}
		 ,
		"formats" : {
			"query" : "JSON" ,
			"update" : "HTML"}
		 ,
		"security" : {
			"client_id" : "..." ,
			"client_secret" : "..." ,
			"jwt" : "..." ,
			"expires" : "..." ,
			"type" : "..."},
			
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
	public ApplicationProfile(String propertiesFile) throws  SEPAPropertiesException {
		super(propertiesFile);
	}

	public ApplicationProfile(String propertiesFile,byte[] secret) throws SEPAPropertiesException {
		super(propertiesFile,secret);
	}
	
	protected Logger logger = LogManager.getLogger("JSAP");	
	
	public JsonObject getExtendedData(){
		if (doc.get("extended") == null) return null;
		return doc.get("extended").getAsJsonObject();
	}
	/**
	  <pre>"UPDATE_1" : {
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
			}</pre>
	 */
	public String update(String updateID) {
		JsonElement elem = null;
		if ((elem = doc.get("updates")) != null) 
			if ((elem = elem.getAsJsonObject().get(updateID))!=null)
				if ((elem = elem.getAsJsonObject().get("sparql"))!=null) return elem.getAsString();
		return null;
	}
	
	public String subscribe(String subscribeID) {
		JsonElement elem = null;
		if ((elem = doc.get("queries")) != null) 
			if ((elem = elem.getAsJsonObject().get(subscribeID))!=null)
				if ((elem = elem.getAsJsonObject().get("sparql"))!=null) return elem.getAsString();
		return null;
	}
	
	public Set<String> getUpdateIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("updates"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	public Set<String> getSubscribeIds() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("queries"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	/**
	 <pre>"forcedBindings" : {
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
				}</pre>
	 */
	public Bindings updateBindings(String selectedValue) {
		JsonElement elem;
		Bindings ret = new Bindings();
		if ((elem = doc.get("updates")) !=null)
			if((elem = elem.getAsJsonObject().get(selectedValue))!=null)
				if((elem = elem.getAsJsonObject().get("forcedBindings"))!=null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;
				
						if (value.get("type")!=null){
							if(value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							}
							else {
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
		if ((elem = doc.get("queries")) !=null)
			if((elem = elem.getAsJsonObject().get(selectedValue))!=null)
				if((elem = elem.getAsJsonObject().get("forcedBindings"))!=null) {
					for (Entry<String, JsonElement> binding : elem.getAsJsonObject().entrySet()) {
						JsonObject value = binding.getValue().getAsJsonObject();
						RDFTerm bindingValue = null;
				
						if (value.get("type")!=null){
							if(value.get("type").getAsString().equals("uri")) {
								bindingValue = new RDFTermURI(value.get("value").getAsString());
							}
							else {
								bindingValue = new RDFTermLiteral(value.get("value").getAsString());
							}
						}
						ret.addBinding(binding.getKey(), bindingValue);
					}
				}
		return ret;
	}

	/**
	 <pre>"namespaces" : { 
	 	"iot":"http://www.arces.unibo.it/iot#",
	 	"rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"},</pre>
	 */
	     	
	public Set<String> getPrefixes() {
		JsonElement elem;
		HashSet<String> ret = new HashSet<String>();
		if((elem = doc.get("namespaces"))!=null)
			for (Entry<String, JsonElement> key :elem.getAsJsonObject().entrySet()){
				ret.add(key.getKey());
			}
		return ret;
	}
	
	public String getNamespaceURI(String prefix) {
		JsonElement elem;
		String ret = null;
		if((elem = doc.get("namespaces"))!=null)
			if((elem = elem.getAsJsonObject().get(prefix))!= null)
				return elem.getAsString();
		return ret;
	}

	public String getFileName() {
		return propertiesFile.getName();
	}

	public String printParameters() {
		return parameters.toString();
	}
}
