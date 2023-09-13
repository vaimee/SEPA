/* This class abstracts the aggregator client of the SEPA Application Design Pattern
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

import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.api.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public abstract class Aggregator extends Consumer implements IConsumer, IProducer {
	private final String sparqlUpdate;
	protected final String updateId;
	private final ForcedBindings updateForcedBindings;
	private final SPARQL11Protocol sparql11;
	
	public Aggregator(JSAP appProfile, String subscribeID, String updateID)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(appProfile, subscribeID);

		if (updateID == null) {
			Logging.logger.fatal("Update ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Update ID is null null"));
		}

		if (appProfile.getSPARQLUpdate(updateID) == null) {
			Logging.logger.fatal("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
		}

		updateId = updateID;

		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);

		updateForcedBindings = appProfile.getUpdateBindings(updateID);
		
		sparql11 = new SPARQL11Protocol(sm);
	}
	
	public final Response update() throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		return update(TIMEOUT,NRETRY);
	}

	public final Response update(long timeout,long nRetry) throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		String authorizationHeader = null;
		
		if (isSecure()) authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(updateId), appProfile.getUpdateProtocolScheme(updateId),appProfile.getUpdateHost(updateId), appProfile.getUpdatePort(updateId),
					appProfile.getUpdatePath(updateId), appProfile.addPrefixesAndReplaceBindings(sparqlUpdate, addDefaultDatatype(updateForcedBindings,updateId,false)),
					appProfile.getUsingGraphURI(updateId), appProfile.getUsingNamedGraphURI(updateId),authorizationHeader,timeout,nRetry);
		
		Logging.logger.trace("UPDATE "+req);
		
		 Response retResponse = sparql11.update(req);
		 
		 return retResponse;
	}

	public final void setUpdateBindingValue(String variable, RDFTerm value) throws SEPABindingsException {
		updateForcedBindings.setBindingValue(variable, value);
	}
}
