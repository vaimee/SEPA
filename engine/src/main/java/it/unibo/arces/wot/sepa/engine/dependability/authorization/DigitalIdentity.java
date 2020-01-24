/* An abstract class of digital identities (e.g., not physical users)
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

package it.unibo.arces.wot.sepa.engine.dependability.authorization;

/**
 * A digital identity includes the credentials to get access to the SPARQL endpoint
 * */
public abstract class DigitalIdentity {
	
	private String uid;
	private Credentials endpointCredentials = new Credentials("SEPATest","SEPATest");
	
	public DigitalIdentity(String uid) {
		this.uid = uid;
	}
	
	public DigitalIdentity(String uid,Credentials cred) {
		this.uid = uid;
		if (cred != null) this.endpointCredentials = cred;
	}
	
	public String getUid() {
		return uid;
	}
	
	public abstract String getObjectClass();
	
	public Credentials getEndpointCredentials() {
		return endpointCredentials;
	}
	
	public String toString() {
		return "UID: "+uid+" "+endpointCredentials;
	}
}
