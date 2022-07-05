/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

/**
 *
 * @author Lorenzo
 */
public class JenaDatasetFactory {
     public static Dataset newInstance(String mode,String path,DatasetACL acl) {
         Dataset ret = null;
         switch(mode.trim().toUpperCase()) {
             case EngineProperties.DS_MODE_MEM:
             default:
                 ret = DatasetFactory.createTxnMem(acl);
                 break;
                 
             case EngineProperties.DS_MODE_TDB1: {
                final org.apache.jena.tdb.base.file.Location loc = org.apache.jena.tdb.base.file.Location.create(path);
                ret = org.apache.jena.tdb.TDBFactory.createDataset(loc, acl);
                 
                 break;
             }
             
             case EngineProperties.DS_MODE_TDB2: {
                final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create(path);
                ret = org.apache.jena.tdb2.TDB2Factory.connectDataset(loc,acl);       
                break;
             }
         }
         
         return ret;
     }
     
     public static Dataset newInstance(String mode,String path,boolean useACLIfPossible) {
         
         
         Dataset ret = null;
         
         if (EngineBeans.isAclEnabled() && useACLIfPossible) {
             final DatasetACL acl = SEPAAcl.getInstance(ACLTools.makeACLStorage());
             ret = newInstance(mode, path,acl);
         } else {
            switch(mode.trim().toLowerCase()) {
                case EngineProperties.DS_MODE_MEM:
                default:
                    ret = DatasetFactory.createTxnMem();
                    break;

                case EngineProperties.DS_MODE_TDB1: {
                   final org.apache.jena.tdb.base.file.Location loc = org.apache.jena.tdb.base.file.Location.create(path);
                   ret = org.apache.jena.tdb.TDBFactory.createDataset(loc);

                    break;
                }

                case EngineProperties.DS_MODE_TDB2: {
                   final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create(path);
                   ret = org.apache.jena.tdb2.TDB2Factory.connectDataset(loc);       
                   break;
                }
            }
         }         
         return ret;
     }
     
}
