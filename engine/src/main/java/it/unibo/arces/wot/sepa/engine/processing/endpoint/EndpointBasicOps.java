/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;

/**
 *
 * @author Lorenzo
 */
public class EndpointBasicOps {
    public static Response query(QueryRequest req,Dataset dataset) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFConnection conn = RDFConnection.connect(dataset);
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

    public static Response update(UpdateRequest req,Dataset dataset) {
            RDFConnection conn = RDFConnection.connect(dataset);
            Txn.executeWrite(conn, ()-> {
                    conn.update(req.getSPARQL());
            });
            return new UpdateResponse("Jena-in-memory-update");
    }

    
}
