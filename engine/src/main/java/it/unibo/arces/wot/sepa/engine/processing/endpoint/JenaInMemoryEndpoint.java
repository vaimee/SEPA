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
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.UpdateResult;

/**
 * 
 * Special Graph Names URI Meaning urn:x-arq:UnionGraph The RDF merge of all the
 * named graphs in the datasets of the query. urn:x-arq:DefaultGraph The default
 * graph of the dataset, used when the default graph of the query is the union
 * graph.
 * 
 * Note that setting tdb:unionDefaultGraph does not affect the default graph or
 * default model obtained with dataset.getDefaultModel().
 * 
 * The RDF merge of all named graph can be accessed as the named graph
 * urn:x-arq:UnionGraph using Dataset.getNamedModel("urn:x-arq:UnionGraph") .
 * 
 * An RDF Dataset is a collection of one, unnamed, default graph and zero, or
 * more named graphs. In a SPARQL query, a query pattern is matched against the
 * default graph unless the GRAPH keyword is applied to a pattern.
 * 
 * Transactions are part of the interface to RDF Datasets. There is a default
 * implementation, based on MRSW locking (multiple-reader or single-writer) that
 * can be used with any mixed set of components. Certain storage sub-systems
 * provide better concurrency with MR+SW (multiple-read and single writer).
 * 
 * Dataset Facilities Creation TxnMem MR+SW DatasetFactory.createTxnMem TDB
 * MR+SW, persistent TDBFactory.create TDB2 MR+SW, persistent TDB2Factory.create
 * General MRSW DatasetFactory.create
 * 
 * The general dataset can have any graphs added to it (e.g. inference graphs).
 */

public class JenaInMemoryEndpoint implements SPARQLEndpoint {
	protected static final Logger logger = LogManager.getLogger();

	private static Dataset dataset;
	private static boolean hasInit;

	private synchronized static void init() {
		if (hasInit == false) {

			dataset = JenaDatasetFactory.newInstance(EngineBeans.getFirstDatasetMode(),
					EngineBeans.getFirstDatasetPath(), true);
			hasInit = true;
		}
	}

	@Override
	public Response query(QueryRequest req, SEPAUserInfo usr) {
		init();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final RDFConnection conn = (usr != null && usr.userName != null && usr.userName.trim().length() > 0)
				? RDFConnectionFactory.connect(dataset, usr.userName)
				: RDFConnectionFactory.connect(dataset);) {

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
	}

	@Override

	public Response update(UpdateRequest req, SEPAUserInfo usr) {
		init();

		try (final RDFConnection conn = (usr != null && usr.userName != null && usr.userName.trim().length() > 0)
				? RDFConnectionFactory.connect(dataset, usr.userName)
				: RDFConnectionFactory.connect(dataset);) {

			final Set<Quad> updated = new TreeSet<>(new QuadComparator());
			final Set<Quad> removed = new TreeSet<>(new QuadComparator());
			Txn.executeWrite(conn, () -> {
				final List<UpdateResult> lur = conn.update(req.getSPARQL());
				if (lur != null) {
					for (final UpdateResult ur : lur) {
						if (ur.deletedTuples != null) {
							for (final Quad q : ur.deletedTuples) {
								removed.add(q);
							}
						}

						if (ur.updatedTuples != null) {
							for (final Quad q : ur.updatedTuples) {
								updated.add(q);
							}
						}

					}

				}

			});

			return new UpdateResponse(removed, updated);
		}
	}

	@Override
	public void close() {
	}

	private class QuadComparator implements Comparator<Quad> {

		@Override
		public int compare(Quad o1, Quad o2) {
			return o1.toString().compareTo(o2.toString());
		}

	}

}
