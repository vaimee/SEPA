/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.query.Dataset;
import static org.apache.jena.query.QueryExecution.dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.modify.UpdateResult;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateAction;

/**
 *
 * @author Lorenzo
 */
public class LocalDatasetActions {
    private static class RSHolder {
        public  ResultSet       rs;
    }
    public static ResultSet query(Dataset dataset, String query,RDFConnection conn) {
        return query(dataset, query,null,conn);
    }
    public static ResultSet query(Dataset dataset, String query,String userName,RDFConnection conn) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            final RSHolder rsh = new RSHolder();
            Txn.executeRead(conn, ()-> {
                    final ResultSet rs = conn.query(QueryFactory.create(query)).execSelect();
                    rsh.rs = rs.rewindable();
                    ResultSetFormatter.outputAsJSON(out, rs);
            });

            try {
                    System.out.println("Query output : " + out.toString(StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                    System.err.println(e);
            }
            
            return rsh.rs;
    }
    public static void update(Dataset dataset, String query,RDFConnection conn) {
        update(dataset,query,null,conn);
    }
    public static void update(Dataset dataset, String query,String userName,RDFConnection conn) {
            Txn.executeWrite(conn, ()-> {
                    final List<UpdateResult> ur = conn.update(query);
                    if (ur != null) {
                        System.out.println("Update output ");
                        for(final UpdateResult u : ur ) {
                            System.out.println("******************");
                            System.out.println(u == null ? "<null>" : u.toString());
                        }
                    }
            });
            
    }
    public static void insertData(Dataset dataset, String query) {
        UpdateAction.parseExecute(query, dataset,null);
        RDFDataMgr.write(System.out, dataset, Lang.TRIG);
            
    }
    
}
