/* A response to a scheduled request
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

import com.vaimee.sepa.commons.response.Response;

public class ScheduledResponse {
	private int token = -1;
	private Response response = null;
	long timestamp;
	
	public ScheduledResponse(int token,Response response) {
		this.token = token;
		this.response = response;
		this.timestamp = new Date().getTime();
	}
	
	public Response getResponse() {
		return response;
	}
	
	public int getToken() {
		return token;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		return "RESPONSE #"+token;
	}
}
