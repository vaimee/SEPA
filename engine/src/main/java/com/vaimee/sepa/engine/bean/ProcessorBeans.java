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

package com.vaimee.sepa.engine.bean;

import com.vaimee.sepa.commons.properties.SPARQL11Properties;

public class ProcessorBeans {
	
	private static SPARQL11Properties prop;
	
	public static void setEndpoint(SPARQL11Properties p) {
		prop = p;
	}

	public static String getEndpointHost() {
		return prop.getHost();
	}
	public static int getEndpointPort() {
		return prop.getPort();
	}
	public static String getEndpointQueryPath() {
		return prop.getQueryPath();
	}
	public static String getEndpointUpdatePath() {
		return prop.getUpdatePath();
	}
	public static String getEndpointUpdateMethod() {
		return prop.getUpdateMethod().name();
	}
	public static String getEndpointQueryMethod() {
		return prop.getQueryMethod().name();
	}
}
