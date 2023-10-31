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

package it.unibo.arces.wot.sepa.commons.properties;

import java.io.IOException;
import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
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
	protected SPARQL11SEProtocolProperties sparql11seprotocol;

	public SPARQL11SEProperties() {
		sparql11seprotocol = new SPARQL11SEProtocolProperties();

		override(null);
	}

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param in where to read the JSAP from
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(Reader in) throws SEPAPropertiesException {
		super(in);

		parseJSAP(in);

		override(null);
	}

	public SPARQL11SEProperties(Reader in,String[] args) throws SEPAPropertiesException {
		super(in,args);

		parseJSAP(in);

		override(args);
	}

	public SPARQL11SEProperties(String propertiesFile,String[] args) throws SEPAPropertiesException {
		super(propertiesFile);

		Reader in = getReaderFromUri(propertiesFile);
		parseJSAP(in);
		try {
			in.close();
		} catch (IOException e) {
			throw new SEPAPropertiesException(e);
		}

		override(args);
	}

	public SPARQL11SEProperties(String propertiesFile) throws SEPAPropertiesException {
		super(propertiesFile);

		Reader in = getReaderFromUri(propertiesFile);
		parseJSAP(in);
		try {
			in.close();
		} catch (IOException e) {
			throw new SEPAPropertiesException(e);
		}

		override(null);
	}

	private void parseJSAP(Reader in) throws SEPAPropertiesException {
		SPARQL11SEProperties jsap;
		try {
			jsap = new Gson().fromJson(in, SPARQL11SEProperties.class);
			sparql11seprotocol = jsap.sparql11seprotocol;
		} catch (JsonSyntaxException | JsonIOException e2) {
			Logging.logger.error(e2.getMessage());
			e2.printStackTrace();
			throw new SEPAPropertiesException(e2);
		}
	}

	protected final void setParameter(String key,String value) {
		switch (key) {
			case "-host" :
				this.host = value;
				break;
			case "-sparql11protocol.port":
				this.sparql11protocol.port = Integer.valueOf(value);
				break;
			case "-sparql11protocol.host":
				this.sparql11protocol.host = host;
				break;
			case "-sparql11protocol.protocol":
				this.sparql11protocol.protocol = (value == "http" ? ProtocolScheme.http : ProtocolScheme.https);
				break;
			case "-sparql11protocol.update.method":
				this.sparql11protocol.update.method = (value == "post" ? UpdateProperties.UpdateHTTPMethod.POST : UpdateProperties.UpdateHTTPMethod.URL_ENCODED_POST);
				break;
			case "-sparql11protocol.update.format":
				this.sparql11protocol.update.format = (value == "json" ? UpdateProperties.UpdateResultsFormat.JSON : UpdateProperties.UpdateResultsFormat.HTML);
				break;
			case "-sparql11protocol.update.path":
				this.sparql11protocol.update.path = value;
				break;
			case "-sparql11protocol.query.method":
				this.sparql11protocol.query.method = (value == "get" ? QueryProperties.QueryHTTPMethod.GET : (value == "post" ? QueryProperties.QueryHTTPMethod.POST : QueryProperties.QueryHTTPMethod.URL_ENCODED_POST));
				break;
			case "-sparql11protocol.query.format":
				this.sparql11protocol.query.format = (value == "json" ? QueryProperties.QueryResultsFormat.JSON : (value == "xml" ? QueryProperties.QueryResultsFormat.XML : QueryProperties.QueryResultsFormat.CSV));
				break;
			case "-sparql11protocol.query.path":
				this.sparql11protocol.query.path = value;
				break;
			case "-sparql11seprotocol.host":
				this.sparql11seprotocol.host = value;
				break;
			case "-sparql11seprotocol.protocol":
				this.sparql11seprotocol.protocol = value;
				break;
			case "-sparql11seprotocol.reconnect":
				this.sparql11seprotocol.reconnect = Boolean.valueOf(value);
				break;
			default:
				if (key.startsWith("-sparql11seprotocol.availableProtocols")) {
					String[] token = key.split(".");
					if (token[3] == "path") this.sparql11seprotocol.availableProtocols.get(token[2]).path = value;
					else if (token[3] == "port") this.sparql11seprotocol.availableProtocols.get(token[2]).port = Integer.valueOf(value);
					else if (token[3] == "scheme") this.sparql11seprotocol.availableProtocols.get(token[2]).scheme = value;
				}
		}
	}

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

	public SubscriptionProtocolProperties getSubscriptionProtocol() {
		return sparql11seprotocol.availableProtocols.get(sparql11seprotocol.protocol);
	}

	public boolean getReconnect() {
		return sparql11seprotocol.reconnect;
	}

	public void setSubscriptionProtocol(String scheme) {
		sparql11seprotocol.protocol = scheme;

	}
}
