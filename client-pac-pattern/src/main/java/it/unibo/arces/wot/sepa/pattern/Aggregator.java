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

public abstract class Aggregator extends Consumer implements IAggregator {
	protected String sparqlUpdate = null;
	
	private static final Logger logger = LogManager.getLogger("Aggregator");
	
	public Aggregator(ApplicationProfile appProfile,String subscribeID,String updateID) throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile,subscribeID);
		
		if (appProfile == null || subscribeID == null || updateID == null){
			logger.fatal("Some arguments are null)");
			throw new IllegalArgumentException("Arguments can not be null");
		}
		
		if (appProfile.update(updateID) == null) {
			logger.fatal("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
		}
		
		sparqlUpdate = appProfile.update(updateID);
	} 

	public Response update(Bindings forcedBindings){	 
		 if (protocolClient == null || sparqlUpdate == null) {
			 logger.fatal("Aggregator not initialized");			 
			 return new ErrorResponse(400,"Aggregator not initialized");
		 }
		 
		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);		 		 
		 
		 return protocolClient.update(new UpdateRequest(sparql));
	 }
}
