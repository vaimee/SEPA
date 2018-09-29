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

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

/**
 * The Class SPARQL11SEProperties.
 *
 * <pre>
 "sparql11seprotocol": {
		"protocol": "ws",
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

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/**
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

	public enum SubscriptionProtocol {
		WS, WSS
	}

	private SubscriptionProtocol subscriptionProtocol;

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(String propertiesFile) throws SEPAPropertiesException {
		super(propertiesFile);
	}

//	/**
//	 * Instantiates a new SPARQL 11 SE properties.
//	 *
//	 * @param propertiesFile
//	 *            the properties file
//	 * @throws SEPAPropertiesException
//	 */
//	public SPARQL11SEProperties(File propertiesFile) throws SEPAPropertiesException {
//		super(propertiesFile);
//	}

	public String toString() {
		return jsap.toString();
	}

	/**
	 * <pre>
	"sparql11seprotocol": {
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/secure/subscribe"
			}
		}
	 * </pre>
	 */
	@Override
	protected void defaults() {
		super.defaults();

		JsonObject sparql11seprotocol = new JsonObject();
		sparql11seprotocol.add("protocol", new JsonPrimitive("ws"));

		JsonObject availableProtocols = new JsonObject();
		JsonObject ws = new JsonObject();
		JsonObject wss = new JsonObject();
		ws.add("port", new JsonPrimitive(9000));
		ws.add("path", new JsonPrimitive("/subscribe"));
		availableProtocols.add("ws", ws);
		ws.add("port", new JsonPrimitive(9443));
		ws.add("path", new JsonPrimitive("/subscribe"));
		availableProtocols.add("wss", wss);
		sparql11seprotocol.add("availableProtocols", availableProtocols);

//		JsonObject security = new JsonObject();
//		security.add("register", new JsonPrimitive("/oauth/register"));
//		security.add("tokenRequest", new JsonPrimitive("/oauth/token"));
//		security.add("securePath", new JsonPrimitive("/secure"));
//		sparql11seprotocol.add("security", security);

		jsap.add("sparql11seprotocol", sparql11seprotocol);
	}

	@Override
	protected void validate() throws SEPAPropertiesException {
		super.validate();

		try {
			JsonElement sparql11seprotocol = jsap.get("sparql11seprotocol");
			String protocol = sparql11seprotocol.getAsJsonObject().get("protocol").getAsString();

			jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject().get(protocol);

			switch (protocol) {
			case "ws":
				subscriptionProtocol = SubscriptionProtocol.WS;
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("port").getAsInt();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("path").getAsString();

				break;
			case "wss":
				subscriptionProtocol = SubscriptionProtocol.WSS;
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("port").getAsInt();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("path").getAsString();
				break;
			}

//			if (jsap.get("sparql11seprotocol").getAsJsonObject().get("security") != null) {
//				jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("register")
//						.getAsString();
//				jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("tokenRequest")
//						.getAsString();
//			}

		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public String getDefaultHost() {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("host").getAsString();
		}
		catch(Exception e) {
			return super.getDefaultHost();
		}
	}
	
	public String getSubscribePath() {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(jsap.get("sparql11seprotocol").getAsJsonObject().get("protocol").getAsString()).getAsJsonObject().get("path").getAsString();
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	public int getSubscribePort() {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(jsap.get("sparql11seprotocol").getAsJsonObject().get("protocol").getAsString()).getAsJsonObject().get("port").getAsInt();
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			return -1;
		}
	}

	public int getHttpsPort() {
		try {
			if (jsap.get("sparql11protocol").getAsJsonObject().get("protocol").getAsString().equals("https"))
				return jsap.get("sparql11protocol").getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
			return -1;
		}
		return -1;
	}

	public SubscriptionProtocol getSubscriptionProtocol() {
		return subscriptionProtocol;
	}
}
