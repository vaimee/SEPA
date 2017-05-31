/* This class implements the configuration properties of the Semantic Event Processing Architecture (SEPA) Engine
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
package it.unibo.arces.wot.sepa.engine.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.logging.log4j.LogManager;

/**
 * {"parameters":{ "timeouts":{ "scheduling":0, "queuesize":1000,
 * "keepalive":5000, "http":5000}, "path":"/sparql", "scheme":"http",
 * "port":8000, "securequery":{"port":8443,"scheme":"https"},
 * "secureupdate":{"port":8443,"scheme":"https"},
 * "subscribe":{"scheme":"ws","port":9000},
 * "securesubscribe":{"scheme":"wss","port":9443,"path":"/secure/sparql"},
 * "authorizationserver":{ "port":8443, "scheme":"https",
 * "register":"/oauth/register", "tokenrequest":"/oauth/token" } } }
 * 
 * @author Luca Roffia
 *
 */
public class EngineProperties {
	private static final Logger logger = LogManager.getLogger("EngineProperties");

	private String defaultsFileName = "engine.defaults";
	private String propertiesFile = "engine.jpar";

	private JsonObject properties = new JsonObject();

	public EngineProperties(String propertiesFile) throws NoSuchElementException, IOException {
		this.propertiesFile = propertiesFile;

		loadProperties();
	}

	protected void defaults() {
		JsonObject parameters = new JsonObject();

		// Engine timeouts
		JsonObject timeouts = new JsonObject();
		timeouts.add("scheduling", new JsonPrimitive(0));
		timeouts.add("queuesize", new JsonPrimitive(1000));
		timeouts.add("keepalive", new JsonPrimitive(5000));
		timeouts.add("http", new JsonPrimitive(5000));
		parameters.add("timeouts", timeouts);

		// Default path, scheme and port
		parameters.add("path", new JsonPrimitive("/sparql"));
		parameters.add("scheme", new JsonPrimitive("http"));
		parameters.add("port", new JsonPrimitive(8000));

		// Secure query
		JsonObject secureQuery = new JsonObject();
		secureQuery.add("port", new JsonPrimitive(8443));
		secureQuery.add("scheme", new JsonPrimitive("https"));
		parameters.add("securequery", secureQuery);

		// Secure update
		JsonObject secureUpdate = new JsonObject();
		secureUpdate.add("port", new JsonPrimitive(8443));
		secureUpdate.add("scheme", new JsonPrimitive("https"));
		parameters.add("secureupdate", secureUpdate);

		// Subscribe
		JsonObject subscribe = new JsonObject();
		subscribe.add("scheme", new JsonPrimitive("ws"));
		subscribe.add("port", new JsonPrimitive(9000));
		parameters.add("subscribe", subscribe);

		// Secure subscribe
		JsonObject secureSubscribe = new JsonObject();
		secureSubscribe.add("scheme", new JsonPrimitive("wss"));
		secureSubscribe.add("port", new JsonPrimitive(9443));
		secureSubscribe.add("path", new JsonPrimitive("/secure/sparql"));
		parameters.add("securesubscribe", secureSubscribe);

		// Authorization server
		JsonObject authServer = new JsonObject();
		authServer.add("port", new JsonPrimitive(8443));
		authServer.add("scheme", new JsonPrimitive("https"));
		authServer.add("register", new JsonPrimitive("/oauth/register"));
		authServer.add("tokenrequest", new JsonPrimitive("/oauth/token"));
		parameters.add("authorizationserver", authServer);

		properties.add("parameters", parameters);
	}

	private void loadProperties() throws NoSuchElementException, IOException {
		FileReader in = null;
		try {
			in = new FileReader(propertiesFile);
			if (in != null) {
				properties = new JsonParser().parse(in).getAsJsonObject();
				if (properties.get("parameters") == null) {
					logger.warn("parameters key is missing");
					throw new NoSuchElementException("parameters key is missing");
				}
				properties = properties.get("parameters").getAsJsonObject();
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

	private void storeProperties(String propertiesFile) throws IOException {
		FileWriter out = new FileWriter(propertiesFile);
		out.write(properties.toString());
		out.close();
	}

	private int getParameter(String rootKey, String nestedKey, int def) {
		if (rootKey == null) {
			if (properties.get(nestedKey) != null)
				return properties.get(nestedKey).getAsInt();
			return def;
		}
		if (properties.get(rootKey) != null) {
			if (properties.get(rootKey).getAsJsonObject().get(nestedKey) != null)
				return properties.get(rootKey).getAsJsonObject().get(nestedKey).getAsInt();

		}
		if (properties.get(nestedKey) != null)
			return properties.get(nestedKey).getAsInt();

		logger.warn(rootKey + " or " + nestedKey + " keys not found");
		return def;
	}

	private String getParameter(String rootKey, String nestedKey, String def) {
		if (rootKey == null) {
			if (properties.get(nestedKey) != null)
				return properties.get(nestedKey).getAsString();
			else
				return def;
		}
		if (properties.get(rootKey) != null) {
			if (properties.get(rootKey).getAsJsonObject().get(nestedKey) != null)
				return properties.get(rootKey).getAsJsonObject().get(nestedKey).getAsString();

		}
		if (properties.get(nestedKey) != null)
			return properties.get(nestedKey).getAsString();

		logger.warn(rootKey + " or " + nestedKey + " keys not found");
		return def;
	}

	public int getHttpTimeout() {
		return getParameter("timeouts", "http", 0);
	}

	public int getSubscribePort() {
		return getParameter("subscribe", "port", 9000);
	}

	public int getSecureUpdatePort() {
		return getParameter("secureupdate", "port", 8443);
	}

	public int getSecureSubscribePort() {
		return getParameter("securesubscribe", "port", 9443);
	}

	public int getUpdatePort() {
		return getParameter("update", "port", 8000);
	}

	public long getSchedulingTimeout() {
		return getParameter("timeouts", "scheduling", 5000);
	}

	public int getSchedulingQueueSize() {
		return getParameter("timeouts", "queuesize", 1000);
	}

	public int getKeepAlivePeriod() {
		return getParameter("timeouts", "keepalive", 5000);
	}

	public String getUpdatePath() {
		return getParameter("update", "path", "/sparql");
	}

	public String getSecureUpdatePath() {
		return getParameter("secureupdate", "path", "/sparql");
	}

	public String getSubscribePath() {
		return getParameter("subscribe", "path", "/sparql");
	}

	public String getSecureSubscribePath() {
		return getParameter("securesubscribe", "path", "/secure/sparql");
	}

	public String getRegisterPath() {
		return getParameter("authorizationserver", "register", "/oauth/register");
	}

	public String getTokenRequestPath() {
		return getParameter("authorizationserver", "tokenrequest", "/oauth/token");
	}

	public String getQueryPath() {
		return getParameter("query", "path", "/sparql");
	}

	public int getQueryPort() {
		return getParameter("query", "port", 8000);
	}

	public String getSecureQueryPath() {
		return getParameter("securequery", "path", "/sparql");
	}

	public int getSecureQueryPort() {
		return getParameter("securequery", "port", 8443);
	}

	public int getAuthorizationServerPort() {
		return getParameter("authorizationserver", "port", 8443);
	}

	public int getAuthorizationServerScheme() {
		return getParameter("authorizationserver", "https", 8443);
	}
}
