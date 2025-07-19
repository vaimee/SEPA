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

package com.vaimee.sepa.api.pattern;

import com.vaimee.sepa.api.commons.sparql.RDFTerm;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.logging.Timings;
import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.Response;

import java.util.ArrayList;

public abstract class Aggregator extends Consumer implements IConsumer, IProducer {
	private final String sparqlUpdate;
	protected final String updateId;
	private final ForcedBindings updateForcedBindings;
	private final SPARQL11Protocol sparql11;
	private MultipleForcedBindings multipleForcedBindings;
	public Aggregator(JSAP appProfile, String subscribeID, String updateID)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(appProfile, subscribeID);

		if (updateID == null) {
			Logging.getLogger().fatal("Update ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Update ID is null null"));
		}

		if (appProfile.getSPARQLUpdate(updateID) == null) {
			Logging.getLogger().fatal("UPDATE ID " + updateID + " not found");
			throw new IllegalArgumentException("UPDATE ID " + updateID + " not found");
		}

		updateId = updateID;
		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);

		updateForcedBindings = appProfile.getUpdateBindings(updateID);
		multipleForcedBindings = appProfile.getUpdateMultipleBindings(updateID);

		sparql11 = new SPARQL11Protocol(sm);
	}
	
	public final Response update() throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		return update(TIMEOUT,NRETRY);
	}

	public final Response update(long timeout,long nRetry) throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		String authorizationHeader = null;
		
		if (isSecure()) authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(updateId), appProfile.getUpdateProtocolScheme(updateId),appProfile.getUpdateHost(updateId), appProfile.getUpdatePort(updateId),
					appProfile.getUpdatePath(updateId), appProfile.addPrefixesAndReplaceBindings(sparqlUpdate, addUpdateDefaultDatatype(updateForcedBindings,updateId,false)),
					appProfile.getUsingGraphURI(updateId), appProfile.getUsingNamedGraphURI(updateId),authorizationHeader,timeout,nRetry);
		
		Logging.getLogger().trace("UPDATE "+req);
		
		 Response retResponse = sparql11.update(req);
		 
		 return retResponse;
	}

	public final Response multipleUpdate(long timeout,long nRetry)
			throws SEPASecurityException, SEPAPropertiesException, SEPABindingsException, SEPAProtocolException {
		long start = Timings.getTime();
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(updateId),
				appProfile.getUpdateProtocolScheme(updateId), appProfile.getUpdateHost(updateId),
				appProfile.getUpdatePort(updateId), appProfile.getUpdatePath(updateId),
				appProfile.addPrefixesAndReplaceMultipleBindings(sparqlUpdate,
						addUpdateDefaultDatatype(multipleForcedBindings.getBindings(), updateId, false)),
				appProfile.getUsingGraphURI(updateId), appProfile.getUsingNamedGraphURI(updateId),
				(appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null), timeout,nRetry);
		long stop = Timings.getTime();
		Timings.log("multipleUpdate create UpdateRequest", start, stop);
		
		Logging.getLogger().trace(req);

		Response retResponse = sparql11.update(req);

		Logging.getLogger().trace(retResponse);

		return retResponse;
	}

	public final Response multipleUpdate()
			throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		return multipleUpdate(TIMEOUT,NRETRY);
	}

	public final void setUpdateBindingValue(String variable, RDFTerm value) throws SEPABindingsException {
		updateForcedBindings.setBindingValue(variable, value);
	}

	public final void setUpdateMultipleBindings(ArrayList<String> variables, ArrayList<ArrayList<RDFTerm>> values) throws SEPABindingsException {
		multipleForcedBindings.add(variables,values);
	}
}
