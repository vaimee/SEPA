/* This is the SEPA consumer interface
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public interface IConsumer extends ISubscriptionHandler {	
	void subscribe(long timeout) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException ;
	void unsubscribe(long timeout) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException;
	
	void onResults(ARBindingsResults results);
	void onAddedResults(BindingsResults results);
	void onRemovedResults(BindingsResults results);
}
