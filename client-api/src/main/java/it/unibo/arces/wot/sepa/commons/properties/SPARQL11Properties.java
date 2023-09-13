/*  This class describes the properties used to access a SPARQL 1.1 Protocol Service
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.commons.properties;

import java.io.*;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.properties.QueryProperties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.properties.QueryProperties.QueryResultsFormat;
import it.unibo.arces.wot.sepa.commons.properties.UpdateProperties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.properties.UpdateProperties.UpdateResultsFormat;
import it.unibo.arces.wot.sepa.logging.Logging;

/**
 * The Class SPARQL11Properties includes all the properties needed to connect to
 * a SPARQL 1.1 Protocol Service: the URLs used by queries and updates (scheme,
 * host, port and path), the HTTP method used by the primitives (GET, POST or
 * URL_ENCODED_POST) and the format of the results (JSON, XML, HTML, CSV). The
 * update result format is implementation specific. While for the query the
 * "formats" is the required return format, for the update it specifies the
 * format implemented by the SPARQL 1.1 Protocol service.
 * 
 * <pre>
 {
 	"host" : "localhost" ,
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
	"authentication": {
		"register": "https://localhost:8443/oauth/register",
		"tokenRequest": "https://localhost:8443/oauth/token",
		"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
		"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
		"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
		"expires": "04/5tRBT5n/VJ0XQASgs/w==",
		"type": "XPrHEX2xHy+5IuXHPHigMw=="
	},
	"graphs": {							(optional)
		"default_graph_uri": ["http://default"],
		"named_graph_uri": ["http://default"],
		"using_graph_uri": ["http://default"],
		"using_named_graph_uri": ["http://default"]
	},
}
 * </pre>
 */

public class SPARQL11Properties {
	// Members.
	protected String host;
	protected SPARQL11ProtocolProperties sparql11protocol;	
	protected GraphsProperties graphs = null;
	
	/** The defaults file name. */
	protected String defaultsFileName = "endpoint.jpar";

	/** The properties file. */
	protected String propertiesFile = defaultsFileName;
	
		
	/**
	 * The Enum SPARQLPrimitive (QUERY, UPDATE).
	 */
	public enum SPARQLPrimitive {
		/** The query. */
		QUERY,
		/** The update. */
		UPDATE
	};

	/**
	 * The Enum UpdateResultsFormat (HTTP,HTTPS).
	 */
	public enum ProtocolScheme {
		/** The http protocol scheme. */
		http("http"),
		/** The https protocol scheme. */
		https("https"),
		/* In memory JENA API*/
		jena_api("jena-api");
		
		private final String label;
		
		private ProtocolScheme(String value) {
			label = value;
		}

		public String getProtocolScheme() {
			return label;
		}
	};

	
	public SPARQL11Properties(String host,ProtocolScheme scheme) {
		this.host = host;
		this.sparql11protocol = new SPARQL11ProtocolProperties();
		this.sparql11protocol.protocol = scheme;
	}

	public SPARQL11Properties(String jsapFile) throws SEPAPropertiesException  {
		SPARQL11Properties jsap = null;
		if (jsapFile == null) throw new SEPAPropertiesException("File is null");
		try {
			FileReader reader = new FileReader(jsapFile);
			jsap = new Gson().fromJson(reader, SPARQL11Properties.class);
		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException  e) {
			Logging.logger.error(e.getMessage());
			e.printStackTrace();
			throw new SEPAPropertiesException(e);
		}
		
		this.host = jsap.host;
		this.sparql11protocol = jsap.sparql11protocol;
		this.graphs = jsap.graphs;
		
		propertiesFile = jsapFile;
	}

	public String getJSAPFilename() {
		return propertiesFile;
	}

	public String toString() {
		return new Gson().toJson(this);
	}

	/**
	 * Store properties.
	 *
	 * @param propertiesFile the properties file
	 * @throws SEPAPropertiesException
	 * @throws IOException             Signals that an I/O exception has occurred.
	 */
	protected void storeProperties(String propertiesFile) throws SEPAPropertiesException {
		FileWriter out;
		try {
			out = new FileWriter(propertiesFile);
			out.write(this.toString());
			out.close();
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}

	}

	/**
	 * Gets the host.
	 *
	 * @return the host (default is localhost)
	 */
	public String getHost() {
		if (sparql11protocol == null) return host;
		if (sparql11protocol.host == null) return host;
		return sparql11protocol.host;
	}

	/**
	 * Sets the host.
	 *
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the update port.
	 *
	 * @return the update port
	 */
	public int getPort() {
		return sparql11protocol.port;
	}

	/**
	 * Sets the update port.
	 *
	 * @return the update port
	 */
	public void setPort(int port) {
		sparql11protocol.port = port;
	}

	/**
	 * Gets the default graph URI.
	 * 
	 * <pre>
	"graphs": {							(optional)
		"default_graph_uri": ["http://default"],
		"named_graph_uri": ["http://default"],
		"using_graph_uri": ["http://default"],
		"using_named_graph_uri": ["http://default"]
	},
	 * </pre>
	 * 
	 * @return the default graph URI
	 */
	public Set<String> getDefaultGraphURI() {
		if (graphs == null) return null;
		return graphs.default_graph_uri;

	}

	public void setDefaultGraphURI(Set<String> graph) {
		if (graphs == null) graphs = new GraphsProperties();
		graphs.default_graph_uri.clear();
		graphs.default_graph_uri.addAll(graph);
	}

	/**
	 * Gets the named graph URI.
	 *
	 * <pre>
	"graphs": {							(optional)
		"default_graph_uri": ["http://default"],
		"named_graph_uri": ["http://default"],
		"using_graph_uri": ["http://default"],
		"using_named_graph_uri": ["http://default"]
	},
	 * </pre>
	 * 
	 * @return the default graph URI
	 */
	public Set<String> getNamedGraphURI() {
		if (graphs == null) return null;
		return graphs.named_graph_uri;
	}

	public void setNamedGraphURI(Set<String> graph) {
		if (graphs == null) graphs = new GraphsProperties();
		graphs.named_graph_uri.clear();
		graphs.named_graph_uri.addAll(graph);
	}

	/**
	 * Gets the using graph URI.
	 *
	 * <pre>
	"graphs": {							(optional)
		"default_graph_uri": ["http://default"],
		"named_graph_uri": ["http://default"],
		"using_graph_uri": ["http://default"],
		"using_named_graph_uri": ["http://default"]
	},
	 * </pre>
	 * 
	 * @return the default graph URI
	 */
	public Set<String> getUsingGraphURI() {
		if (graphs == null) return null;
		return graphs.using_graph_uri;
	}

	public void setUsingGraphURI(Set<String> graph) {
		if (graphs == null) graphs = new GraphsProperties();
		graphs.using_graph_uri.clear();
		graphs.using_named_graph_uri.addAll(graph);
	}

	/**
	 * Gets the using named graph URI.
	 *
	 * <pre>
	"graphs": {							(optional)
		"default_graph_uri": ["http://default"],
		"named_graph_uri": ["http://default"],
		"using_graph_uri": ["http://default"],
		"using_named_graph_uri": ["http://default"]
	},
	 * </pre>
	 * 
	 * @return the default graph URI
	 */
	public Set<String> getUsingNamedGraphURI() {
		if (graphs == null) return null;
		return graphs.using_named_graph_uri;
	}

	public void setUsingNamedGraphURI(Set<String> graph) {
		if (graphs == null) graphs = new GraphsProperties();
		graphs.using_named_graph_uri.clear();
		graphs.using_named_graph_uri.addAll(graph);
	}

	/**
	 * Gets the update path.
	 *
	 * @return the update path (default is /sparql)
	 */
	public String getUpdatePath() {
		return sparql11protocol.update.path;
	}

	public void setUpdatePath(String path) {
		sparql11protocol.update.path = path;
	}

	/**
	 * Gets the update method.
	 *
	 * @return the update method (POST, URL_ENCODED_POST)
	 * 
	 * @see QueryHTTPMethod
	 */
	public UpdateHTTPMethod getUpdateMethod() {
		return sparql11protocol.update.method;
	}

	public void setUpdateMethod(UpdateHTTPMethod method) {
		sparql11protocol.update.method = method;
	}

	/**
	 * Gets the update HTTP Accept header
	 *
	 * @return the update HTTP Accept header string
	 */
	public String getUpdateAcceptHeader() {
		return sparql11protocol.update.format.getUpdateAcceptHeader();
	}

	public void setUpdateAcceptHeader(UpdateResultsFormat format) {
		sparql11protocol.update.format = format;
	}
	
	/**
	 * Gets the query path.
	 *
	 * @return the query path (default is /sparql)
	 */
	public String getQueryPath() {
		return sparql11protocol.query.path;
	}

	public void setQueryPath(String path) {
		sparql11protocol.query.path = path;
	}

	/**
	 * Gets the query method.
	 *
	 * @return the query method (POST, URL_ENCODED_POST)
	 * 
	 * @see QueryHTTPMethod
	 */
	public QueryHTTPMethod getQueryMethod() {
		return sparql11protocol.query.method;
	}

	public void setQueryMethod(QueryHTTPMethod method) {
		sparql11protocol.query.method = method;
	}

	/**
	 * Gets the query HTTP Accept header string
	 *
	 * @return the query HTTP Accept header string
	 * 
	 */
	public String getQueryAcceptHeader() {
		return sparql11protocol.query.format.getQueryAcceptHeader();
	}

	public void setQueryAcceptHeader(QueryResultsFormat format) {
		sparql11protocol.query.format = format;
	}
	
	public String getUpdateContentTypeHeader() {
		return sparql11protocol.update.method.getUpdateContentTypeHeader();
	}

	public String getQueryContentTypeHeader() {
		return sparql11protocol.query.method.getQueryContentTypeHeader();
	}

	public String getProtocolScheme() {
		return (sparql11protocol.protocol == null ? null : sparql11protocol.protocol.getProtocolScheme());
	}

	public void setProtocolScheme(ProtocolScheme scheme) {
		sparql11protocol.protocol = scheme;
	}

	

	
}
