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

package com.vaimee.sepa.commons.properties;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.properties.QueryProperties.QueryHTTPMethod;
import com.vaimee.sepa.commons.properties.QueryProperties.QueryResultsFormat;
import com.vaimee.sepa.commons.properties.UpdateProperties.UpdateHTTPMethod;
import com.vaimee.sepa.commons.properties.UpdateProperties.UpdateResultsFormat;
import com.vaimee.sepa.logging.Logging;

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
	protected SPARQL11ProtocolProperties sparql11protocol;
	protected GraphsProperties graphs = null;

	/**
	 * The Enum SPARQLPrimitive (QUERY, UPDATE).
	 */
	public enum SPARQLPrimitive {
		/** The query. */
		QUERY,
		/** The update. */
		UPDATE
	}

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
		
		ProtocolScheme(String value) {
			label = value;
		}

		public String getProtocolScheme() {
			return label;
		}
	}

	public SPARQL11Properties() throws SEPAPropertiesException {
		this((String[]) null);
	}

	public SPARQL11Properties(String uri) throws SEPAPropertiesException {
		this(URI.create(uri));
	}

	public SPARQL11Properties(String uri,String[] args) throws SEPAPropertiesException {
		this(URI.create(uri),args);
	}

	public SPARQL11Properties(String[] args) throws SEPAPropertiesException {
		this((URI) null,args);
	}

	public SPARQL11Properties(URI uri) throws SEPAPropertiesException {
		this(uri,null);
	}
	public SPARQL11Properties(URI uri,String[] args) throws SEPAPropertiesException {
		if (uri != null) {
			Reader in = getReaderFromUri(uri);

			SPARQL11Properties jsap;
			try {
				jsap = new Gson().fromJson(in, SPARQL11Properties.class);
			} catch (JsonSyntaxException | JsonIOException  e) {
				Logging.logger.error(e.getMessage());
				throw new SEPAPropertiesException(e);
			}

			this.sparql11protocol = jsap.sparql11protocol;
			this.graphs = jsap.graphs;

			try {
				in.close();
			} catch (IOException e) {
				throw new SEPAPropertiesException(e);
			}
		}
		else sparql11protocol = new SPARQL11ProtocolProperties();

		Map<String, String> envs = System.getenv();
		for(String var : envs.keySet()) {
			Logging.logger.trace("Environmental variable "+var+" : "+envs.get(var));
			setParameter("-"+var, envs.get(var));
		}

		if (args != null)
			// Setting values: -key=value
			for (int i = 0; i < args.length; i++) {
				Logging.logger.trace("Argument  "+args[i]);
				String[] params = args[i].split("=");
				if (params.length == 2) {
					setParameter(params[0], params[1]);
				}
			}
	}

	protected void setParameter(String key,String value) {
		switch (key) {
			case "-sparql11protocol.port":
				sparql11protocol.setPort(Integer.parseInt(value));
				break;
			case "-sparql11protocol.host":
				sparql11protocol.setHost(value);
				break;
			case "-sparql11protocol.protocol":
				sparql11protocol.setProtocol((Objects.equals(value, "http") ? ProtocolScheme.http : ProtocolScheme.https));
				break;
			case "-sparql11protocol.update.method":
				sparql11protocol.getUpdate().setMethod((Objects.equals(value, "post") ? UpdateHTTPMethod.POST : UpdateHTTPMethod.URL_ENCODED_POST));
				break;
			case "-sparql11protocol.update.format":
				sparql11protocol.getUpdate().setFormat((Objects.equals(value, "json") ? UpdateResultsFormat.JSON : UpdateResultsFormat.HTML));
				break;
			case "-sparql11protocol.update.path":
				sparql11protocol.getUpdate().setPath(value);
				break;
			case "-sparql11protocol.query.method":
				sparql11protocol.getQuery().setMethod((Objects.equals(value, "get") ? QueryHTTPMethod.GET : (Objects.equals(value, "post") ? QueryHTTPMethod.POST : QueryHTTPMethod.URL_ENCODED_POST)));
				break;
			case "-sparql11protocol.query.format":
				sparql11protocol.getQuery().setFormat((Objects.equals(value, "json") ? QueryResultsFormat.JSON : (Objects.equals(value, "xml") ? QueryResultsFormat.XML : QueryResultsFormat.CSV)));
				break;
			case "-sparql11protocol.query.path":
				sparql11protocol.getQuery().setPath(value);
				break;
		}
	}

	/*
	* Applications working with file paths and file URIs should take great care
	* to use the appropriate methods to convert between the two.
	* The Path.of(URI) factory method and the File.File(URI) constructor
	* can be used to create Path or File objects from a file URI.
	* Path.toUri() and File.toURI() can be used to create a URI from a file path,
	* which can be converted to URL using URI.toURL(). Applications should never
	* try to construct or parse a URL from the direct string representation
	* of a File or Path instance
	* */
	protected Reader getReaderFromUri(URI uri) throws SEPAPropertiesException {
		Reader in;

		Logging.logger.trace("Get reader from URI, trying STREAM: "+uri);
		try {
			in = new BufferedReader(
					new InputStreamReader(uri.toURL().openStream()));
			Logging.logger.trace("Success");
		} catch (IOException | IllegalArgumentException e) {
			Logging.logger.trace("Failed to get input stream: "+e.getMessage());
			try {
				Logging.logger.trace("Get reader from URI, trying FILE: "+uri);
				in = new FileReader(Path.of(uri.toASCIIString()).toFile());
				Logging.logger.trace("Success");
			} catch (FileNotFoundException ex) {
				Logging.logger.trace("Failed to get file reader: "+ex.getMessage());
				Logging.logger.trace("Get reader from URI, trying RESOURCE: "+uri);
				if (getClass().getClassLoader().getResourceAsStream(uri.toASCIIString()) == null) {
					Logging.logger.trace("Resource not found: "+uri);
					throw new SEPAPropertiesException("Resource not found: "+uri);
				}
				in = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(uri.toASCIIString())));
				Logging.logger.trace("Success");
			}
		}

		return in;
	}

	public String toString() {
		return new Gson().toJson(this);
	}

	/**
	 * Gets the host.
	 *
	 * @return the host (default is localhost)
	 */
	public String getHost() {
		return sparql11protocol.getHost();
	}

	/**
	 * Sets the host.
	 *
	 */
	public void setHost(String host) {
		sparql11protocol.setHost(host);
	}

	/**
	 * Gets the update port.
	 *
	 * @return the update port
	 */
	public int getPort() {
		return sparql11protocol.getPort();
	}

	/**
	 * Sets the update port.
	 **/
	public void setPort(int port) {
		sparql11protocol.setPort(port);
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
		return sparql11protocol.getUpdate().getPath();
	}

	public void setUpdatePath(String path) {
		sparql11protocol.getUpdate().setPath(path);
	}

	/**
	 * Gets the update method.
	 *
	 * @return the update method (POST, URL_ENCODED_POST)
	 * 
	 * @see QueryHTTPMethod
	 */
	public UpdateHTTPMethod getUpdateMethod() {
		return sparql11protocol.getUpdate().getMethod();
	}

	public void setUpdateMethod(UpdateHTTPMethod method) {
		sparql11protocol.getUpdate().setMethod(method);
	}

	/**
	 * Gets the update HTTP Accept header
	 *
	 * @return the update HTTP Accept header string
	 */
	public String getUpdateAcceptHeader() {
		return sparql11protocol.getUpdate().getFormat().getUpdateAcceptHeader();
	}

	public void setUpdateAcceptHeader(UpdateResultsFormat format) {
		sparql11protocol.getUpdate().setFormat(format);
	}
	
	/**
	 * Gets the query path.
	 *
	 * @return the query path (default is /sparql)
	 */
	public String getQueryPath() {
		return sparql11protocol.getQuery().getPath();
	}

	public void setQueryPath(String path) {
		sparql11protocol.getQuery().setPath(path);
	}

	/**
	 * Gets the query method.
	 *
	 * @return the query method (POST, URL_ENCODED_POST)
	 * 
	 * @see QueryHTTPMethod
	 */
	public QueryHTTPMethod getQueryMethod() {
		return sparql11protocol.getQuery().getMethod();
	}

	public void setQueryMethod(QueryHTTPMethod method) {
		sparql11protocol.getQuery().setMethod(method);
	}

	/**
	 * Gets the query HTTP Accept header string
	 *
	 * @return the query HTTP Accept header string
	 * 
	 */
	public String getQueryAcceptHeader() {
		return sparql11protocol.getQuery().getFormat().getQueryAcceptHeader();
	}

	public void setQueryAcceptHeader(QueryResultsFormat format) {
		sparql11protocol.getQuery().setFormat(format);
	}
	
	public String getUpdateContentTypeHeader() {
		return sparql11protocol.getUpdate().getMethod().getUpdateContentTypeHeader();
	}

	public String getQueryContentTypeHeader() {
		return sparql11protocol.getQuery().getMethod().getQueryContentTypeHeader();
	}

	public String getProtocolScheme() {
		return (sparql11protocol.getProtocol() == null ? null : sparql11protocol.getProtocol().getProtocolScheme());
	}

	public void setProtocolScheme(ProtocolScheme scheme) {
		sparql11protocol.setProtocol(scheme);
	}

	

	
}
