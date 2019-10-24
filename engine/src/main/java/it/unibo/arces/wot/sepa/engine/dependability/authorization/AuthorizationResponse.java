/* The class represents the response to an authorization request
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

public class AuthorizationResponse {

	private boolean authorized = true;
	
	private String error = null;
	private String description = null;
	
	private Credentials credentials = null;
	
	public AuthorizationResponse() {
	}
	
	public AuthorizationResponse(String error,String description) {
		this.authorized = false;
		this.error = error;
		this.description = description;
	}
	
	public AuthorizationResponse(Credentials credentials) {
		this.credentials = credentials;
	}
	
	public boolean isAuthorized() {
		return authorized;
	}
	
	public Credentials getClientCredentials() {
		return credentials;
	}
	
	public String getError() {
		return error;
	}
	
	public String getDescription() {
		return description;
	}

}
