/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
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

package it.unibo.arces.wot.sepa.api;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.logging.Logging;

/**
 * The Class SPARQL11SEProperties.
 *
 * <pre>
 "sparql11seprotocol": {
        "host" : "override" (optional)
		"protocol": "ws",
		"reconnect" : true, (optional),
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/subscribe"
			}
		}
 * </pre>
 */
public class SPARQL11SEProperties extends SPARQL11Properties {
	/**
	 * 
	 * The primitives introduced by the SPARQL 1.1 SE Protocol are:
	 *
	 * SECUREUPDATE,SECUREQUERY,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN
	 *
	 *
	 * @author Luca Roffia (luca.roffia@unibo.it)
	 * @version 0.1
	 */
	public enum SPARQL11SEPrimitive {
		/** A secure update primitive */
		SECUREUPDATE,
		/** A subscribe primitive */
		SUBSCRIBE,
		/** A secure subscribe primitive. */
		SECURESUBSCRIBE,
		/** A unsubscribe primitive. */
		UNSUBSCRIBE,
		/** A secure unsubscribe primitive. */
		SECUREUNSUBSCRIBE,
		/** A register primitive. */
		REGISTER,
		/** A request token primitive. */
		REQUESTTOKEN,
		/** A secure query primitive. */
		SECUREQUERY
	}

	// Members
	protected SPARQL11SEProtocol sparql11seprotocol;

	public static class SubscriptionProtocol {
		public String path = null;
		public int port = -1;
		public String scheme = null;
	}

	protected static class SPARQL11SEProtocol {
		public String protocol = null;
		public HashMap<String,SubscriptionProtocol> availableProtocols = null;
		public String host = null;
		public boolean reconnect = true;

		public SPARQL11SEProtocol merge(SPARQL11SEProtocol temp) {
			if (temp != null) {
				protocol = (temp.protocol != null ? temp.protocol : protocol);
				host = (temp.host != null ? temp.host : host);
				reconnect = temp.reconnect;
				availableProtocols = (temp.availableProtocols != null ? temp.availableProtocols : availableProtocols);
			}
			return this;
		}

		public int getPort() {
			return availableProtocols.get(protocol).port;
		}

		public String getPath() {
			return availableProtocols.get(protocol).path;
		}

		public SubscriptionProtocol getSubscriptionProtocol() {
			return availableProtocols.get(protocol);
		}
	}

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile the properties file
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(String propertiesFile) throws SEPAPropertiesException {
		super(propertiesFile);

		SPARQL11SEProperties jsap;
		try {
			jsap = new Gson().fromJson(new FileReader(propertiesFile), SPARQL11SEProperties.class);
			sparql11seprotocol = jsap.sparql11seprotocol;
		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e2) {
			Logging.logger.error(e2.getMessage());
			e2.printStackTrace();
			throw new SEPAPropertiesException(e2);
		}

//		try {
//			jsap = new Gson().fromJson(new FileReader(propertiesFile), SPARQL11SEProperties.class);
//		} catch (Exception e) {
//			Logging.logger.warn("Create from file: " + propertiesFile);
//			Logging.logger.warn(e.getMessage());
//			jsap = new SPARQL11SEProperties();
//			Logging.logger.warn("USING DEFAULTS. Edit \"" + defaultsFileName + "\" (if needed) and run again the broker");
//			try {
//				jsap.storeProperties(defaultsFileName);
//			} catch (SEPAPropertiesException e1) {
//				Logging.logger.error(e1.getMessage());
//			}
//			
//		}

		
	}

//	public SPARQL11SEProperties() {
//		super();
//		sparql11seprotocol = new SPARQL11SEProtocol();
//	}

	public String toString() {
		return new Gson().toJson(this);
	}

	public String getSubscribeHost() {
		return (sparql11seprotocol.host != null ? sparql11seprotocol.host : super.host);
	}

	public void setHost(String host) {
		sparql11seprotocol.host = host;
	}

	public String getSubscribePath() {
		return sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol).path;
	}

	public void setSubscribePath(String path) {
		sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol).path = path;
	}

	public int getSubscribePort() {
		return sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol).port;
	}

	public void setSubscribePort(int port) {
		sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol).port = port;
	}

	public SubscriptionProtocol getSubscriptionProtocol() {
		return sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol);
	}

	public boolean getReconnect() {
		return sparql11seprotocol.reconnect;
	}

	public void setSubscriptionProtocol(String scheme) {
		sparql11seprotocol.protocol = scheme;

	}
}
