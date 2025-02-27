/* A scheduled request
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

package com.vaimee.sepa.engine.scheduling;

import java.util.Date;

import com.vaimee.sepa.engine.core.ResponseHandler;

public class ScheduledRequest {
	private InternalRequest request = null;
	private ResponseHandler handler = null;
	private int token;
	private long timestamp;
	
	public ScheduledRequest(int token,InternalRequest request,ResponseHandler handler) {
		this.request = request;
		this.handler = handler;
		this.token = token;
		this.timestamp = new Date().getTime();
	}
	
	@Override
	public String toString() {
		return "REQUEST #"+token+ (request.isQueryRequest() ? " QUERY" : request.isUpdateRequest() ? " UPDATE" : request.isSubscribeRequest() ? " SUBSCRIBE" : " UNSUBSCRIBE");
	}
	
	public int getToken() {
		return token;
	}
	
	public InternalRequest getRequest() {
		return request;
	}
	
	public ResponseHandler getResponseHandler() {
		return handler;
	}
	
	public boolean isUpdateRequest() {
		return request.isUpdateRequest();
	}
	
	public boolean isQueryRequest() {
		return request.isQueryRequest();
	}
	
	public boolean isSubscribeRequest() {
		return request.isSubscribeRequest();
	}
	
	public boolean isUnsubscribeRequest() {
		return request.isUnsubscribeRequest();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
}
