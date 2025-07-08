/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.acl.storage;

import static com.vaimee.sepa.engine.acl.storage.Constants.initGroupsQuery;
import static com.vaimee.sepa.engine.acl.storage.Constants.initQuery;

import com.vaimee.sepa.engine.dependability.acl.storage.LocalDatasetActions;
import org.apache.jena.query.Dataset;

/**
 *
 * @author Lorenzo
 */
class CommonFuncs {
    public static void initACLDataset(final String dsName, final boolean fUseTDB2 ) throws Exception {
        //connect and clear dataset prior to testing
        Dataset ds = LocalDatasetFactory.newInstance(dsName, fUseTDB2);

        LocalDatasetActions.insertData(ds, initQuery);
        LocalDatasetActions.insertData(ds, initGroupsQuery);
        ds.close();
    }
    public static void initACLDataset(final String dsName, final boolean fUseTDB2,String iq, String igq ) throws Exception {
        //connect and clear dataset prior to testing
        Dataset ds = LocalDatasetFactory.newInstance(dsName, fUseTDB2);

        LocalDatasetActions.insertData(ds, iq);
        LocalDatasetActions.insertData(ds, igq);
        ds.close();
    }    
}
