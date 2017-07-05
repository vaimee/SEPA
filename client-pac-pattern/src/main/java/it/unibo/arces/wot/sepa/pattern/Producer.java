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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class Producer extends Client implements IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	
	private static final Logger logger = LogManager.getLogger("GenericClient");
	
	public Producer(ApplicationProfile appProfile,String updateID) throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile);
		
		if (appProfile == null) {
			logger.fatal("Application profile is null)");
			throw new IllegalArgumentException("Application profile is null");
		}
		
		if (appProfile.update(updateID) == null) {
			logger.fatal("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
		}
		
		SPARQL_ID = updateID;
		
		sparqlUpdate = appProfile.update(updateID);
	}
	
	public boolean update(Bindings forcedBindings){	 
		 if (sparqlUpdate == null || protocolClient == null) {
			 logger.fatal("Producer not initialized");
			 return false;
		 }

		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);
		 
		 logger.debug("<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 Response response = protocolClient.update(new UpdateRequest(sparql));

		 
		 return !(response.getClass().equals(ErrorResponse.class));
		 
	 }
}
