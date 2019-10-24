/* The engine internal representation of a unsubscribe request
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

package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public class InternalUnsubscribeRequest extends InternalRequest {
	protected String sid;
	protected String gid;
	
	public InternalUnsubscribeRequest(String gid,String sid,Credentials credentials) {
		super(credentials);
		
		this.sid = sid;
		this.gid = gid;
	}
	
	@Override
	public String toString() {
		return "*UNSUBSCRIBE* "+sid;
	}
	
	public String getSID() {
		return sid;
	}
	
	public String getGID() {
		return gid;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalUnsubscribeRequest)) return false;
		return sid.equals(((InternalUnsubscribeRequest)obj).sid);
	}
}
