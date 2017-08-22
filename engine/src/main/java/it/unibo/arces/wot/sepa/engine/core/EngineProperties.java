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
 {
	"parameters": {
		"timeouts": {
			"scheduling": 0,
			"queueSize": 1000,
			"keepalive": 5000,
			"http": 5000
		},
		"ports": {
			"http": 8000,
			"ws": 9000,
			"https": 8443,
			"wss": 9443
		},
		"paths": {
			"update": "/update",
			"query": "/query",
			"subscribe": "/subscribe",
			"unsubscribe": "/unsubscribe",
			"register": "/oauth/register",
			"tokenRequest": "/oauth/token",
			"securePath" : "/secure"
		}
	}
}
 */
public class EngineProperties {
	private static final Logger logger = LogManager.getLogger("EngineProperties");

	private String defaultsFileName = "defaults.jpar";
	private String propertiesFile = "engine.jpar";

	private JsonObject properties = new JsonObject();

	public EngineProperties(String propertiesFile) throws IllegalArgumentException,NoSuchElementException, IOException {
		if (propertiesFile == null) throw new IllegalArgumentException("Properties file is null");
		this.propertiesFile = propertiesFile;

		loadProperties();
	}
	
	public String toString() {
		return properties.toString();
	}

	protected void defaults() {
		JsonObject parameters = new JsonObject();

		// Engine timeouts
		JsonObject timeouts = new JsonObject();
		timeouts.add("scheduling", new JsonPrimitive(0));
		timeouts.add("queueSize", new JsonPrimitive(1000));
		timeouts.add("keepalive", new JsonPrimitive(5000));
		timeouts.add("http", new JsonPrimitive(5000));
		parameters.add("timeouts", timeouts);

		// Ports
		JsonObject ports = new JsonObject();
		ports.add("http", new JsonPrimitive(8000));
		ports.add("https", new JsonPrimitive(8443));
		ports.add("ws", new JsonPrimitive(9000));
		ports.add("wss", new JsonPrimitive(9443));
		parameters.add("ports", ports);
		
		// URI patterns
		JsonObject paths = new JsonObject();
		parameters.add("securePath", new JsonPrimitive("/secure"));
		paths.add("update", new JsonPrimitive("/update"));
		paths.add("query", new JsonPrimitive("/query"));
		paths.add("subscribe", new JsonPrimitive("/subscribe"));
		paths.add("unsubscribe", new JsonPrimitive("/unsubscribe"));
		paths.add("register", new JsonPrimitive("/oauth/register"));
		paths.add("tokenRequest", new JsonPrimitive("/oauth/token"));
		parameters.add("paths", paths);
		
		properties.add("parameters", parameters);
	}
	
	protected boolean checkParameters() throws IllegalStateException, ClassCastException {
		properties.get("timeouts").getAsJsonObject().get("scheduling").getAsInt();
		properties.get("timeouts").getAsJsonObject().get("queueSize").getAsInt();
		properties.get("timeouts").getAsJsonObject().get("keepalive").getAsInt();
		properties.get("timeouts").getAsJsonObject().get("http").getAsInt();
		
		properties.get("ports").getAsJsonObject().get("http").getAsInt();
		properties.get("ports").getAsJsonObject().get("https").getAsInt();
		properties.get("ports").getAsJsonObject().get("ws").getAsInt();
		properties.get("ports").getAsJsonObject().get("wss").getAsInt();
		
		properties.get("paths").getAsJsonObject().get("securePath").getAsString();
		properties.get("paths").getAsJsonObject().get("update").getAsString();
		properties.get("paths").getAsJsonObject().get("query").getAsString();
		properties.get("paths").getAsJsonObject().get("subscribe").getAsString();
		properties.get("paths").getAsJsonObject().get("register").getAsString();
		properties.get("paths").getAsJsonObject().get("tokenRequest").getAsString();
		
		return true;
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
				
				if(!checkParameters()) throw new NoSuchElementException("JPAR missing parameter");
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
		try {
			if (rootKey == null) return def;
			if (nestedKey == null) return properties.get(rootKey).getAsInt();
			return properties.get(rootKey).getAsJsonObject().get(nestedKey).getAsInt();
		} catch (Exception e) {
		}
		
		return def;
	}

	private String getParameter(String rootKey, String nestedKey, String def) {
		try {
			if (rootKey == null) return def;
			if (nestedKey == null) return properties.get(rootKey).getAsString();
			return properties.get(rootKey).getAsJsonObject().get(nestedKey).getAsString();
		} catch (Exception e) {
		}
		
		return def;
	}

	public int getHttpTimeout() {
		return getParameter("timeouts", "http", 0);
	}

	public long getSchedulingTimeout() {
		return getParameter("timeouts", "scheduling", 5000);
	}

	public int getSchedulingQueueSize() {
		return getParameter("timeouts", "queueSize", 1000);
	}

	public int getKeepAlivePeriod() {
		return getParameter("timeouts", "keepalive", 5000);
	}

	public int getWsPort() {
		return getParameter("ports", "ws", 9000);
	}

	public int getHttpPort() {
		return getParameter("ports", "http", 8000);
	}

	public int getHttpsPort() {
		return getParameter("ports", "https", 8443);
	}

	public int getWssPort() {
		return getParameter("ports", "wss", 9443);
	}
	
	public String getUpdatePath() {
		return getParameter("paths", "update", "/update");
	}

	public String getSubscribePath() {
		return getParameter("paths", "subscribe", "/subscribe");
	}

	public String getQueryPath() {
		return getParameter("paths", "query", "/query");
	}
	
	public String getRegisterPath() {
		return getParameter("paths", "register", "/oauth/register");
	}

	public String getTokenRequestPath() {
		return getParameter("paths", "tokenRequest", "/oauth/token");
	}

	public String getSecurePath() {
		return getParameter("paths", "securePath", "/secure");
	}
	
}
