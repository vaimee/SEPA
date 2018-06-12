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

package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class Producer extends Client implements IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	private Bindings forcedBindings;
	
	private static final Logger logger = LogManager.getLogger("Producer");
	
	private SPARQL11Protocol client;
	
	public Producer(JSAP appProfile,String updateID) throws SEPAProtocolException, SEPASecurityException  {
		super(appProfile);
		
		if (appProfile.getSPARQLUpdate(updateID) == null) {
			logger.fatal("UPDATE ID [" +updateID+"] not found in "+appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID [" +updateID+"] not found in "+appProfile.getFileName());
		}
		
		SPARQL_ID = updateID;
		
		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);
		
		forcedBindings = appProfile.getUpdateBindings(updateID);
		
		if (appProfile.getUpdateProtocolScheme(updateID).equals("https")) {
			client = new SPARQL11Protocol("sepa.jks", "sepa2017", "sepa2017");
			
		}
		else client = new SPARQL11Protocol();
	}
	
	public final Response update() throws SEPASecurityException, IOException, SEPAPropertiesException {
		return update(0);
	}
	
	public final Response update(int timeout) throws SEPASecurityException, IOException, SEPAPropertiesException{	 
		if(!getToken()) return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
				
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID), appProfile.getUpdateProtocolScheme(SPARQL_ID),appProfile.getUpdateHost(SPARQL_ID), appProfile.getUpdatePort(SPARQL_ID),
					appProfile.getUpdatePath(SPARQL_ID), prefixes() + replaceBindings(sparqlUpdate, forcedBindings), timeout,
					appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),appProfile.getAuthenticationProperties().getBearerAuthorizationHeader());
		 
		 return client.update(req);		 
	 }

	@Override
	public void close() throws IOException {
		client.close();
	}

	public final void setUpdateBindingValue(String variable, String value) throws IllegalArgumentException {
		forcedBindings.setBindingValue(variable, value);
		
	}
}
