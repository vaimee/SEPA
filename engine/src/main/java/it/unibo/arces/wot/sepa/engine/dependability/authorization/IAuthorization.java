/* The methods to be implemented by an authorization class
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

import java.util.Date;

import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IAuthorization {
	// Identities
	public void addIdentity(DigitalIdentity identity) throws SEPASecurityException;	
	public void removeIdentity(String uid) throws SEPASecurityException;
	public DigitalIdentity getIdentity (String uid) throws SEPASecurityException;
	public boolean isAuthorized(String identity) throws SEPASecurityException;
	
	// Credentials
	void storeCredentials(DigitalIdentity identity,String secret) throws SEPASecurityException;
	void removeCredentials(DigitalIdentity identity) throws SEPASecurityException;	
	boolean containsCredentials(String uid) throws SEPASecurityException;
	boolean checkCredentials(String uid, String secret) throws SEPASecurityException;
	Credentials getEndpointCredentials(String uid) throws SEPASecurityException;
	
	// JWT
	void addToken(String id,SignedJWT claims) throws SEPASecurityException;
	boolean containsToken(String id) throws SEPASecurityException;
	SignedJWT getToken(String uid) throws SEPASecurityException;
	
	// Token expiring
	Date getTokenExpiringDate(String id) throws SEPASecurityException;
	long getTokenExpiringPeriod(String id) throws SEPASecurityException;
	void setTokenExpiringPeriod(String id, long period) throws SEPASecurityException;
	void setDeviceExpiringPeriod(long period) throws SEPASecurityException;
	long getDeviceExpiringPeriod() throws SEPASecurityException;
	void setApplicationExpiringPeriod(long period) throws SEPASecurityException;
	long getApplicationExpiringPeriod() throws SEPASecurityException;
	void setUserExpiringPeriod(long period) throws SEPASecurityException;
	long getUserExpiringPeriod() throws SEPASecurityException;
	void setDefaultExpiringPeriod(long period) throws SEPASecurityException;
	long getDefaultExpiringPeriod() throws SEPASecurityException;
	
	// Token claims
	String getIssuer() throws SEPASecurityException;
	void setIssuer(String is) throws SEPASecurityException;
	String getHttpsAudience() throws SEPASecurityException;
	void setHttpsAudience(String audience) throws SEPASecurityException;
	String getWssAudience() throws SEPASecurityException;
	void setWssAudience(String audience) throws SEPASecurityException;
	String getSubject() throws SEPASecurityException;
	void setSubject(String sub) throws SEPASecurityException;		
}
