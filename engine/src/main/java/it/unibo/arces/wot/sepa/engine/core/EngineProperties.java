/* This class implements the configuration properties of the Semantic Event Processing Architecture (SEPA) Engine
 * 
 * Authors:	Luca Roffia (luca.roffia@unibo.it)
 * 			Andrea Bisacchi (andrea.bisacchi5@studio.unibo.it)

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;

/**
 * <pre>
{
	"parameters": {
		"scheduler": {
			"queueSize": 100,
			"timeout" : 5000
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
	private static final transient Logger logger = LogManager.getLogger();

	private static final transient String defaultsFileName = "engine.jpar";

	private Parameters parameters = new Parameters();

	private EngineProperties() {}
	
	public static EngineProperties load(String propertiesFile, boolean secure) throws SEPAPropertiesException {
		EngineProperties result = EngineProperties.load(propertiesFile);
		result.parameters.gates.secure = secure;
		return result;
	}
	
	public static EngineProperties load(String propertiesFile) throws SEPAPropertiesException {
		if (propertiesFile == null) {
			throw new SEPAPropertiesException(new IllegalArgumentException("Properties file is null"));
		}

		EngineProperties result;
		Gson gson = new Gson();
		
		try {
			result = gson.fromJson(new FileReader(propertiesFile), EngineProperties.class);
			setDefaultForNulls(result);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			result = defaults();
			try {
				result.storeProperties(defaultsFileName);
			} catch (IOException e1) {
				logger.error(e1.getMessage());
				throw new SEPAPropertiesException(e1);
			}
			logger.warn("USING DEFAULTS. Edit \"" + defaultsFileName + "\" (if needed) and run again the broker");
		}
		return result;
	}

	public static void store(EngineProperties properties, String propertiesFile) throws IOException {
		properties.storeProperties(propertiesFile);
	}
	
	public String toString() {
		return new Gson().toJson(this);
	}

	private static void setDefaultForNulls(EngineProperties result) {
		EngineProperties defaultProperties = defaults();
		
		// Scheduler
		if (result.parameters.scheduler.queueSize < 0) result.parameters.scheduler.queueSize = defaultProperties.parameters.scheduler.queueSize;
		if (result.parameters.scheduler.timeout < 0) result.parameters.scheduler.timeout = defaultProperties.parameters.scheduler.timeout;
		
		// Processor
		if (result.parameters.processor.updateTimeout < 0) result.parameters.processor.updateTimeout = defaultProperties.parameters.processor.updateTimeout;
		if (result.parameters.processor.updateTimeout < 0) result.parameters.processor.queryTimeout = defaultProperties.parameters.processor.queryTimeout;
		if (result.parameters.processor.maxConcurrentRequests < 0) result.parameters.processor.maxConcurrentRequests = defaultProperties.parameters.processor.maxConcurrentRequests;
		//result.parameters.processor.reliableUpdate = defaultProperties.parameters.processor.reliableUpdate;
		
		// SPU
		if (result.parameters.spu.timeout < 0) result.parameters.spu.timeout = defaultProperties.parameters.spu.timeout;

		// Gates
		//result.parameters.gates.secure = defaultProperties.parameters.gates.secure;

		// Gates -> Ports
		if (result.parameters.gates.ports.http <= 0) result.parameters.gates.ports.http = defaultProperties.parameters.gates.ports.http;
		if (result.parameters.gates.ports.https <= 0) result.parameters.gates.ports.https = defaultProperties.parameters.gates.ports.https;
		if (result.parameters.gates.ports.ws <= 0) result.parameters.gates.ports.ws = defaultProperties.parameters.gates.ports.ws;
		if (result.parameters.gates.ports.wss <= 0) result.parameters.gates.ports.wss = defaultProperties.parameters.gates.ports.wss;

		// Gates -> Paths
		if (result.parameters.gates.paths.secure == null) result.parameters.gates.paths.secure = defaultProperties.parameters.gates.paths.secure;
		if (result.parameters.gates.paths.update == null) result.parameters.gates.paths.update = defaultProperties.parameters.gates.paths.update;
		if (result.parameters.gates.paths.query == null) result.parameters.gates.paths.query = defaultProperties.parameters.gates.paths.query;
		if (result.parameters.gates.paths.subscribe == null) result.parameters.gates.paths.subscribe = defaultProperties.parameters.gates.paths.subscribe;
		if (result.parameters.gates.paths.unsubscribe == null) result.parameters.gates.paths.unsubscribe = defaultProperties.parameters.gates.paths.unsubscribe;
		if (result.parameters.gates.paths.register == null) result.parameters.gates.paths.register = defaultProperties.parameters.gates.paths.register;
		if (result.parameters.gates.paths.tokenRequest == null) result.parameters.gates.paths.tokenRequest = defaultProperties.parameters.gates.paths.tokenRequest;
	}
	
	protected static EngineProperties defaults() {
		EngineProperties result = new EngineProperties();
		
		// Scheduler
		result.parameters.scheduler.queueSize = 100;
		result.parameters.scheduler.timeout = 5000;
		
		// Processor
		result.parameters.processor.updateTimeout = 5000;
		result.parameters.processor.queryTimeout = 5000;
		result.parameters.processor.maxConcurrentRequests = 5;
		result.parameters.processor.reliableUpdate = true;
		
		// SPU
		result.parameters.spu.timeout = 5000;

		// Gates
		result.parameters.gates.secure = false;

		// Gates -> Ports
		result.parameters.gates.ports.http = 8000;
		result.parameters.gates.ports.https = 8443;
		result.parameters.gates.ports.ws = 9000;
		result.parameters.gates.ports.wss = 9443;

		// Gates -> Paths
		result.parameters.gates.paths.secure = "/secure";
		result.parameters.gates.paths.update = "/update";
		result.parameters.gates.paths.query = "/query";
		result.parameters.gates.paths.subscribe = "/subscribe";
		result.parameters.gates.paths.unsubscribe = "/unsubscribe";
		result.parameters.gates.paths.register = "/oauth/register";
		result.parameters.gates.paths.tokenRequest = "/oauth/token";
		return result;
	}

	private void storeProperties(String propertiesFile) throws IOException {
		FileWriter out = new FileWriter(propertiesFile);
		out.write(this.toString());
		out.close();
	}

	public boolean isSecure() {
		return this.parameters.gates.secure;
	}

	public int getMaxConcurrentRequests() {
		return this.parameters.processor.maxConcurrentRequests;
	}

	public int getUpdateTimeout() {
		return this.parameters.processor.updateTimeout;
	}

	public int getQueryTimeout() {
		return this.parameters.processor.queryTimeout;
	}

	public int getSchedulingQueueSize() {
		return this.parameters.scheduler.queueSize;
	}

	public int getWsPort() {
		return this.parameters.gates.ports.ws;
	}

	public int getHttpPort() {
		return this.parameters.gates.ports.http;
	}

	public int getHttpsPort() {
		return this.parameters.gates.ports.https;
	}

	public int getWssPort() {
		return this.parameters.gates.ports.wss;
	}

	public String getUpdatePath() {
		return this.parameters.gates.paths.update;
	}

	public String getSubscribePath() {
		return this.parameters.gates.paths.subscribe;
	}
	
	public String getUnsubscribePath() {
		return this.parameters.gates.paths.unsubscribe;
	}

	public String getQueryPath() {
		return this.parameters.gates.paths.query;
	}

	public String getRegisterPath() {
		return this.parameters.gates.paths.register;
	}

	public String getTokenRequestPath() {
		return this.parameters.gates.paths.tokenRequest;
	}

	public String getSecurePath() {
		return this.parameters.gates.paths.secure;
	}

	public int getSPUProcessingTimeout() {
		return this.parameters.spu.timeout;
	}

	public boolean isUpdateReliable() {
		return this.parameters.processor.reliableUpdate;
	}

	public int getSchedulerTimeout() {
		return this.parameters.scheduler.timeout;
	}

	private class Parameters {
		public Scheduler scheduler = new Scheduler();
		public Processor processor = new Processor();
		public Spu spu = new Spu();
		public Gates gates = new Gates();
	}
	
	private class Scheduler {
		public int queueSize;
		public int timeout;
	}
	
	private class Processor {
		public int updateTimeout;
		public int queryTimeout;
		public int maxConcurrentRequests;
		public boolean reliableUpdate;
	}
	
	private class Spu {
		public int timeout;
	}
	
	private class Gates {
		public boolean secure;
		public Paths paths = new Paths();
		public Ports ports = new Ports();
	}
	
	private class Paths {
		public String secure;
		public String update;
		public String query;
		public String subscribe;
		public String unsubscribe;
		public String register;
		public String tokenRequest;
	}
	
	private class Ports {
		public int http;
		public int https;
		public int ws;
		public int wss;
	}

}