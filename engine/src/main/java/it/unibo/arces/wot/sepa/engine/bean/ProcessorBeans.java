/* This class belongs to the JMX classes used for the remote monitoring of the engine
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

package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

public class ProcessorBeans {
	
	private static String host;
	private static int port;
	private static String queryPath;
	private static String updatePath;
	private static String updateMethod;
	private static String queryMethod;
	
	public static void setEndpoint(SPARQL11Properties prop) {
		host = prop.getHost();
		port = prop.getPort();
		queryPath = prop.getQueryPath();
		updatePath = prop.getUpdatePath();
		updateMethod = prop.getUpdateMethod().name();
		queryMethod = prop.getQueryMethod().name();
	}

	public static String getEndpointHost() {
		return host;
	}
	public static int getEndpointPort() {
		return port;
	}
	public static String getEndpointQueryPath() {
		return queryPath;
	}
	public static String getEndpointUpdatePath() {
		return updatePath;
	}
	public static String getEndpointUpdateMethod() {
		return updateMethod;
	}
	public static String getEndpointQueryMethod() {
		return queryMethod;
	}
}
