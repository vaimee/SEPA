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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.security.OAuthProperties;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermBNode;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.logging.Logging;

/**
 * JSAP file example
 * 
 * <pre>
 * {
 "#include" : ["file:///test1.jsap","file://localhost/test2.jsap","file:/test3.jsap",...]
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
		"reconnect" : false, (optional)
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
		"default-graph-uri ": ["..."],
		"named-graph-uri": ["..."],
		"using-graph-uri": ["..."],
		"using-named-graph-uri": ["..."]
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
	private static Set<String> numbersOrBoolean = new HashSet<String>();

	private String prefixes = "";

	// Members
	private HashMap<String, Query> queries = null;// new HashMap<String, Query>();
	private HashMap<String, Update> updates = null;// new HashMap<String, Update>();
	private HashMap<String, String> namespaces = new HashMap<String, String>();
	private JsonObject extended = null;// new JsonObject();
	private JsonArray include = null; // new JsonArray();

	private static class Query {
		public String sparql = null;
		public HashMap<String, ForcedBinding> forcedBindings = null;
		public SPARQL11Properties sparql11protocol = null;
		public SPARQL11SEProperties sparql11seprotocol = null;
	}

	private static class Update {
		public String sparql = null;
		public HashMap<String, ForcedBinding> forcedBindings = null;
		public SPARQL11Properties sparql11protocol = null;
	}

	private static class ForcedBinding {
		public String type = null;
		public String datatype = null;
		public String value = null;
		public String language = null;
	}
	
	private void defaultNamespaces() {
		// Numbers or boolean
		numbersOrBoolean.add("xsd:integer");
		numbersOrBoolean.add("xsd:decimal");
		numbersOrBoolean.add("xsd:double");
		numbersOrBoolean.add("xsd:boolean");

		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#integer");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#decimal");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#double");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#boolean");

		// Default namespaces
		if (namespaces == null) namespaces = new HashMap<String, String>();
		namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		namespaces.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		namespaces.put("owl", "http://www.w3.org/2002/07/owl#");
		namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}

	public JSAP(String propertiesFile) throws SEPAPropertiesException, SEPASecurityException {
		super(propertiesFile);

		loadFromFile(propertiesFile, true);
		
		defaultNamespaces();

		buildSPARQLPrefixes();
	}

	private JSAP merge(JSAP temp) {
		host = (temp.host != null ? temp.host : this.host);

		sparql11protocol = mergeSparql11protocol(sparql11protocol, temp.sparql11protocol);
		sparql11seprotocol = mergeSparql11seprotocol(sparql11seprotocol, temp.sparql11seprotocol);
		extended = mergeExtended(extended, temp.extended);
		graphs = mergeGraphs(graphs, temp.graphs);
		namespaces = mergeNamespaces(namespaces, temp.namespaces);
		queries = mergeQueries(queries, temp.queries);
		updates = mergeUpdates(updates, temp.updates);

		return this;
	}

	private SPARQL11SEProtocol mergeSparql11seprotocol(SPARQL11SEProtocol jsap, SPARQL11SEProtocol temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		return jsap.merge(temp);
	}

	private SPARQL11Protocol mergeSparql11protocol(SPARQL11Protocol jsap, SPARQL11Protocol temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		return jsap.merge(temp);
	}

	private JsonObject mergeExtended(JsonObject jsap, JsonObject temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;

		for (Entry<String, JsonElement> entry : temp.entrySet()) {
			JsonElement value = entry.getValue();
			String key = entry.getKey();

			if (!jsap.has(key)) {
				jsap.add(key, value);
				continue;
			}

			if (value.isJsonPrimitive()) {
				jsap.add(key, value);
			} else if (value.isJsonObject()) {
				JsonObject obj = mergeExtended(value.getAsJsonObject(), jsap.getAsJsonObject(key));
				jsap.add(key, obj);
			} else if (value.isJsonArray()) {
				for (JsonElement arr : value.getAsJsonArray()) {
					jsap.getAsJsonArray(key).add(arr);
				}
			}
		}

		return jsap;
	}

	private HashMap<String, Update> mergeUpdates(HashMap<String, Update> jsap, HashMap<String, Update> temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		jsap.putAll(temp);
		return jsap;
	}

	private HashMap<String, Query> mergeQueries(HashMap<String, Query> jsap, HashMap<String, Query> temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		jsap.putAll(temp);
		return jsap;
	}

	private HashMap<String, String> mergeNamespaces(HashMap<String, String> jsap, HashMap<String, String> temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		jsap.putAll(temp);
		return jsap;
	}

	private Graphs mergeGraphs(Graphs jsap, Graphs temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;

		try {
			jsap.default_graph_uri.addAll(temp.default_graph_uri);
		} catch (Exception e) {

		}
		try {
			jsap.named_graph_uri.addAll(temp.named_graph_uri);
		} catch (Exception e) {

		}
		try {
			jsap.using_graph_uri.addAll(temp.using_graph_uri);
		} catch (Exception e) {

		}
		try {
			jsap.using_named_graph_uri.addAll(temp.using_named_graph_uri);
		} catch (Exception e) {

		}

		return jsap;
	}

	public JsonObject getExtendedData() {
		return extended;
	}

	private void __read(Reader in, boolean replace) {
		JSAP jsap = new Gson().fromJson(in, JSAP.class);

		if (replace) {
			this.include = jsap.include;
			this.host = jsap.host;
			this.sparql11protocol = jsap.sparql11protocol;
			this.sparql11seprotocol = jsap.sparql11seprotocol;
			this.extended = jsap.extended;
			this.graphs = jsap.graphs;
			this.namespaces = jsap.namespaces;
			this.queries = jsap.queries;
			this.updates = jsap.updates;
		} else {
			merge(jsap);
		}

		buildSPARQLPrefixes();
	}

	private void loadFromFile(String uri, boolean replace) throws SEPAPropertiesException, SEPASecurityException {
		read(uri, replace);

		ArrayList<String> files = new ArrayList<>();

		if (include != null) {
			for (JsonElement element : include)
				files.add(element.getAsString());

			include = new JsonArray();
		}

		for (String file : files) {
			String path = new File(uri).getParent();
			if (path == null) loadFromFile(file, false);
			else loadFromFile(path + File.separator + file, false);
		}
			
	}

	public void read(Reader input, boolean replace) throws SEPAPropertiesException, SEPASecurityException {
		//__read(new InputStreamReader(input), replace);
		__read(input, replace);
	}

	/**
	 * Parse the file and merge the content with the actual JSAP object. Primitive
	 * values are replaced if replace = true.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 */
	public void read(String filename, boolean replace) throws SEPAPropertiesException, SEPASecurityException {
		FileReader in;
		try {
			in = new FileReader(filename);
		} catch (FileNotFoundException e) {
			throw new SEPAPropertiesException(e.getMessage());
		}

		__read(in, replace);

		try {
			in.close();
		} catch (IOException e) {
			Logging.logger.error(e.getMessage());
		}
	}

	public void write() throws SEPAPropertiesException {
		write(getFileName());
	}

	public void write(String fileName) throws SEPAPropertiesException {
		FileWriter out;
		try {
			out = new FileWriter(fileName);
			out.write(this.toString());
			out.close();
		} catch (IOException e) {
			if (Logging.logger.isTraceEnabled())
				e.printStackTrace();
			throw new SEPAPropertiesException(e.getMessage());
		}
	}

	// TODO: add the query as parameter and get just the namespaces required
	public String getPrefixes() {
		return prefixes;
	}

	public HashMap<String, String> getNamespaces() {
		return namespaces;
	}

	private void buildSPARQLPrefixes() {
		prefixes = "";
		if (namespaces == null) return;
		for (String prefix : namespaces.keySet()) {
			prefixes += "PREFIX " + prefix + ":<" + namespaces.get(prefix) + "> ";
		}
	}

	public OAuthProperties getAuthenticationProperties() {
		return null;
	}

	public boolean isSecure() {
		return false;
	}

	public boolean reconnect() {
		return super.getReconnect();
	}

	/*
	 * UPDATE
	 */
	public String getSPARQLUpdate(String id) {
		return (updates.get(id) == null ? null : updates.get(id).sparql);
	}

	public String getUpdateHost(String id) {
		try {
			return updates.get(id).sparql11protocol.getHost();
		} catch (Exception e) {
			return host;
		}
	}

	public String getUpdateAcceptHeader(String id) {
		try {
			return updates.get(id).sparql11protocol.getUpdateAcceptHeader();
		} catch (Exception e) {
			return sparql11protocol.update.format.getUpdateAcceptHeader();
		}
	}

	public UpdateHTTPMethod getUpdateMethod(String id) {
		try {
			return updates.get(id).sparql11protocol.getUpdateMethod();
		} catch (Exception e) {
			return sparql11protocol.update.method;
		}
	}

	public String getUpdateProtocolScheme(String id) {
		try {
			return updates.get(id).sparql11protocol.getProtocolScheme();
		} catch (Exception e) {
			return sparql11protocol.protocol.getProtocolScheme();
		}
	}

	public String getUpdatePath(String id) {
		try {
			return updates.get(id).sparql11protocol.getUpdatePath();
		} catch (Exception e) {
			return sparql11protocol.update.path;
		}
	}

	public int getUpdatePort(String id) {
		try {
			return updates.get(id).sparql11protocol.getPort();
		} catch (Exception e) {
			return sparql11protocol.port;
		}
	}

	public Set<String> getUsingNamedGraphURI(String id) {
		try {
			return updates.get(id).sparql11protocol.getUsingNamedGraphURI();
		} catch (Exception e) {
			if (graphs == null) return null;
			return graphs.using_named_graph_uri;
		}
	}

	public Set<String> getUsingGraphURI(String id) {
		try {
			return updates.get(id).sparql11protocol.getUsingGraphURI();
		} catch (Exception e) {
			if (graphs == null) return null;
			return graphs.using_graph_uri;
		}
	}

	public void setSPARQLUpdate(String id, String sparql) {
		if (updates.get(id) != null)
			updates.get(id).sparql = sparql;
	}

	public void setUpdateHost(String id, String host) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setHost(host);
	}

	public void setUpdateAcceptHeader(String id, UpdateResultsFormat format) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setUpdateAcceptHeader(format);
	}

	public void setUpdateMethod(String id, UpdateHTTPMethod method) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setUpdateMethod(method);
	}

	public void setUpdateProtocolScheme(String id, ProtocolScheme scheme) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setProtocolScheme(scheme);
	}

	public void setUpdatePath(String id, String path) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setUpdatePath(path);
	}

	public void setUpdatePort(String id, int port) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setPort(port);
	}

	public void setUsingNamedGraphUri(String id, Set<String> graph) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setUsingNamedGraphURI(graph);
	}

	public void setUsingGraphURI(String id, Set<String> graph) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setUsingGraphURI(graph);
	}

	/*
	 * QUERY
	 */
	public String getSPARQLQuery(String id) {
		return (queries.get(id) == null ? null : queries.get(id).sparql);
	}

	public String getQueryHost(String id) {
		try {
			return queries.get(id).sparql11protocol.getHost();
		} catch (Exception e) {
			return host;
		}
	}

	public String getQueryProtocolScheme(String id) {
		try {
			return queries.get(id).sparql11protocol.getProtocolScheme();
		} catch (Exception e) {
			return sparql11protocol.protocol.getProtocolScheme();
		}
	}

	public int getQueryPort(String id) {
		try {
			return queries.get(id).sparql11protocol.getPort();
		} catch (Exception e) {
			return sparql11protocol.port;
		}
	}

	public String getQueryPath(String id) {
		try {
			return queries.get(id).sparql11protocol.getQueryPath();
		} catch (Exception e) {
			return sparql11protocol.query.path;
		}
	}

	public QueryHTTPMethod getQueryMethod(String id) {
		try {
			return queries.get(id).sparql11protocol.getQueryMethod();
		} catch (Exception e) {
			return sparql11protocol.query.method;
		}
	}

	public String getQueryAcceptHeader(String id) {
		try {
			return queries.get(id).sparql11protocol.getQueryAcceptHeader();
		} catch (Exception e) {
			return sparql11protocol.query.format.getQueryAcceptHeader();
		}
	}

	public Set<String> getNamedGraphURI(String id) {
		try {
			return queries.get(id).sparql11protocol.getNamedGraphURI();
		} catch (Exception e) {
			if (graphs == null) return null;
			return graphs.named_graph_uri;
		}
	}

	public Set<String> getDefaultGraphURI(String id) {
		try {
			return queries.get(id).sparql11protocol.getDefaultGraphURI();
		} catch (Exception e) {
			if (graphs == null) return null;
			return graphs.default_graph_uri;
		}
	}

	public void setSPARQLQuery(String id, String sparql) {
		if (queries.get(id) != null)
			queries.get(id).sparql = sparql;
	}

	public void setQueryHost(String id, String host) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setHost(host);
	}

	public void setQueryAcceptHeader(String id, QueryResultsFormat format) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setQueryAcceptHeader(format);
	}

	public void setQueryMethod(String id, QueryHTTPMethod method) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setQueryMethod(method);
	}

	public void setQueryProtocolScheme(String id, ProtocolScheme scheme) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setProtocolScheme(scheme);
	}

	public void setQueryPath(String id, String path) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setQueryPath(path);
	}

	public void setQueryPort(String id, int port) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setPort(port);
	}

	public void setNamedGraphUri(String id, Set<String> graph) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setNamedGraphURI(graph);
	}

	public void setDefaultGraphURI(String id, Set<String> graph) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setDefaultGraphURI(graph);
	}

	/*
	 * SUBSCRIBE
	 */

	public String getSubscribeHost(String id) {
		try {
			return queries.get(id).sparql11seprotocol.getHost();
		} catch (Exception e) {
			return host;
		}
	}

	public void setSubscribeHost(String id, String host) {
		if (queries.get(id) == null)
			return;
//		if (queries.get(id).sparql11seprotocol == null)
//			queries.get(id).sparql11seprotocol = new SPARQL11SEProperties();
		queries.get(id).sparql11seprotocol.setHost(host);
	}

	public int getSubscribePort(String id) {
		try {
			return queries.get(id).sparql11seprotocol.getPort();
		} catch (Exception e) {
			return sparql11seprotocol.getPort();
		}
	}

	public void setSubscribePort(String id, int port) {
		if (queries.get(id) == null)
			return;
//		if (queries.get(id).sparql11seprotocol == null)
//			queries.get(id).sparql11seprotocol = new SPARQL11SEProperties();
		queries.get(id).sparql11seprotocol.setPort(port);
	}

	public String getSubscribePath(String id) {
		try {
			return queries.get(id).sparql11seprotocol.getQueryPath();
		} catch (Exception e) {
			return sparql11seprotocol.getPath();
		}
	}

	public void setSubscribePath(String id, String path) {
		if (queries.get(id) == null)
			return;
//		if (queries.get(id).sparql11seprotocol == null)
//			queries.get(id).sparql11seprotocol = new SPARQL11SEProperties();
		queries.get(id).sparql11seprotocol.setSubscribePath(path);
	}

	public SubscriptionProtocol getSubscribeProtocol(String id) {
		try {
			return queries.get(id).sparql11seprotocol.getSubscriptionProtocol();
		} catch (Exception e) {
			return sparql11seprotocol.getSubscriptionProtocol();
		}
	}

	public void setSubscribeProtocol(String id, SubscriptionProtocol sp) {
		if (queries.get(id) == null)
			return;
//		if (queries.get(id).sparql11seprotocol == null)
//			queries.get(id).sparql11seprotocol = new SPARQL11SEProperties();
		queries.get(id).sparql11seprotocol.setSubscriptionProtocol(sp.scheme);
	}

	public Set<String> getUpdateIds() {
		return updates.keySet();
	}

	public Set<String> getQueryIds() {
		return queries.keySet();
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
	public ForcedBindings getUpdateBindings(String id) throws IllegalArgumentException {
		if (updates.get(id) == null)
			throw new IllegalArgumentException("Update ID not found: " + id);

		ForcedBindings ret = new ForcedBindings();

		if (updates.get(id).forcedBindings == null)
			return ret;

		try {
			for (Entry<String, ForcedBinding> binding : updates.get(id).forcedBindings.entrySet()) {
				if (binding.getValue().type == null)
					continue;

				RDFTerm bindingValue = null;
				String value = binding.getValue().value;

				switch (binding.getValue().type) {
				case "literal":
					String datatype = (binding.getValue().datatype == null ? null : binding.getValue().datatype);
					String language = (binding.getValue().language == null ? null : binding.getValue().language);

					bindingValue = new RDFTermLiteral(value, datatype, language);
					break;
				case "uri":
					bindingValue = new RDFTermURI(value);
					break;
				case "bnode":
					bindingValue = new RDFTermBNode(value);
					break;
				default:
					Logging.logger.error("JSAP unknown type: " + binding);
					continue;
				}

				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
			Logging.logger.error("getUpdateBindings " + id + " exception: " + e.getMessage());
		}

		return ret;
	}

	public ForcedBindings getQueryBindings(String id) throws IllegalArgumentException {
		if (queries.get(id) == null)
			throw new IllegalArgumentException("Query ID not found: " + id);

		ForcedBindings ret = new ForcedBindings();

		if (queries.get(id).forcedBindings == null)
			return ret;

		try {
			for (Entry<String, ForcedBinding> binding : queries.get(id).forcedBindings.entrySet()) {
				if (binding.getValue().type == null)
					continue;

				RDFTerm bindingValue = null;
				String value = binding.getValue().value;

				switch (binding.getValue().type) {
				case "literal":
					String datatype = (binding.getValue().datatype == null ? null : binding.getValue().datatype);
					String language = (binding.getValue().language == null ? null : binding.getValue().language);

					bindingValue = new RDFTermLiteral(value, datatype, language);
					break;
				case "uri":
					bindingValue = new RDFTermURI(value);
					break;
				case "bnode":
					bindingValue = new RDFTermBNode(value);
					break;
				default:
					Logging.logger.error("JSAP unknown type: " + binding);
					continue;
				}

				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
			Logging.logger.error("getQueryBindings " + id + " exception: " + e.getMessage());
		}

		return ret;
	}

	public String getFileName() {
		return propertiesFile;
	}

	public String toString() {
		return new Gson().toJson(this);
	}

//	public String printParameters() {
//		return this.toString();
//	}

	public String getUpdateUrl(String id) throws IllegalArgumentException {
		if (updates.get(id) == null)
			throw new IllegalArgumentException("Update ID not found: " + id);
		String port = "";
		if (getUpdatePort(id) != -1)
			port = ":" + getUpdatePort(id);
		return getUpdateProtocolScheme(id) + "://" + getUpdateHost(id) + port + getUpdatePath(id);
	}

	public String getQueryUrl(String id) throws IllegalArgumentException {
		if (queries.get(id) == null)
			throw new IllegalArgumentException("Query ID not found: " + id);
		String port = "";
		if (getQueryPort(id) != -1)
			port = ":" + getQueryPort(id);
		return getQueryProtocolScheme(id) + "://" + getQueryHost(id) + port + getQueryPath(id);
	}

	public String getSubscribeUrl(String id) throws IllegalArgumentException {
		if (queries.get(id) == null)
			throw new IllegalArgumentException("Subscribe ID not found: " + id);

		String scheme = queries.get(id).sparql11seprotocol.getSubscriptionProtocol().scheme;
		String port = "";

		if (getSubscribePort(id) != -1)
			port = ":" + getSubscribePort(id);

		return scheme + "://" + getSubscribeHost(id) + port + getSubscribePath();
	}

	/*
	 * The grammar of SPARQL 1.1 Update is specified here:
	 * https://www.w3.org/TR/sparql11-query/#rUpdate
	 * 
	 * A request MAY include multiple primitives separated by ";" (see Update [29])
	 * 
	 * 
	 * [29] Update ::= Prologue ( Update1 ( ';' Update )? )?
	 */

	public String addPrefixesAndReplaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
		return prefixes + replaceBindings(sparql, bindings);
	}

	// TODO: use Jena?
//	private final String replaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
//		QuerySolutionMap initialBinding = new QuerySolutionMap();
//		for (String var : bindings.getVariables()) {
//			if (bindings.getRDFTerm(var).isLiteral()) {
//				RDFTermLiteral literal = (RDFTermLiteral) bindings.getRDFTerm(var);
//				String dataType = literal.getDatatype();
//				String lan = literal.getLanguageTag();
//				String value = literal.getValue();
//				if (dataType == null && lan == null) initialBinding.add(var, ResourceFactory.createPlainLiteral(value));
//				else if (dataType != null) initialBinding.add(var, ResourceFactory.createTypedLiteral(value, new XSDPlainType(dataType) ));
//				else if (lan != null) initialBinding.add(var, ResourceFactory.createLangLiteral(value, lan));
//			}
//			else initialBinding.add(var, ResourceFactory.createResource(bindings.getValue(var)));
//
//		}
//		QueryExecution qe = QueryExecutionFactory.create(sparql, initialBinding);
//		return qe.getQuery().toString();
//		
//	}

	public static final String replaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
		if (bindings == null || sparql == null)
			return sparql;

		String replacedSparql = String.format("%s", sparql);

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

				if (datatype == null) {
					if (lang != null)
						value += "@" + bindings.getLanguage(var);
					else {
						value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
					}
				} else if (!numbersOrBoolean.contains(datatype)) {
					// Check if datatype is a qname or not
					URI uri = null;
					try {
						uri = new URI(datatype);
					} catch (URISyntaxException e) {
						Logging.logger.error(e.getMessage());
					}

					if (uri != null) {
						if (uri.getSchemeSpecificPart().startsWith("/"))
							datatype = "<" + datatype + ">";
					}

					value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
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
					Logging.logger.error(e.getMessage());
				}

				if (uri != null) {
					if (uri.getSchemeSpecificPart().startsWith("/") || uri.getScheme().equals("urn"))
						value = "<" + value + ">";
				}
			} else {
				// A blank node
				Logging.logger.trace("Blank node: " + value);

				// Not a BLANK_NODE_LABEL
				// [142] BLANK_NODE_LABEL ::= '_:' ( PN_CHARS_U | [0-9] ) ((PN_CHARS|'.')*
				// PN_CHARS)?
				if (!value.startsWith("_:"))
					value = "<" + value + ">";
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
					if (index + var.length() + 1 <= replacedSparql.length() - 1) {
						int unicode = replacedSparql.codePointAt(index + var.length() + 1);
						if (!isValidVarChar(unicode)) {
							replacedSparql = replacedSparql.substring(0, index) + value
									+ replacedSparql.substring(index + var.length() + 1);
						}
					}
					// END OF STRING
					else {
						replacedSparql = replacedSparql.substring(0, index) + value;
					}

				} else
					start = index;
			}
		}

		return replacedSparql;
	}

	private static boolean isValidVarChar(int c) {
		return ((c == '_') || (c == 0x00B7) || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040)
				|| ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9')
				|| (0x00C0 <= c && c <= 0x00D6) || (0x00D8 <= c && c <= 0x00F6) || (0x00F8 <= c && c <= 0x02FF)
				|| (0x0370 <= c && c <= 0x037D) || (0x037F <= c && c <= 0x1FFF) || (0x200C <= c && c <= 0x200D)
				|| (0x2070 <= c && c <= 0x218F) || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
				|| (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD) || (0x10000 <= c && c <= 0xEFFFF));
	}

	public void setAutoReconnect(boolean b) {
		sparql11seprotocol.reconnect = b;
	}

}
