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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

// TODO: Auto-generated Javadoc
/**
 * The Class SPARQL11Properties includes all the properties needed to connect to
 * a SPARQL 1.1 Protocol Service: the URLs used by queries and updates (scheme,
 * host, port and path), the HTTP method used by the primitives (GET, POST or
 * URL_ENCODED_POST) and the format of the results (JSON, XML, HTML, CSV)
 * 
 * 
 * <pre>
 { parameters": 
   { "host": "localhost", 
     "ports":{ "http" : 9999 }, 
     "paths": {
       "update" : "/blazegraph/namespace/kb/sparql",
       "query" : "/blazegraph/namespace/kb/sparql" }, 
     "methods": { 
       "query": "POST", 
       "update": "URL_ENCODED_POST" }, 
     "formats" : { 
       "update" : "HTML", 
       "query" : "JSON" } } }
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
	// Properties file
	protected String defaultsFileName = "defaults.jpar";

	/** The properties file. */
	protected String propertiesFile = "endpoint.jpar";

	/** The parameters. */
	protected JsonObject parameters = new JsonObject();

	/** The doc. */
	protected JsonObject doc = new JsonObject();

	/** The host. */
	private String host;

	/** The http port. */
	private int httpPort;

	/** The update path. */
	private String updatePath;

	/** The query path. */
	private String queryPath;

	/** The update method. */
	private String updateMethod;

	/** The update results format. */
	private String updateResultsFormat;

	/** The query method. */
	private String queryMethod;

	/** The query results format. */
	private String queryResultsFormat;

	/**
	 * Instantiates a new SPARQL 11 properties.
	 *
	 * @param propertiesFile
	 *            the properties file (e.g., endpoint.jpar)
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws NoSuchElementException
	 *             the no such element exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NullPointerException
	 *             the null pointer exception
	 * @throws ClassCastException
	 *             the class cast exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 * @throws IllegalBlockSizeException
	 *             the illegal block size exception
	 * @throws BadPaddingException
	 *             the bad padding exception
	 */
	public SPARQL11Properties(String propertiesFile) throws FileNotFoundException, NoSuchElementException, IOException,
			NumberFormatException, InvalidKeyException, NullPointerException, ClassCastException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		this.propertiesFile = propertiesFile;

		loadProperties();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return doc.toString();
	}

	/**
	 * Defaults.
	 * 
	 * { parameters": { "host": "localhost", "ports":{ "http" : 9999 }, "paths":
	 * { "update" : "/blazegraph/namespace/kb/sparql", "query" :
	 * "/blazegraph/namespace/kb/sparql" }, "methods": { "query": "POST",
	 * "update": "URL_ENCODED_POST" }, "formats" : { "update" : "HTML", "query"
	 * : "JSON" } } }
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

	/**
	 * Load properties.
	 *
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws NoSuchElementException
	 *             the no such element exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NullPointerException
	 *             the null pointer exception
	 * @throws ClassCastException
	 *             the class cast exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 * @throws IllegalBlockSizeException
	 *             the illegal block size exception
	 * @throws BadPaddingException
	 *             the bad padding exception
	 */
	protected void loadProperties() throws FileNotFoundException, NoSuchElementException, IOException,
			NumberFormatException, InvalidKeyException, NullPointerException, ClassCastException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		FileReader in = null;
		try {
			in = new FileReader(propertiesFile);
			if (in != null) {
				doc = new JsonParser().parse(in).getAsJsonObject();
				if (doc.get("parameters") == null) {
					logger.warn("parameters key is missing");
					throw new NoSuchElementException("parameters key is missing");
				}
				parameters = doc.get("parameters").getAsJsonObject();

				setParameters();
			}
			if (in != null)
				in.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());

			defaults();

			storeProperties(defaultsFileName);

			logger.warn(
					"USING DEFAULTS. Edit \"" + defaultsFileName + "\" and rename it to \"" + propertiesFile + "\"");

			throw new FileNotFoundException(
					"USING DEFAULTS. Edit \"" + defaultsFileName + "\" and rename it to \"" + propertiesFile + "\"");
		}
	}

	/**
	 * Sets the parameters.
	 *
	 * @throws NullPointerException
	 *             the null pointer exception
	 * @throws ClassCastException
	 *             the class cast exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 * @throws IllegalBlockSizeException
	 *             the illegal block size exception
	 * @throws BadPaddingException
	 *             the bad padding exception
	 */
	protected void setParameters()
			throws NullPointerException, ClassCastException, IOException, NumberFormatException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		host = parameters.get("host").getAsString();

		for (Entry<String, JsonElement> elem : parameters.get("ports").getAsJsonObject().entrySet()) {
			if (elem.getKey().equals("http"))
				httpPort = elem.getValue().getAsInt();
		}
		for (Entry<String, JsonElement> elem : parameters.get("paths").getAsJsonObject().entrySet()) {
			if (elem.getKey().equals("update"))
				updatePath = elem.getValue().getAsString();
			if (elem.getKey().equals("query"))
				queryPath = elem.getValue().getAsString();
		}
		if (parameters.get("methods") != null) {
			for (Entry<String, JsonElement> elem : parameters.get("methods").getAsJsonObject().entrySet()) {
				if (elem.getKey().equals("update"))
					updateMethod = elem.getValue().getAsString().toUpperCase();
				if (elem.getKey().equals("query"))
					queryMethod = elem.getValue().getAsString().toUpperCase();
			}
		}
		else {
			updateMethod = "POST";
			queryMethod = "POST";
		}
		if (parameters.get("formats") != null) {
			for (Entry<String, JsonElement> elem : parameters.get("formats").getAsJsonObject().entrySet()) {
				if (elem.getKey().equals("update"))
					updateResultsFormat = elem.getValue().getAsString().toUpperCase();
				if (elem.getKey().equals("query"))
					queryResultsFormat = elem.getValue().getAsString().toUpperCase();
			}
		}
		else {
			updateResultsFormat = "JSON";
			queryResultsFormat = "JSON";
		}
	}

	/**
	 * Store properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected void storeProperties(String propertiesFile) throws IOException {
		FileWriter out = new FileWriter(propertiesFile);
		out.write(doc.toString());
		out.close();
	}

	/**
	 * Gets the host.
	 *
	 * @return the host (default is localhost)
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Gets the update port.
	 *
	 * @return the update port (default is 9999)
	 */
	public int getHttpPort() {
		return httpPort;
	}

	/**
	 * Gets the update path.
	 *
	 * @return the update path (default is /blazegraph/namespace/kb/sparql)
	 */
	public String getUpdatePath() {
		return updatePath;
	}

	/**
	 * Gets the update method.
	 *
	 * @return the update method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getUpdateMethod() {
		switch (updateMethod) {
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
		switch (updateResultsFormat) {
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
		return queryPath;
	}

	/**
	 * Gets the query method.
	 *
	 * @return the query method (POST, URL_ENCODED_POST)
	 * 
	 * @see HTTPMethod
	 */
	public HTTPMethod getQueryMethod() {
		switch (queryMethod) {
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

	 */
	public String getQueryAcceptHeader() {
		switch (queryResultsFormat) {
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
		switch (updateMethod) {
		case "POST":
			return "application/sparql-update";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-update";
		}
	}

	public String getQueryContentTypeHeader() {
		switch (queryMethod) {
		case "POST":
			return "application/sparql-query";
		case "URL_ENCODED_POST":
			return "application/x-www-form-urlencoded";
		default:
			return "application/sparql-query";
		}
	}

}
