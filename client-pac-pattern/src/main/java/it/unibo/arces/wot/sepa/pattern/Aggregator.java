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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public abstract class Aggregator extends Consumer implements IConsumer,IProducer {
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	
	private static final Logger logger = LogManager.getLogger("Aggregator");
	
	public Aggregator(ApplicationProfile appProfile,String subscribeID,String updateID) throws SEPAProtocolException, SEPASecurityException {
		super(appProfile,subscribeID);
		
		if (updateID == null){
			logger.fatal("Update ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Update ID is null null"));
		}
		
		if (appProfile.update(updateID) == null) {
			logger.fatal("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID " +updateID+" not found in "+appProfile.getFileName());
		}
		
		SPARQL_ID = updateID;
		
		sparqlUpdate = appProfile.update(updateID);
	} 

	public final Response update(Bindings forcedBindings){	 
		 if (protocolClient == null || sparqlUpdate == null) {
			 logger.fatal("Aggregator not initialized");			 
			 return new ErrorResponse(-1,400,"Aggregator not initialized");
		 }
		 
		 String sparql = prefixes() + replaceBindings(sparqlUpdate,forcedBindings);		 		 
		 
		 logger.debug("<UPDATE> "+ SPARQL_ID+" ==> "+sparql);
		 
		 return protocolClient.update(new UpdateRequest(sparql));
	 }
}
