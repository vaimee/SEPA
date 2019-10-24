/* The engine internal representation of a discard request
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

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public class InternalDiscardRequest extends InternalRequest {

    private final String original;
    private final ErrorResponse error;

    public InternalDiscardRequest(String original, ErrorResponse error,Credentials credentials){
    	super(credentials);
    	
        this.original = original;
        this.error = error;
    }

    public String getOriginal() {
        return original;
    }

    public ErrorResponse getError() {
        return error;
    }
}
