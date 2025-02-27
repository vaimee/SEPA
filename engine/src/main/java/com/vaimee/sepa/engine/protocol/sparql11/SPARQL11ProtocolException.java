/* SEPA exception on security
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

package com.vaimee.sepa.engine.protocol.sparql11;

public class SPARQL11ProtocolException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1400541672372477641L;
	private final int code;
    private final String body;

    public SPARQL11ProtocolException(int code, String body){
        this.code = code;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return body;
    }
}
