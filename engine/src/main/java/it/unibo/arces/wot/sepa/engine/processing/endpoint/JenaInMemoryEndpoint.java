/* This class implements a JENA base in-memory RDF data set
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

package it.unibo.arces.wot.sepa.engine.processing.endpoint;


import org.apache.jena.query.Dataset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;

public class JenaInMemoryEndpoint implements SPARQLEndpoint{
	protected static final Logger logger = LogManager.getLogger();
	
    	public static synchronized void reset() {
		hasInit = false;
		dataset = null;
	}

    private static Dataset       dataset;
	private static boolean       hasInit;

	public  synchronized static void init() {
            if (hasInit == false) {
                dataset = JenaDatasetFactory.newInstance(EngineBeans.getFirstDatasetMode(), EngineBeans.getFirstDatasetPath(),true);
                hasInit = true;
            }
	}

	
	@Override
	public Response query(QueryRequest req,SEPAUserInfo notUsed) {
            init();
            return EndpointBasicOps.query(req, dataset);

	}

	@Override
	public Response update(UpdateRequest req,SEPAUserInfo notUsed) {
            init();
            return EndpointBasicOps.update(req, dataset);
	}

	@Override
	public void close() {
	}

}