/* This class represents a ping message
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

package it.unibo.arces.wot.sepa.commons.response;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonPrimitive;

/**
 * This class represents the ping message sent on every active websocket (see SPARQL 1.1 Subscription Language)
 *
 * The JSON serialization is the following:
 *
 * {"ping" : "yyyy-MM-dd HH:mm:ss.SSS"}
 *
 * */

public class Ping extends Response {
	
	/**
	 * Instantiates a new ping.
	 */
	public Ping() {
		super();
		
		json.add("ping", new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) );
	}
}
