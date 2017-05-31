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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.tyrus.client.ClientProperties;

import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;

/**
 * The Class SecureWebsocketClientEndpoint.
 */
public class SecureWebsocketClientEndpoint extends WebsocketClientEndpoint {
	
	/** The logger. */
	protected Logger logger = LogManager.getLogger("SecureWebsocketClientEndpoint");
	
	/** The Security Manager based on JKS */
	private SSLSecurityManager sm = new SSLSecurityManager("sepa.jks","*sepa.jks*","SepaKey","*SepaKey*","SepaCertificate",true,false,null);
	
	/**
	 * Instantiates a new secure websocket client endpoint.
	 *
	 * @param wsUrl the ws url (e.g., wss://wot.arces.unibo.it:9443/secure/sparql)
	 */
	public SecureWebsocketClientEndpoint(String wsUrl) {
		super(wsUrl);
			
		client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sm.getWssConfigurator());
	}

}
