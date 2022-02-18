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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import org.apache.jena.query.Dataset;

public class JenaInMemoryEndpoint implements SPARQLEndpoint{
	protected static final Logger logger = LogManager.getLogger();
	public enum datasetId {
            dsiPrimary,             //where to write first
            dsiAlternate,           //where to read first
        }
	//static final Dataset dataset = DatasetFactory.createTxnMem();
        private static final Dataset      primaryDataset    = DatasetFactory.createTxnMem();
        private static final Dataset      alternateDataset  = DatasetFactory.createTxnMem();
        
        private final Dataset             dataset;  
        
	private JenaInMemoryEndpoint (final Dataset src) {
            dataset = src;
        }
                
        public static JenaInMemoryEndpoint newInstanceda(final datasetId id) {
            JenaInMemoryEndpoint ret = null;
            switch(id) {
                case dsiAlternate:
                    ret = new JenaInMemoryEndpoint(alternateDataset);
                    break;
                case dsiPrimary:
                    ret = new JenaInMemoryEndpoint(primaryDataset);
                    break;
            }
            
            return ret;
        }
	@Override
	public Response query(QueryRequest req) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFConnection conn = RDFConnectionFactory.connect(dataset);
		Txn.executeRead(conn, ()-> {
			ResultSet rs = conn.query(QueryFactory.create(req.getSPARQL())).execSelect();
			ResultSetFormatter.outputAsJSON(out, rs);
		});
	 
		try {
			return new QueryResponse(out.toString(StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			return new ErrorResponse(500, "UnsupportedEncodingException", e.getMessage());
		}
	}

	@Override
	public Response update(UpdateRequest req) {
		RDFConnection conn = RDFConnectionFactory.connect(dataset);
		Txn.executeWrite(conn, ()-> {
			conn.update(req.getSPARQL());
		});
		return new UpdateResponse("Jena-in-memory-update");
	}

	@Override
	public void close() {
	}

}
