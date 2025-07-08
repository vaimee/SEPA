/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.acl.storage;

import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.tdb.TDBFactory;

/**
 *
 * @author Lorenzo
 */
public class  LocalDatasetFactory {
        public static Dataset newInstance(String name,boolean fUseTDB2) {
            Dataset ret = null;

            if (fUseTDB2) {
                final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create(name);
                ret = TDB2Factory.connectDataset(loc);
            }
            else {
                final Location loc = Location.create(name);
                ret = TDBFactory.createDataset(loc);
            }

            if (ret.asDatasetGraph() != null) {
                ret.begin(ReadWrite.WRITE);
                ret.asDatasetGraph().clear();
                ret.commit();
            }
            
            return ret;

        }
        public static Dataset newInstance(String name,DatasetACL acl,boolean fUseTDB2) {
            
            Dataset ret = null;

            if (fUseTDB2) {
                final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create("./" + name + "_tdb2");
                ret = TDB2Factory.connectDataset(loc);

            } else {
                final Location loc = Location.create("./" + name + "_tdb1");
                ret = TDBFactory.createDataset(loc);

            }

            if (ret.asDatasetGraph() != null) {
                ret.begin(ReadWrite.WRITE);
                ret.asDatasetGraph().clear();
                ret.commit();
            }
            
            return ret;

        }
        
}
