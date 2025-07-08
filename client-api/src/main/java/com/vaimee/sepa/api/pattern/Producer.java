/* This class implements a SEPA producer
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.RDFTerm;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.Response;

public class Producer extends Client implements IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	private ForcedBindings forcedBindings;
	private MultipleForcedBindings multipleForcedBindings;
	private SPARQL11Protocol client;

	public Producer(JSAP appProfile, String updateID)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(appProfile);

		if (appProfile.getSPARQLUpdate(updateID) == null) {
			Logging.logger.fatal("UPDATE ID [" + updateID + "] not found");
			throw new IllegalArgumentException("UPDATE ID [" + updateID + "] not found");
		}

		SPARQL_ID = updateID;

		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);

		forcedBindings = appProfile.getUpdateBindings(updateID);
		multipleForcedBindings = appProfile.getUpdateMultipleBindings(updateID);

		client = new SPARQL11Protocol(sm);
	}

	public final Response update()
			throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		return update(TIMEOUT,NRETRY);
	}

	public final Response update(long timeout,long nRetry)
			throws SEPASecurityException, SEPAPropertiesException, SEPABindingsException, SEPAProtocolException {
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID),
				appProfile.getUpdateProtocolScheme(SPARQL_ID), appProfile.getUpdateHost(SPARQL_ID),
				appProfile.getUpdatePort(SPARQL_ID), appProfile.getUpdatePath(SPARQL_ID),
				appProfile.addPrefixesAndReplaceBindings(sparqlUpdate,
						addDefaultDatatype(forcedBindings, SPARQL_ID, false)),
				appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),
				(appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null), timeout,nRetry);

		Logging.logger.trace(req);
		
		Response retResponse = client.update(req);

		Logging.logger.trace(retResponse);

		return retResponse;
	}

	public final Response multipleUpdate(long timeout,long nRetry)
			throws SEPASecurityException, SEPAPropertiesException, SEPABindingsException, SEPAProtocolException {
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID),
				appProfile.getUpdateProtocolScheme(SPARQL_ID), appProfile.getUpdateHost(SPARQL_ID),
				appProfile.getUpdatePort(SPARQL_ID), appProfile.getUpdatePath(SPARQL_ID),
				appProfile.addPrefixesAndReplaceMultipleBindings(sparqlUpdate,
						addDefaultDatatype(multipleForcedBindings.getBindings(), SPARQL_ID, false)),
				appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),
				(appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null), timeout,nRetry);

		Logging.logger.trace(req);

		Response retResponse = client.update(req);

		Logging.logger.trace(retResponse);

		return retResponse;
	}

	public final Response multipleUpdate()
			throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException {
		return multipleUpdate(TIMEOUT,NRETRY);
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (client != null) client.close();
	}

	public final void setUpdateBindingValue(String variable, RDFTerm value) throws SEPABindingsException {
		forcedBindings.setBindingValue(variable, value);
	}

	public final void setUpdateMultipleBindings(MultipleForcedBindings b) throws SEPABindingsException {
		multipleForcedBindings = b;
	}
	public final void addUpdateMultipleBindings(ArrayList<String> variables, ArrayList<ArrayList<RDFTerm>> values) throws SEPABindingsException {
		multipleForcedBindings.add(variables,values);
	}
	
	public final void addUpdateMultipleBindings(List<Bindings> b) throws SEPABindingsException {
		multipleForcedBindings.add(b);
	}
}
