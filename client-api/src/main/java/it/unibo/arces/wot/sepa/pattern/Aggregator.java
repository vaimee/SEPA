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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public abstract class Aggregator extends Consumer implements IConsumer, IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	protected Bindings updateForcedBindings;

	private static final Logger logger = LogManager.getLogger("Aggregator");

	public Aggregator(JSAP appProfile, String subscribeID, String updateID, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, subscribeID, sm);

		if (updateID == null) {
			logger.fatal("Update ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Update ID is null null"));
		}

		if (appProfile.getSPARQLUpdate(updateID) == null) {
			logger.fatal("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
		}

		SPARQL_ID = updateID;

		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);

		updateForcedBindings = appProfile.getUpdateBindings(updateID);
	}

	public Aggregator(JSAP appProfile, String subscribeID, String updateID) throws SEPAProtocolException {
		super(appProfile, subscribeID);

		if (updateID == null) {
			logger.fatal("Update ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Update ID is null null"));
		}

		if (appProfile.getSPARQLUpdate(updateID) == null) {
			logger.fatal("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " + updateID + " not found in " + appProfile.getFileName());
		}

		SPARQL_ID = updateID;

		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);

		updateForcedBindings = appProfile.getUpdateBindings(updateID);
	}

	public final Response update() throws SEPASecurityException, IOException, SEPAPropertiesException {
		return update(0);
	}

	public final Response update(int timeout) throws SEPASecurityException, IOException, SEPAPropertiesException {
		String authorizationHeader = null;

		if (isSecure()) {
			authorizationHeader = sm.getAuthorizationHeader();
//			if (!getToken())
//				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Failed to get or renew token");
//			
//			if (appProfile.getAuthenticationProperties() != null)
//				authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		}

		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID),
				appProfile.getUpdateProtocolScheme(SPARQL_ID), appProfile.getUpdateHost(SPARQL_ID),
				appProfile.getUpdatePort(SPARQL_ID), appProfile.getUpdatePath(SPARQL_ID),
				prefixes() + replaceBindings(sparqlUpdate, updateForcedBindings),
				appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),
				authorizationHeader,timeout);

		return client.update(req);
	}

	public final void setUpdateBindingValue(String variable, RDFTerm value) throws IllegalArgumentException {
		updateForcedBindings.setBindingValue(variable, value);
	}
}
