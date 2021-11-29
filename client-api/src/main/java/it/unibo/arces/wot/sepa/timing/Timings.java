/* Timings statistics of the different requests 
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

package it.unibo.arces.wot.sepa.timing;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class Timings {
	private static final Logger logger = LogManager.getLogger();
	
	public static long getTime() {
		return System.nanoTime();
	}
	
	public synchronized static void log(String tag,long start,long stop) {
		String message = String.format("%d,%d,%s",System.currentTimeMillis(),stop-start,tag);
		Level level =Level.getLevel("timing");
		if(level==null) {
			//default
			logger.log(Level.TRACE,message);
		}else {
			logger.log(level,message);
		}
	}
	
	public synchronized static void log(Request request) {
		long start = getTime();
		
		String tag;
		if (request.isUpdateRequest()) tag = "UPDATE_REQUEST";
		else if (request.isSubscribeRequest()) tag = "SUBSCRIBE_REQUEST";
		else if(request.isQueryRequest()) tag = "QUERY_REQUEST"; 
		else if(request.isUnsubscribeRequest()) tag = "UNSUBSCRIBE_REQUEST";
		else tag = "UNKNOWN_REQUEST";
		
		log(tag,start,start);
	}
	
	public synchronized static void log(Response response) {
		long start = getTime();
		
		String tag;
		if (response.isUpdateResponse()) tag = "UPDATE_RESPONSE";
		else if (response.isSubscribeResponse()) tag = "SUBSCRIBE_RESPONSE";
		else if(response.isQueryResponse()) tag = "QUERY_RESPONSE"; 
		else if(response.isUnsubscribeResponse()) tag = "UNSUBSCRIBE_RESPONSE"; 
		else if(response.isError()) tag = "ERROR_RESPONSE";
		else tag = "UNKNOWN_RESPONSE";
		
		log(tag,start,start);
	}
}
