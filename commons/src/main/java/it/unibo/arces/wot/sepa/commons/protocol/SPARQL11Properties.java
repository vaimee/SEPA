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

import java.io.FileReader;
import java.io.FileWriter;

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
 * update result format is implementation specific. While for the query the "formats"
 * is the required return format, for the update it specifies the 
 * format implemented by the SPARQL 1.1 Protocol service.
 * 
 * 
 * <pre>
 {
		"parameters" : {
			"host" : "localhost" ,
			"ports" : {
				"http" : 9999}
			 ,
			"paths" : {
				"query" : "/blazegraph/namespace/kb/sparql" ,
				"update" : "/blazegraph/namespace/kb/sparql"}
			 ,
			"methods" : {
				"query" : "GET|POST|URL_ENCODED_POST" ,
				"update" : "POST|URL_ENCODED_POST"}
			 ,
			"formats" : {
				"query" : "JSON|XML|CSV" ,
				"update" : "HTML"}
		}
	}
 * </pre>
 */

public class SPARQL11Properties {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11ProtocolProperties");

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
	protected String defaultsFileName = "defaults.jpar";

	/** The properties file. */
	protected String propertiesFile = "endpoint.jpar";

	/** The parameters. */
	protected JsonObject parameters = new JsonObject();

	/** The doc. */
	protected JsonObject doc = new JsonObject();

	public SPARQL11Properties(String propertiesFile) throws SEPAPropertiesException {
		this.propertiesFile = propertiesFile;

		try {
			FileReader in = new FileReader(propertiesFile);

			doc = new JsonParser().parse(in).getAsJsonObject();
			if (doc.get("parameters") == null) {
				logger.warn("parameters key is missing");
				throw new SEPAPropertiesException(new Exception("parameters key is missing"));
			}
			parameters = doc.get("parameters").getAsJsonObject();

			// Validate the JSON elements
			validate();

			in.close();

		} catch (Exception e) {

			logger.warn(e.getMessage());

			defaults();

			try {
				storeProperties(defaultsFileName);
			} catch (Exception e1) {
				throw new SEPAPropertiesException(e1);
			}

			logger.warn("USING DEFAULTS. Edit \"" + defaultsFileName + "\" and rename it to\"" + propertiesFile + "\"");

			throw new SEPAPropertiesException(new Exception(
					"USING DEFAULTS. Edit \"" + defaultsFileName + "\" and rename it to\"" + propertiesFile + "\""));
		}
	}

	public String toString() {
		return doc.toString();
	}

	/**
	 * <pre>
 {
		"parameters" : {
			"host" : "localhost" ,
			"ports" : {
				"http" : 9999}
			 ,
			"paths" : {
				"query" : "/blazegraph/namespace/kb/sparql" ,
				"update" : "/blazegraph/namespace/kb/sparql"}
			 ,
			"methods" : {
				"query" : "GET|POST|URL_ENCODED_POST" ,
				"update" : "POST|URL_ENCODED_POST"}
			 ,
			"formats" : {
				"query" : "JSON|XML|CSV" ,
				"update" : "HTML"}
		}
	}
 * </pre>
	 */
	protected void defaults() {
		parameters.add("host", new JsonPrimitive("localhost"));

		JsonObject ports = new JsonObject();
		ports.add("http", new JsonPrimitive(9999));
		parameters.add("ports", ports);

		JsonObject paths = new JsonObject();
		paths.add("query", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		paths.add("update", new JsonPrimitive("/blazegraph/namespace/kb/sparql"));
		parameters.add("paths", paths);

		JsonObject methods = new JsonObject();
		methods.add("query", new JsonPrimitive("POST"));
		methods.add("update", new JsonPrimitive("URL_ENCODED_POST"));
		parameters.add("methods", methods);

		JsonObject formats = new JsonObject();
		formats.add("query", new JsonPrimitive("JSON"));
		formats.add("update", new JsonPrimitive("HTML"));
		parameters.add("formats", formats);

		doc.add("parameters", parameters);
	}

	protected void validate() throws SEPAPropertiesException {
		try {
			parameters.get("host").getAsString();

			parameters.get("ports").getAsJsonObject().get("http").getAsInt();

			parameters.get("paths").getAsJsonObject().get("update").getAsString();
			parameters.get("paths").getAsJsonObject().get("query").getAsString();

			parameters.get("methods").getAsJsonObject().get("update").getAsString();
			parameters.get("methods").getAsJsonObject().get("query").getAsString();

			parameters.get("formats").getAsJsonObject().get("update").getAsString();
			parameters.get("formats").getAsJsonObject().get("query").getAsString();
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
			out.write(doc.toString());
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
		return parameters.get("host").getAsString();
	}

	/**
	 * Gets the update port.
	 *
	 * @return the update port (default is 9999)
	 */
	public int getHttpPort() {
		return parameters.get("ports").getAsJsonObject().get("http").getAsInt();
	}

	/**
	 * Gets the update path.
	 *
	 * @return the update path (default is /blazegraph/namespace/kb/sparql)
	 */
	public String getUpdatePath() {
		return parameters.get("paths").getAsJsonObject().get("update").getAsString();
	}

	/**
	 * Gets the update method.
	 *
	 * @return the update method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getUpdateMethod() {
		switch (parameters.get("methods").getAsJsonObject().get("update").getAsString()) {
		case "POST":
			return HTTPMethod.POST;
		case "URL_ENCODED_POST":
			return HTTPMethod.URL_ENCODED_POST;
		default:
			return HTTPMethod.POST;
		}
	}

	/**
	 * Gets the update HTTP Accept header
	 *
	 * @return the update HTTP Accept header string
	 */
	public String getUpdateAcceptHeader() {
		switch (parameters.get("formats").getAsJsonObject().get("update").getAsString()) {
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
	 * @return the query path (default is /blazegraph/namespace/kb/sparql)
	 */
	public String getQueryPath() {
		return parameters.get("paths").getAsJsonObject().get("query").getAsString();
	}

	/**
	 * Gets the query method.
	 *
	 * @return the query method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getQueryMethod() {
		switch (parameters.get("methods").getAsJsonObject().get("query").getAsString()) {
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
		switch (parameters.get("formats").getAsJsonObject().get("query").getAsString()) {
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
		switch (parameters.get("methods").getAsJsonObject().get("update").getAsString()) {
		case "POST":
			return "application/sparql-update";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-update";
		}
	}

	public String getQueryContentTypeHeader() {
		switch (parameters.get("methods").getAsJsonObject().get("query").getAsString()) {
		case "POST":
			return "application/sparql-query";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-query";
		}
	}
}
