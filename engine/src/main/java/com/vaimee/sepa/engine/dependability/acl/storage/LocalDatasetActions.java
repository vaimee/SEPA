/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.dependability.acl.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
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

    public static ResultSet query(String query,RDFConnection conn) {
            System.out.println("[QUERY] : "+ System.lineSeparator() + query + System.lineSeparator());
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            final RSHolder rsh = new RSHolder();
            Txn.executeRead(conn, ()-> {
                    final ResultSet rs = conn.query(QueryFactory.create(query)).execSelect();
                    rsh.rs = rs.rewindable();
                    ResultSetFormatter.outputAsJSON(out, rs);
            });

        System.out.println("Query output : " + out.toString(StandardCharsets.UTF_8));

        return rsh.rs;
    }

    public static void update(String query,RDFConnection conn) {
            System.out.println("[UPDATE] : " + System.lineSeparator() + query + System.lineSeparator() );
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
        dataset.begin(ReadWrite.WRITE);
        System.out.println("[INSERT] : "+ System.lineSeparator() + query + System.lineSeparator());
        UpdateAction.parseExecute(query, dataset,null);
        RDFDataMgr.write(System.out, dataset, Lang.TRIG);
        dataset.commit();
            
    }
    
}
