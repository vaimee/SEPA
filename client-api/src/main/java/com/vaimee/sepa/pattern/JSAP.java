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

package com.vaimee.sepa.pattern;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.modify.request.UpdateAdd;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.sparql.modify.request.UpdateCopy;
import org.apache.jena.sparql.modify.request.UpdateCreate;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateDrop;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.modify.request.UpdateMove;
import org.apache.jena.sparql.modify.request.UpdateWriterVisitor;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vaimee.sepa.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.properties.QueryProperties.QueryHTTPMethod;
import com.vaimee.sepa.commons.properties.QueryProperties.QueryResultsFormat;
import com.vaimee.sepa.commons.properties.SPARQL11SEProperties;
import com.vaimee.sepa.commons.properties.SubscriptionProtocolProperties;
import com.vaimee.sepa.commons.properties.UpdateProperties;
import com.vaimee.sepa.commons.properties.UpdateProperties.UpdateHTTPMethod;
import com.vaimee.sepa.commons.properties.UpdateProperties.UpdateResultsFormat;
import com.vaimee.sepa.commons.security.OAuthProperties;
import com.vaimee.sepa.commons.sparql.Bindings;
import com.vaimee.sepa.commons.sparql.RDFTerm;
import com.vaimee.sepa.commons.sparql.RDFTermBNode;
import com.vaimee.sepa.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.commons.sparql.RDFTermURI;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.logging.Timings;
import com.vaimee.sepa.pattern.JSAPPrimitive.ForcedBinding;

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
	private static final Set<String> numbersOrBoolean = new HashSet<>();
	private final PrefixMappingMem prefixes;

	// Members
	protected String host = "localhost";
	protected HashMap<String, QueryPrimitive> queries = null;// new HashMap<String, Query>();
	protected HashMap<String, UpdatePrimitive> updates = null;// new HashMap<String, Update>();
	protected HashMap<String, String> namespaces = new HashMap<>();
	protected JsonObject extended = null;// new JsonObject();
	protected JsonArray include = null; // new JsonArray()
	private String prologue;

	public static void writeToFile(JSAP jsap,String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		fw.write(jsap.toString());
		fw.close();
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
		if (namespaces == null)
			namespaces = new HashMap<>();
		namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		namespaces.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		namespaces.put("owl", "http://www.w3.org/2002/07/owl#");
		namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}

	public JSAP(URI uri) throws SEPAPropertiesException {
		this(uri,null);
	}
	
	public JSAP(String uri) throws SEPAPropertiesException {
		this(URI.create(uri),null);
	}

	public JSAP(String uri, String[] args) throws SEPAPropertiesException {
		this(URI.create(uri),args);
	}
	public JSAP(URI uri, String[] args) throws SEPAPropertiesException {
		super(uri,args);

		prefixes = new PrefixMappingMem();

		Reader in = getReaderFromUri(uri);
		JSAP jsap = new Gson().fromJson(in, JSAP.class);

		include = jsap.include;
		host = jsap.host;
		extended = jsap.extended;
		graphs = jsap.graphs;
		namespaces = jsap.namespaces;
		queries = jsap.queries;
		updates = jsap.updates;

		try {
			in.close();
		} catch (IOException e) {
			throw new SEPAPropertiesException(e);
		}

		Map<String, String> envs = System.getenv();
		for(String var : envs.keySet()) {
			Logging.logger.trace("Environmental variable "+var+" : "+envs.get(var));
			setJsapParameter("-"+var, envs.get(var));
		}

		if (args != null)
			for (int i = 0; i < args.length; i++) {
				Logging.logger.trace("Argument  "+args[i]);
				String[] params = args[i].split("=");
				if (params.length == 2) {
					setJsapParameter(params[0], params[1]);
				}
			}

		ArrayList<String> uriList = new ArrayList<>();

		if (include != null) {
			for (JsonElement element : include)
				uriList.add(element.getAsString());

			include = new JsonArray();
		}

		for (String child : uriList) {
			JSAP temp = new JSAP(URI.create(child));
			merge(temp);
		}

		defaultNamespaces();

		buildSPARQLPrefixes();
	}
	
	protected void setJsapParameter(String key,String value) {
        if (key.equals("-host")) {
            this.host = value;
        }
	}

	public void read(Reader in, boolean replace) {
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

	public void merge(JSAP temp) {
		host = (temp.host != null ? temp.host : this.host);

		if (sparql11protocol != null)
			sparql11protocol.merge(temp.sparql11protocol);
		else
			sparql11protocol = temp.sparql11protocol;

		if (sparql11seprotocol != null)
			sparql11seprotocol.merge(temp.sparql11seprotocol);
		else
			sparql11seprotocol = temp.sparql11seprotocol;

		if (graphs != null)
			graphs.merge(temp.graphs);
		else
			graphs = temp.graphs;

		extended = mergeExtended(extended, temp.extended);
		namespaces = mergeNamespaces(namespaces, temp.namespaces);
		queries = mergeQueries(queries, temp.queries);
		updates = mergeUpdates(updates, temp.updates);
	}

	private JsonObject mergeExtended(JsonObject extended, JsonObject temp) {
		if (extended == null)
			return temp;
		if (temp == null)
			return extended;

		for (Entry<String, JsonElement> entry : temp.entrySet()) {
			JsonElement value = entry.getValue();
			String key = entry.getKey();

			if (!extended.has(key)) {
				extended.add(key, value);
				continue;
			}

			if (value.isJsonPrimitive()) {
				extended.add(key, value);
			} else if (value.isJsonObject()) {
				JsonObject obj = mergeExtended(value.getAsJsonObject(), extended.getAsJsonObject(key));
				extended.add(key, obj);
			} else if (value.isJsonArray()) {
				for (JsonElement arr : value.getAsJsonArray()) {
					extended.getAsJsonArray(key).add(arr);
				}
			}
		}

		return extended;
	}

	private HashMap<String, UpdatePrimitive> mergeUpdates(HashMap<String, UpdatePrimitive> jsap,
			HashMap<String, UpdatePrimitive> temp) {
		if (jsap == null)
			return temp;
		if (temp == null)
			return jsap;
		jsap.putAll(temp);
		return jsap;
	}

	private HashMap<String, QueryPrimitive> mergeQueries(HashMap<String, QueryPrimitive> jsap,
			HashMap<String, QueryPrimitive> temp) {
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

	public JsonObject getExtendedData() {
		return extended;
	}

	// TODO: add the query as parameter and get just the namespaces required
	public PrefixMappingMem getPrefixes() {
		return prefixes;
	}

	public HashMap<String, String> getNamespaces() {
		return namespaces;
	}

	private void buildSPARQLPrefixes() {
		// prefixes = "";
		if (namespaces == null)
			return;
		for (String prefix : namespaces.keySet()) {
			prefixes.setNsPrefix(prefix, namespaces.get(prefix));
			// prefixes += "PREFIX " + prefix + ":<" + namespaces.get(prefix) + "> ";
		}
		Logging.logger.trace("Prefixes map: " + prefixes.toString());
		prologue = "";
		StringBuilder sb = new StringBuilder();
		for (String pre : getPrefixes().getNsPrefixMap().keySet()) {
			sb.append("PREFIX ").append(pre).append(":<").append(getPrefixes().getNsPrefixURI(pre)).append("> ");
        }
		prologue = sb.toString();
		Logging.logger.trace("Prefixes SPARQL: " + prologue);

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
	public String getUpdateHost() {
		String ret = sparql11protocol.getHost();
		if (ret == null) ret = host;
		return ret;
	}	
	
	public String getSPARQLUpdate(String id) {
		return (updates.get(id) == null ? null : updates.get(id).sparql);
	}

	public String getUpdateHost(String id) {
		String ret = null;
		try {
			ret = updates.get(id).sparql11protocol.getHost();
		} catch (Exception ignored) {

		}
		if (ret == null) {
			ret = sparql11protocol.getHost();
			if (ret == null) ret = host;
		}
		
		return ret;
	}

	public String getUpdateAcceptHeader(String id) {
		String ret = null;
		try {
			ret = updates.get(id).sparql11protocol.getUpdate().getFormat().getUpdateAcceptHeader();
		} catch (Exception ignored) {
			
		}
		return (ret == null ? sparql11protocol.getUpdate().getFormat().getUpdateAcceptHeader() : ret);
	}

	public UpdateProperties.UpdateHTTPMethod getUpdateMethod(String id) {
		UpdateProperties.UpdateHTTPMethod ret = null;
		try {
			ret =  updates.get(id).sparql11protocol.getUpdate().getMethod();
		} catch (Exception ignored) {
			
		}
		return (ret == null ? sparql11protocol.getUpdate().getMethod() : ret);
	}

	public String getUpdateProtocolScheme(String id) {
		String ret = null;
		try {
			ret = updates.get(id).sparql11protocol.getProtocol().getProtocolScheme();
		} catch (Exception ignored) {
			
		}
		return (ret == null ? sparql11protocol.getProtocol().getProtocolScheme() : ret);
	}

	public String getUpdatePath(String id) {
		String ret = null;
		try {
			ret = updates.get(id).sparql11protocol.getUpdate().getPath();
		} catch (Exception ignored) {
			
		}
		return (ret == null ? sparql11protocol.getUpdate().getPath() : ret);
	}

	public int getUpdatePort(String id) {
		int ret = -1;
		try {
			ret =  updates.get(id).sparql11protocol.getPort();
		} catch (Exception ignored) {
			
		}
		return (ret == -1 ? sparql11protocol.getPort() : ret);
	}

	public Set<String> getUsingNamedGraphURI(String id) {
		try {
			return updates.get(id).graphs.using_named_graph_uri;
		} catch (Exception e) {
			if (graphs == null)
				return null;
			return graphs.using_named_graph_uri;
		}
	}

	public Set<String> getUsingGraphURI(String id) {
		try {
			return updates.get(id).graphs.using_graph_uri;
		} catch (Exception e) {
			if (graphs == null)
				return null;
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
		updates.get(id).sparql11protocol.getUpdate().setFormat(format);
	}

	public void setUpdateMethod(String id, UpdateHTTPMethod method) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.getUpdate().setMethod(method);
	}

	public void setUpdateProtocolScheme(String id, ProtocolScheme scheme) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setProtocol(scheme);
	}

	public void setUpdatePath(String id, String path) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.getUpdate().setPath(path);
	}

	public void setUpdatePort(String id, int port) {
		if (updates.get(id) == null)
			return;
		updates.get(id).sparql11protocol.setPort(port);
	}

	public void setUsingNamedGraphUri(String id, Set<String> graph) {
		if (updates.get(id) == null)
			return;
		updates.get(id).graphs.using_named_graph_uri = graph;
	}

	public void setUsingGraphURI(String id, Set<String> graph) {
		if (updates.get(id) == null)
			return;
		updates.get(id).graphs.using_graph_uri = graph;
	}

	/*
	 * QUERY
	 */
	public String getQueryHost() {
		String ret = sparql11protocol.getHost();
		if (ret == null) ret = host;
		return ret;
	}	

	public String getSPARQLQuery(String id) {
		return (queries.get(id) == null ? null : queries.get(id).sparql);
	}

	public String getQueryHost(String id) {
		String ret = null;
		try {
			ret = queries.get(id).sparql11protocol.getHost();
		} catch (Exception ignored) {
			
		}
		if (ret == null) {
			ret = sparql11protocol.getHost();
			if (ret == null) ret = host;
		}
		
		return ret;
	}

	public String getQueryProtocolScheme(String id) {
		try {
			return queries.get(id).sparql11protocol.getProtocol().getProtocolScheme();
		} catch (Exception e) {
			return sparql11protocol.getProtocol().getProtocolScheme();
		}
	}

	public int getQueryPort(String id) {
		int ret = -1;
		try {
			ret =  queries.get(id).sparql11protocol.getPort();
		} catch (Exception ignored) {
			
		}
		return (ret == -1 ? sparql11protocol.getPort() : ret);
	}

	public String getQueryPath(String id) {
		String ret = null;
		try {
			ret = queries.get(id).sparql11protocol.getQuery().getPath();
		} catch (Exception ignored) {
			
		}
		return (ret == null ?sparql11protocol.getQuery().getPath() : ret);
	}

	public QueryHTTPMethod getQueryMethod(String id) {
		QueryHTTPMethod ret = null;
		try {
			ret = queries.get(id).sparql11protocol.getQuery().getMethod();
		} catch (Exception ignored) {
			
		}
		return (ret == null ? sparql11protocol.getQuery().getMethod() : ret);
	}

	public String getQueryAcceptHeader(String id) {
		String ret= null;
		try {
			ret =  queries.get(id).sparql11protocol.getQuery().getFormat().getQueryAcceptHeader();
		} catch (Exception ignored) {
		}
		return (ret == null ? sparql11protocol.getQuery().getFormat().getQueryAcceptHeader(): ret);
	}

	public Set<String> getNamedGraphURI(String id) {
		try {
			return queries.get(id).graphs.named_graph_uri;
		} catch (Exception e) {
			if (graphs == null)
				return null;
			return graphs.named_graph_uri;
		}
	}

	public Set<String> getDefaultGraphURI(String id) {
		try {
			return queries.get(id).graphs.default_graph_uri;
		} catch (Exception e) {
			if (graphs == null)
				return null;
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
		queries.get(id).sparql11protocol.getQuery().setFormat(format);
	}

	public void setQueryMethod(String id, QueryHTTPMethod method) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.getQuery().setMethod(method);
	}

	public void setQueryProtocolScheme(String id, ProtocolScheme scheme) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setProtocol(scheme);
	}

	public void setQueryPath(String id, String path) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.getQuery().setPath(path);
	}

	public void setQueryPort(String id, int port) {
		if (queries.get(id) == null)
			return;
		queries.get(id).sparql11protocol.setPort(port);
	}

	public void setNamedGraphUri(String id, Set<String> graph) {
		if (queries.get(id) == null)
			return;
		queries.get(id).graphs.named_graph_uri = graph;
	}

	public void setDefaultGraphURI(String id, Set<String> graph) {
		if (queries.get(id) == null)
			return;
		queries.get(id).graphs.default_graph_uri = graph;
	}

	/*
	 * SUBSCRIBE
	 */
	public String getSubscribeHost() {
		String ret = sparql11seprotocol.getHost();
		if 	(ret == null) ret = host;
		return ret;
	}
	
	public String getSubscribeHost(String id) {
		String ret=null;
		try {
			ret = queries.get(id).sparql11seprotocol.getHost();
		} catch (Exception ignored) {
			
		}
		if(ret == null) {
			ret = sparql11seprotocol.getHost();
			if 	(ret == null) ret = host;
		}
		return ret;
	}

	public void setSubscribeHost(String id, String host) {
		if (queries.get(id) == null)
			return;
        queries.get(id).sparql11seprotocol.setHost(host);
	}

	public int getSubscribePort(String id) {
		int ret = -1;
		try {
			ret = queries.get(id).sparql11seprotocol.getAvailableProtocols()
					.get(queries.get(id).sparql11seprotocol.getProtocol()).getPort();
		} catch (Exception ignored) {
		}
		return (ret == -1 ? sparql11seprotocol.getPort() : ret);
	}

	public void setSubscribePort(String id, int port) {
		if (queries.get(id) == null)
			return;
        queries.get(id).sparql11seprotocol.getAvailableProtocols()
				.get(queries.get(id).sparql11seprotocol.getProtocol()).setPort(port);
	}

	public String getSubscribePath(String id) {
		String ret=null;
		try {
			ret=  queries.get(id).sparql11seprotocol.getAvailableProtocols()
					.get(queries.get(id).sparql11seprotocol.getProtocol()).getPath();
		} catch (Exception ignored) {
		}
		return (ret == null ? sparql11seprotocol.getPath() : ret);
	}

	public void setSubscribePath(String id, String path) {
		if (queries.get(id) == null)
			return;
        queries.get(id).sparql11seprotocol.getAvailableProtocols()
				.get(queries.get(id).sparql11seprotocol.getProtocol()).setPath(path);
	}

	public SubscriptionProtocolProperties getSubscribeProtocol(String id) {
		SubscriptionProtocolProperties ret = null;
		try {
			ret=  queries.get(id).sparql11seprotocol.getSubscriptionProtocol();
		} catch (Exception ignored) {
		}
		return (ret == null ? sparql11seprotocol.getSubscriptionProtocol() : ret);
	}
	
	public SubscriptionProtocolProperties getSubscribeProtocol() {
		return sparql11seprotocol.getSubscriptionProtocol();		
	}

	public void setSubscribeProtocol(String id, SubscriptionProtocolProperties sp) {
		if (queries.get(id) == null)
			return;
        queries.get(id).sparql11seprotocol.setProtocol(sp.getScheme());
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

				RDFTerm bindingValue;
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
					Logging.logger.error("JSAP unknown type: " + binding.getValue().type);
					continue;
				}

				ret.addBinding(binding.getKey(), bindingValue);
			}
		} catch (Exception e) {
			Logging.logger.error("getUpdateBindings " + id + " exception: " + e.getMessage());
		}

		return ret;
	}

	public MultipleForcedBindings getUpdateMultipleBindings(String id) throws IllegalArgumentException {
		if (updates.get(id) == null)
			throw new IllegalArgumentException("Update ID not found: " + id);

		MultipleForcedBindings ret = new MultipleForcedBindings();

		if (updates.get(id).forcedBindings == null)
			return ret;

		try {
			for (Entry<String, ForcedBinding> binding : updates.get(id).forcedBindings.entrySet()) {
				if (binding.getValue().type == null)
					continue;

				RDFTerm bindingValue;
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

				RDFTerm bindingValue;
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

		String scheme = queries.get(id).sparql11seprotocol.getAvailableProtocols()
				.get(queries.get(id).sparql11seprotocol.getProtocol()).getScheme();
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
		/*
		 * Direct Known Subclasses:
		 * 
		 * UpdateBinaryOp, UpdateCreate, UpdateData, UpdateDeleteWhere, UpdateDropClear,
		 * UpdateLoad, UpdateWithUsing
		 * 
		 */
		StringBuilder sb = new StringBuilder();

		try {
			UpdateRequest request = UpdateFactory.create();
			request.setPrefixMapping(prefixes);
			UpdateFactory.parse(request, replaceBindings(sparql, bindings));
			
			for (Update upd : request.getOperations()) {
				IndentedWriter writer = new IndentedLineBuffer();
				UpdateWriterVisitor visitor = new UpdateWriterVisitor(writer, new SerializationContext());
				if (!sb.toString().isEmpty()) sb.append(";");
				else sb.append(prologue);
				
				if (upd instanceof UpdateDataDelete)
					visitor.visit((UpdateDataDelete) upd);
				else if (upd instanceof UpdateDataInsert)
					visitor.visit((UpdateDataInsert) upd);
				else if (upd instanceof UpdateDeleteWhere)
					visitor.visit((UpdateDeleteWhere) upd);

				else if (upd instanceof UpdateDrop)
					visitor.visit((UpdateDrop) upd);
				else if (upd instanceof UpdateMove)
					visitor.visit((UpdateMove) upd);
				else if (upd instanceof UpdateClear)
					visitor.visit((UpdateClear) upd);
				else if (upd instanceof UpdateLoad)
					visitor.visit((UpdateLoad) upd);

				else if (upd instanceof UpdateAdd)
					visitor.visit((UpdateAdd) upd);
				else if (upd instanceof UpdateCopy)
					visitor.visit((UpdateCopy) upd);
				else if (upd instanceof UpdateCreate)
					visitor.visit((UpdateCreate) upd);
				else if (upd instanceof UpdateModify)
					visitor.visit((UpdateModify) upd);

				sb.append(writer);
			}
			
			
			
		} catch (QueryParseException ex) {
			sb.append(prologue).append(replaceBindings(sparql, bindings));
		}

		return sb.toString();
	}

	public String addPrefixesAndReplaceMultipleBindings(String sparql, ArrayList<Bindings> bindings)
			throws SEPABindingsException {
		return prologue + replaceMultipleBindings(sparql, bindings);
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

	public static String replaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
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

	public static String replaceMultipleBindings(String sparql, ArrayList<Bindings> multipleBindings)
			throws SEPABindingsException {
		long start = Timings.getTime();
		if (multipleBindings == null || sparql == null)
			return sparql;

		if (multipleBindings.isEmpty())
			return sparql;

        ArrayList<String> vars = new ArrayList<>(multipleBindings.get(0).getVariables());

		ArrayList<ArrayList<String>> allValues = new ArrayList<>();

		for (Bindings bindings : multipleBindings) {
			ArrayList<String> values = new ArrayList<>();
			for (String var : vars) {
				String value = bindings.getValue(var);
				if (value == null) {
					// https://www.w3.org/TR/sparql11-query/#inline-data
					// If a variable has no value for a particular solution in the VALUES clause,
					// the keyword UNDEF is used instead of an RDF term.
					values.add("UNDEF");
					continue;
				}

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

				values.add(value);
			}

			allValues.add(values);
		}

		String replacedSparql = String.format("%s", sparql);

		// VALUES (?book ?title)
		// { (UNDEF "SPARQL Tutorial")
		// (:book2 UNDEF)
		// }
		StringBuilder VARS = new StringBuilder();
		VARS.append("(");
		for (String var : vars) {
			VARS.append("?").append(var).append(" ");
		}
		VARS.append(")");
		StringBuilder VALUES = new StringBuilder();
		VALUES.append("{");
		for (ArrayList<String> values : allValues) {
			VALUES.append("(");
			for (String value : values) {
				VALUES.append(value).append(" ");
			}
			VALUES.append(")");
		}
		VALUES.append("}");

		int end = replacedSparql.lastIndexOf("}");

		String ret = replacedSparql.substring(0, end) + " VALUES " + VARS + VALUES + "}";
		long stop = Timings.getTime();
		Timings.log("replaceMultipleBindings", start, stop);
		return ret;
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
		sparql11seprotocol.setReconnect(b);
	}

}
