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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

/**
 * 
Special Graph Names
URI	Meaning
urn:x-arq:UnionGraph	The RDF merge of all the named graphs in the datasets of the query.
urn:x-arq:DefaultGraph	The default graph of the dataset, used when the default graph of the query is the union graph.

Note that setting tdb:unionDefaultGraph does not affect the default graph or default model obtained with dataset.getDefaultModel().

The RDF merge of all named graph can be accessed as the named graph urn:x-arq:UnionGraph using Dataset.getNamedModel("urn:x-arq:UnionGraph") .
 
An RDF Dataset is a collection of one, unnamed, default graph and zero, or more named graphs. 
In a SPARQL query, a query pattern is matched against the default graph unless the GRAPH keyword is applied to a pattern.

Transactions are part of the interface to RDF Datasets. There is a default implementation, based on MRSW locking (multiple-reader or single-writer) that can be used with any mixed set of components. Certain storage sub-systems provide better concurrency with MR+SW (multiple-read and single writer).

Dataset		Facilities			Creation
TxnMem		MR+SW				DatasetFactory.createTxnMem
TDB			MR+SW, persistent	TDBFactory.create
TDB2		MR+SW, persistent	TDB2Factory.create
General		MRSW				DatasetFactory.create

The general dataset can have any graphs added to it (e.g. inference graphs).
 * */
public class JenaInMemoryEndpoint implements SPARQLEndpoint {
	static final Dataset dataset = DatasetFactory.create(); // DatasetFactory.createTxnMem();

	@Override
	public Response query(QueryRequest req) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFConnection conn = RDFConnection.connect(dataset);
		Txn.executeRead(conn, () -> {
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
		RDFConnection conn = RDFConnection.connect(dataset);
		Txn.executeWrite(conn, () -> {
			conn.update(req.getSPARQL());
		});
		return new UpdateResponse("Jena-in-memory-update");
	}

	@Override
	public void close() {
	}

}
