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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;

import org.apache.logging.log4j.LogManager;

/**
 * <pre>
{
	"parameters": {
		"scheduler": {
			"queueSize": 100
		},
		"processor": {
			"updateTimeout": 60000,
			"queryTimeout": 60000,
			"maxConcurrentRequests": 5
		},
		"spu": {
			"timeout": 5000
		},
		"gates": {
			"secure": false,
			"paths": {
				"securePath": "/secure",
				"update": "/update",
				"query": "/query",
				"subscribe": "/subscribe",
				"register": "/oauth/register",
				"tokenRequest": "/oauth/token"
			},
			"ports": {
				"http": 8000,
				"https": 8443,
				"ws": 9000,
				"wss": 9443
			}
		}
	}
}
 * </pre>
 */
public class EngineProperties {
	private static final Logger logger = LogManager.getLogger();

	private String defaultsFileName = "engine.jpar";

	private JsonObject properties = new JsonObject();

	public EngineProperties(String propertiesFile) throws SEPAPropertiesException {

		if (propertiesFile == null)
			throw new SEPAPropertiesException(new IllegalArgumentException("Properties file is null"));

		FileReader in = null;
		try {
			in = new FileReader(propertiesFile);
			if (in != null) {
				properties = new JsonParser().parse(in).getAsJsonObject();
				if (properties.get("parameters") == null) {
					logger.warn("parameters key is missing");
					throw new SEPAPropertiesException(new NoSuchElementException("Parameters key is missing"));
				}
				properties = properties.get("parameters").getAsJsonObject();

				validate();
			}
			if (in != null)
				in.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());

			defaults();

			try {
				storeProperties(defaultsFileName);
			} catch (IOException e1) {
				logger.error(e1.getMessage());
				throw new SEPAPropertiesException(e1);
			}

			logger.warn("USING DEFAULTS. Edit \"" + defaultsFileName + "\" (if needed) and run again the broker");
			// throw new SEPAPropertiesException(new FileNotFoundException(
			// "USING DEFAULTS. Edit \"" + defaultsFileName + "\" (if needed) and run again
			// the broker"));
		}
	}

	public String toString() {
		return properties.toString();
	}

	protected void defaults() {
		JsonObject parameters = new JsonObject();

		// Scheduler properties
		JsonObject scheduler = new JsonObject();
		scheduler.add("queueSize", new JsonPrimitive(100));
		parameters.add("scheduler", scheduler);

		// Processor properties
		JsonObject processor = new JsonObject();
		processor.add("updateTimeout", new JsonPrimitive(5000));
		processor.add("queryTimeout", new JsonPrimitive(5000));
		processor.add("maxConcurrentRequests", new JsonPrimitive(5));
		parameters.add("processor", processor);

		// SPU
		JsonObject spu = new JsonObject();
		spu.add("timeout", new JsonPrimitive(5000));
		parameters.add("spu", spu);

		// Gates
		JsonObject gates = new JsonObject();
		gates.add("secure", new JsonPrimitive(false));

		// Ports
		JsonObject ports = new JsonObject();
		ports.add("http", new JsonPrimitive(8000));
		ports.add("https", new JsonPrimitive(8443));
		ports.add("ws", new JsonPrimitive(9000));
		ports.add("wss", new JsonPrimitive(9443));

		// URI patterns
		JsonObject paths = new JsonObject();
		paths.add("securePath", new JsonPrimitive("/secure"));
		paths.add("update", new JsonPrimitive("/update"));
		paths.add("query", new JsonPrimitive("/query"));
		paths.add("subscribe", new JsonPrimitive("/subscribe"));
		paths.add("unsubscribe", new JsonPrimitive("/unsubscribe"));
		paths.add("register", new JsonPrimitive("/oauth/register"));
		paths.add("tokenRequest", new JsonPrimitive("/oauth/token"));

		gates.add("paths", paths);
		gates.add("ports", ports);
		parameters.add("gates", gates);

		properties.add("parameters", parameters);
	}

	protected void validate() throws SEPAPropertiesException {
		try {
			properties.get("scheduler").getAsJsonObject().get("queueSize").getAsInt();

			properties.get("processor").getAsJsonObject().get("updateTimeout").getAsInt();
			properties.get("processor").getAsJsonObject().get("queryTimeout").getAsInt();
			properties.get("processor").getAsJsonObject().get("maxConcurrentRequests").getAsInt();

			properties.get("spu").getAsJsonObject().get("timeout").getAsInt();

			properties.get("gates").getAsJsonObject().get("secure").getAsBoolean();

			properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("http").getAsInt();
			properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("https").getAsInt();
			properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("ws").getAsInt();
			properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("wss").getAsInt();

			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("securePath").getAsString();
			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("update").getAsString();
			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("query").getAsString();
			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("subscribe").getAsString();
			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("register").getAsString();
			properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("tokenRequest").getAsString();
		} catch (Exception e) {
			throw new SEPAPropertiesException(new Exception("Failed to validate jpar: " + e.getMessage()));
		}
	}

	private void storeProperties(String propertiesFile) throws IOException {
		FileWriter out = new FileWriter(propertiesFile);
		out.write(properties.toString());
		out.close();
	}

	public boolean isSecure() {
		try {
			return properties.get("gates").getAsJsonObject().get("secure").getAsBoolean();
		} catch (Exception e) {
			return false;
		}
	}

	public int getMaxConcurrentRequests() {
		try {
			return properties.get("processor").getAsJsonObject().get("maxConcurrentRequests").getAsInt();
		} catch (Exception e) {
			return 5;
		}
	}

	public int getUpdateTimeout() {
		try {
			return properties.get("processor").getAsJsonObject().get("updateTimeout").getAsInt();
		} catch (Exception e) {
			return 5000;
		}
	}

	public int getQueryTimeout() {
		try {
			return properties.get("processor").getAsJsonObject().get("queryTimeout").getAsInt();
		} catch (Exception e) {
			return 5000;
		}
	}

	public int getSchedulingQueueSize() {
		try {
			return properties.get("scheduler").getAsJsonObject().get("queueSize").getAsInt();
		} catch (Exception e) {
			return 1000;
		}
	}

	public int getWsPort() {
		try {
			return properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("ws").getAsInt();
		} catch (Exception e) {
			return 9000;
		}
	}

	public int getHttpPort() {
		try {
			return properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("http").getAsInt();
		} catch (Exception e) {
			return 8000;
		}
	}

	public int getHttpsPort() {
		try {
			return properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("https").getAsInt();
		} catch (Exception e) {
			return 8443;
		}
	}

	public int getWssPort() {
		try {
			return properties.get("gates").getAsJsonObject().get("ports").getAsJsonObject().get("wss").getAsInt();
		} catch (Exception e) {
			return 9443;
		}
	}

	public String getUpdatePath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("update").getAsString();
		} catch (Exception e) {
			return "/update";
		}
	}

	public String getSubscribePath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("subscribe")
					.getAsString();
		} catch (Exception e) {
			return "/subscribe";
		}
	}

	public String getQueryPath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("query").getAsString();
		} catch (Exception e) {
			return "/query";
		}
	}

	public String getRegisterPath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("register")
					.getAsString();
		} catch (Exception e) {
			return "/oauth/register";
		}
	}

	public String getTokenRequestPath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("tokenRequest")
					.getAsString();
		} catch (Exception e) {
			return "/oauth/token";
		}
	}

	public String getSecurePath() {
		try {
			return properties.get("gates").getAsJsonObject().get("paths").getAsJsonObject().get("securePath")
					.getAsString();
		} catch (Exception e) {
			return "/secure";
		}
	}

	public int getSPUProcessingTimeout() {
		try {
			return properties.get("spu").getAsJsonObject().get("timeout").getAsInt();
		} catch (Exception e) {
			return 2000;
		}
	}

	public boolean isUpdateReliable() {
		try {
			return properties.get("processor").getAsJsonObject().get("reliableUpdate").getAsBoolean();
		} catch (Exception e) {
			return true;
		}
	}

}
