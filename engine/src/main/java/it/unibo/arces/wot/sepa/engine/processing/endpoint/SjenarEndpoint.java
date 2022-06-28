package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.ar.UpdateResponseWithAR;
import it.unibo.arces.wot.sepa.logging.Logging;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.UpdateResult;
import org.apache.jena.system.Txn;

public class SjenarEndpoint implements SPARQLEndpoint {

	private static Dataset       dataset;
	private static boolean       hasInit;

	public  synchronized static void init() {
		if (hasInit == false) {

			dataset = JenaDatasetFactory.newInstance(EngineBeans.getFirstDatasetMode(), EngineBeans.getFirstDatasetPath(),true);
			hasInit = true;
		}
	}


	@Override
	public Response query(QueryRequest req,SEPAUserInfo usr) {
		init();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final RDFConnection conn = 
				(usr != null && usr.userName != null && usr.userName.trim().length() > 0 )      ?
						RDFConnection.connect(dataset,usr.userName)                              :
							RDFConnection.connect(dataset); 
				) {


			Txn.executeRead(conn, ()-> {
				ResultSet rs = conn.query(QueryFactory.create(req.getSPARQL())).execSelect();
				ResultSetFormatter.outputAsJSON(out, rs);
			});

			try {
				return new QueryResponse(out.toString(StandardCharsets.UTF_8.name()));
			} catch (UnsupportedEncodingException e) {
				Logging.logger.error("SjenarEndpoint.query error: "+e.getMessage());
				return new ErrorResponse(500, "UnsupportedEncodingException", e.getMessage());
			}
		}
	}

	@Override
	public Response update(UpdateRequest req,SEPAUserInfo usr) {
		init();

		try (final RDFConnection conn = 
				(usr != null && usr.userName != null && usr.userName.trim().length() > 0 )      ?
						RDFConnection.connect(dataset,usr.userName)                              :
							RDFConnection.connect(dataset); 
				) {


			final Set<Quad> updated = new TreeSet<>(new QuadComparator());
			final Set<Quad> removed = new TreeSet<>(new QuadComparator());
			Txn.executeWrite(conn, ()-> {
				final List<UpdateResult> lur = conn.update(req.getSPARQL());
				if (lur != null) {
					for(final UpdateResult ur : lur) {
						if (ur.deletedTuples != null) {
							for(final Quad q : ur.deletedTuples) {
								removed.add(q);
							}
						}

						if (ur.updatedTuples != null) {
							for(final Quad q : ur.updatedTuples) {
								updated.add(q);
							}
						}

					}

				}                    

			});


			return new UpdateResponseWithAR(removed,updated);
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

	public static synchronized void reset() {
		hasInit = false;
		dataset = null;
	}
}
