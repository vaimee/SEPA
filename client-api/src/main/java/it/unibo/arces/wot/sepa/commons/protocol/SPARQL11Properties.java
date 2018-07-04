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

package it.unibo.arces.wot.sepa.commons.protocol;

import java.io.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;

/**
 * The Class SPARQL11Properties includes all the properties needed to connect to
 * a SPARQL 1.1 Protocol Service: the URLs used by queries and updates (scheme,
 * host, port and path), the HTTP method used by the primitives (GET, POST or
 * URL_ENCODED_POST) and the format of the results (JSON, XML, HTML, CSV). The
 * update result format is implementation specific. While for the query the
 * "formats" is the required return format, for the update it specifies the
 * format implemented by the SPARQL 1.1 Protocol service.
 * 
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
		},
		"authentication": {
			"basic": {
				"user": "admin",
				"pass": "admin"
			}
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
		"default-graph-uri": "http://default",
		"named-graph-uri": "http://default",
		"using-graph-uri": "http://default",
		"using-named-graph-uri": "http://default"
	},
}
 * </pre>
 */

public class SPARQL11Properties {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

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
	 * The Enum HTTPMethod (GET,POST,URL_ENCODED_POST).
	 */
	public enum HTTPMethod {
		/** The get. */
		GET,
		/** The post. */
		POST,
		/** The url encoded post. */
		URL_ENCODED_POST
	};

	/**
	 * The Enum QueryResultsFormat (JSON,XML,CSV).
	 */
	public enum QueryResultsFormat {
		/** The json. */
		JSON,
		/** The xml. */
		XML,
		/** The csv. */
		CSV
	};

	/**
	 * The Enum UpdateResultsFormat (JSON,HTML).
	 */
	public enum UpdateResultsFormat {
		/** The html. */
		HTML,
		/** The json. */
		JSON
	};

	/** The defaults file name. */
	protected String defaultsFileName = "endpoint.jpar";

	/** The properties file. */
	protected String propertiesFile = defaultsFileName;

	/** The parameters. */
	protected JsonObject jsap = new JsonObject();
	
	public SPARQL11Properties(String propertiesFile) throws SEPAPropertiesException {
		loadProperties(propertiesFile);
	}

	public SPARQL11Properties(String propertiesFile, byte[] secret) throws SEPAPropertiesException {
		this(propertiesFile);
	}
	
	private void loadProperties(String jsapFile) throws SEPAPropertiesException {
		try (final FileReader in = new FileReader(jsapFile)) {
			jsap = new JsonParser().parse(in).getAsJsonObject();

			// Validate the JSON elements
			validate();
			this.propertiesFile = jsapFile;
		} catch (Exception e) {

			logger.warn(e.getMessage());

			defaults();

			try {
				storeProperties(defaultsFileName);
			} catch (Exception e1) {
				throw new SEPAPropertiesException(e1);
			}

			logger.warn("USING DEFAULTS. Edit \"" + defaultsFileName + "\" (if needed)");
			
			this.propertiesFile = defaultsFileName;
		}
	}

	public String getFilename() {
		return propertiesFile;
	}
	
	public String toString() {
		return jsap.get("sparql11protocol").toString();
	}

	/**
	 * <pre>
	{
	"host" : "localhost" ,
	"sparql11protocol": {
		"protocol": "http",
		"port": 8000,
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
	}
	}
	 * </pre>
	 */
	protected void defaults() {
		jsap.add("host", new JsonPrimitive("localhost"));

		JsonObject sparql11protocol = new JsonObject();
		sparql11protocol.add("protocol", new JsonPrimitive("http"));
		sparql11protocol.add("port", new JsonPrimitive(9999));

		JsonObject query = new JsonObject();
		query.add("path", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		query.add("method", new JsonPrimitive("POST"));
		query.add("format", new JsonPrimitive("JSON"));
		sparql11protocol.add("query", query);

		JsonObject update = new JsonObject();
		update.add("path", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		update.add("method", new JsonPrimitive("POST"));
		update.add("format", new JsonPrimitive("JSON"));
		sparql11protocol.add("update", update);

		jsap.add("sparql11protocol", sparql11protocol);
	}

	protected void validate() throws SEPAPropertiesException {
		try {
			jsap.get("host").getAsString();

			jsap.get("sparql11protocol").getAsJsonObject().get("protocol").getAsString();

			jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("path").getAsString();
			jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("method").getAsString();
			jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("format").getAsString();

			jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("path").getAsString();
			jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("method").getAsString();
			jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("format").getAsString();
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}

	}

	/**
	 * Store properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws SEPAPropertiesException
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected void storeProperties(String propertiesFile) throws SEPAPropertiesException {
		FileWriter out;
		try {
			out = new FileWriter(propertiesFile);
			out.write(jsap.toString());
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
	public String getDefaultHost() {
		try {
			return jsap.get("sparql11protocol").getAsJsonObject().get("host").getAsString();
		}
		catch(Exception e) {
			try {
				return jsap.get("host").getAsString();
			}
			catch(Exception e1) {
				return null;
			}
		}
	}

	/**
	 * Gets the update port.
	 *
	 * @return the update port
	 */
	public int getDefaultPort() {
		try {
			return jsap.get("sparql11protocol").getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Gets the default graph URI.
<pre>
"graphs": { 
	"default-graph-uri": "http://default", 
	"named-graph-uri": "http://default", 
	"using-graph-uri": "http://default",
	"using-named-graph-uri": "http://default"
}
</pre> 
	 * @return the default graph URI
	 */
	public String getDefaultGraphURI() {
		try {
			return jsap.get("graphs").getAsJsonObject().get("default-graph-uri").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets the named graph URI.
	 *
<pre>
"graphs": { 
	"default-graph-uri": "http://default", 
	"named-graph-uri": "http://default", 
	"using-graph-uri": "http://default",
	"using-named-graph-uri": "http://default"
}
</pre> 
	 * @return the default graph URI
	 */
	public String getNamedGraphURI() {
		try {
			return jsap.get("graphs").getAsJsonObject().get("named-graph-uri").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets the using graph URI.
	 *
<pre>
"graphs": { 
	"default-graph-uri": "http://default", 
	"named-graph-uri": "http://default", 
	"using-graph-uri": "http://default",
	"using-named-graph-uri": "http://default"
}
</pre> 
	 * @return the default graph URI
	 */
	public String getUsingGraphURI() {
		try {
			return jsap.get("graphs").getAsJsonObject().get("using-graph-uri").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets the using named graph URI.
	 *
<pre>
"graphs": { 
	"default-graph-uri": "http://default", 
	"named-graph-uri": "http://default", 
	"using-graph-uri": "http://default",
	"using-named-graph-uri": "http://default"
}
</pre> 
	 * @return the default graph URI
	 */
	public String getUsingNamedGraphURI() {
		try {
			return jsap.get("graphs").getAsJsonObject().get("using-named-graph-uri").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets the update path.
	 *
	 * @return the update path (default is /update)
	 */
	public String getUpdatePath() {
		return jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("path").getAsString();
	}

	/**
	 * Gets the update method.
	 *
	 * @return the update method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getUpdateMethod() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("method")
				.getAsString()) {
		case "POST":
			return HTTPMethod.POST;
		case "URL_ENCODED_POST":
			return HTTPMethod.URL_ENCODED_POST;
		default:
			return HTTPMethod.GET;
		}
	}

	/**
	 * Gets the update HTTP Accept header
	 *
	 * @return the update HTTP Accept header string
	 */
	public String getUpdateAcceptHeader() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("format")
				.getAsString()) {
		case "JSON":
			return "application/json";
		case "HTML":
			return "application/html";
		default:
			return "application/json";
		}
	}

	/**
	 * Gets the query path.
	 *
	 * @return the query path (default is /query)
	 */
	public String getDefaultQueryPath() {
		return jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("path").getAsString();
	}

	/**
	 * Gets the query method.
	 *
	 * @return the query method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getQueryMethod() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("method")
				.getAsString()) {
		case "POST":
			return HTTPMethod.POST;
		case "GET":
			return HTTPMethod.GET;
		case "URL_ENCODED_POST":
			return HTTPMethod.URL_ENCODED_POST;
		default:
			return HTTPMethod.POST;
		}
	}

	/**
	 * Gets the query HTTP Accept header string
	 *
	 * @return the query HTTP Accept header string
	 * 
	 */
	public String getQueryAcceptHeader() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("format")
				.getAsString()) {
		case "JSON":
			return "application/sparql-results+json";
		case "XML":
			return "application/sparql-results+xml";
		case "CSV":
			return "text/csv";
		default:
			return "application/sparql-results+json";
		}
	}

	public String getUpdateContentTypeHeader() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("update").getAsJsonObject().get("method")
				.getAsString()) {
		case "POST":
			return "application/sparql-update";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-update";
		}
	}

	public String getQueryContentTypeHeader() {
		switch (jsap.get("sparql11protocol").getAsJsonObject().get("query").getAsJsonObject().get("method")
				.getAsString()) {
		case "POST":
			return "application/sparql-query";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-query";
		}
	}

	public String getDefaultProtocolScheme() {
		return jsap.get("sparql11protocol").getAsJsonObject().get("protocol").getAsString();
	}
}
