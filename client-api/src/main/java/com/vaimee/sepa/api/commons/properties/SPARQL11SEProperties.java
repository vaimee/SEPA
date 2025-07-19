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

package com.vaimee.sepa.api.commons.properties;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.logging.Logging;

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

	public SPARQL11SEProperties() throws SEPAPropertiesException {
		this((URI) null, null);
	}

	public SPARQL11SEProperties(String uri, String[] args) throws SEPAPropertiesException {
		this(URI.create(uri), args);
	}

	public SPARQL11SEProperties(URI uri) throws SEPAPropertiesException {
		this(uri, null);
	}

	public SPARQL11SEProperties(String uri) throws SEPAPropertiesException {
		this(URI.create(uri), null);
	}

	public SPARQL11SEProperties(URI uri, String[] args) throws SEPAPropertiesException {
		super(uri, args);

		if (uri != null) {
			Reader in = getReaderFromUri(uri);
			SPARQL11SEProperties jsap;
			try {
				jsap = new Gson().fromJson(in, SPARQL11SEProperties.class);
				sparql11seprotocol = jsap.sparql11seprotocol;
			} catch (JsonSyntaxException | JsonIOException e2) {
				Logging.getLogger().error(e2.getMessage());
				throw new SEPAPropertiesException(e2);
			}

			try {
				in.close();
			} catch (IOException e) {
				throw new SEPAPropertiesException(e);
			}
		} else
			sparql11seprotocol = new SPARQL11SEProtocolProperties();

		Map<String, String> envs = System.getenv();
		for (String var : envs.keySet()) {
			Logging.getLogger().trace("Environmental variable " + var + " : " + envs.get(var));
			setSeParameter("-" + var, envs.get(var));
		}

		if (args != null)
			for (int i = 0; i < args.length; i++) {
				Logging.getLogger().trace("Argument  " + args[i]);
				String[] params = args[i].split("=");
				if (params.length == 2) {
					setSeParameter(params[0], params[1]);
				}
			}
	}

	protected void setSeParameter(String key, String value) {
		switch (key) {
		case "-sparql11seprotocol.host":
			sparql11seprotocol.setHost(value);
			break;
		case "-sparql11seprotocol.protocol":
			sparql11seprotocol.setProtocol(value);
			break;
		case "-sparql11seprotocol.reconnect":
			sparql11seprotocol.setReconnect(Boolean.parseBoolean(value));
			break;
		default:
			if (key.startsWith("-sparql11seprotocol.availableProtocols")) {
				String[] token = key.split("\\.");
				if (sparql11seprotocol == null)
					sparql11seprotocol = new SPARQL11SEProtocolProperties();
				if (sparql11seprotocol.getAvailableProtocols() == null) {
					sparql11seprotocol.setAvailableProtocols(new HashMap<>());
					sparql11seprotocol.getAvailableProtocols().put(token[2], new SubscriptionProtocolProperties());
				} else {
					if (sparql11seprotocol.getAvailableProtocols().get(token[2]) == null) {
						sparql11seprotocol.getAvailableProtocols().put(token[2], new SubscriptionProtocolProperties());
					}
				}
				if (Objects.equals(token[3], "path"))
					sparql11seprotocol.getAvailableProtocols().get(token[2]).setPath(value);
				else if (Objects.equals(token[3], "port"))
					sparql11seprotocol.getAvailableProtocols().get(token[2]).setPort(Integer.parseInt(value));
				else if (Objects.equals(token[3], "scheme"))
					sparql11seprotocol.getAvailableProtocols().get(token[2]).setScheme(value);
			}
		}
	}

	public String getSubscribeHost() {
		return sparql11seprotocol.getHost();
	}

	public void setHost(String host) {
		sparql11seprotocol.setHost(host);
	}

	public String getSubscribePath() {
		return sparql11seprotocol.getAvailableProtocols().get(sparql11seprotocol.getProtocol()).getPath();
	}

	public void setSubscribePath(String path) {
		sparql11seprotocol.getAvailableProtocols().get(sparql11seprotocol.getProtocol()).setPath(path);
	}

	public int getSubscribePort() {
		return sparql11seprotocol.getAvailableProtocols().get(sparql11seprotocol.getProtocol()).getPort();
	}

	public void setSubscribePort(int port) {
		sparql11seprotocol.getAvailableProtocols().get(sparql11seprotocol.getProtocol()).setPort(port);
	}

	public SubscriptionProtocolProperties getSubscriptionProtocol() {
		return sparql11seprotocol.getAvailableProtocols().get(sparql11seprotocol.getProtocol());
	}

	public boolean getReconnect() {
		return sparql11seprotocol.isReconnect();
	}

	public void setSubscriptionProtocol(String scheme) {
		sparql11seprotocol.setProtocol(scheme);

	}
}
