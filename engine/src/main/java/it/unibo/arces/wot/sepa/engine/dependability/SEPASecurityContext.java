/* SEPA security context class
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

import com.nimbusds.jose.proc.SecurityContext;

/**
Security context. Provides additional information necessary for processing a JOSE object.
Example context information:

Identifier of the message producer (e.g. OpenID Connect issuer) to retrieve its public key to verify the JWS signature.
Indicator whether the message was received over a secure channel (e.g. TLS/SSL) which is essential for processing unsecured (plain) JOSE objects.
*/
 class SEPASecurityContext implements SecurityContext {
	
}
