/* JMX interface for the authorization manager
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

package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.HashMap;

public interface AuthorizationManagerMBean {
	void addAuthorizedIdentity(String id);

	void removeAuthorizedIdentity(String id);

	HashMap<String, Boolean> getAuthorizedIdentities();

	long getTokenExpiringPeriod();

	void setTokenExpiringPeriod(long period);

	String getIssuer();

	void setIssuer(String issuer);

	String getHttpsAudience();

	void setHttpsAudience(String audience);

	String getWssAudience();

	void setWssAudience(String audience);

	String getSubject();

	void setSubject(String sub);
}
