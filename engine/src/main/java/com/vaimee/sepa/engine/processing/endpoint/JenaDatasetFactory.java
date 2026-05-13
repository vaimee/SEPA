/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.core.EngineProperties;
import com.vaimee.sepa.engine.dependability.acl.ACLTools;
import com.vaimee.sepa.engine.dependability.acl.SEPAAcl;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Lorenzo
 */
public class JenaDatasetFactory {
     private static void ensureDirectoryExists(String path) {
         try {
             Files.createDirectories(Path.of(path));
         } catch (IOException e) {
             throw new IllegalStateException("Unable to create Jena dataset directory: " + path, e);
         }
     }

     public static Dataset newInstance(String mode,String path,DatasetACL acl) {
         Dataset ret = null;
         switch(mode.trim().toLowerCase()) {
             case EngineProperties.DS_MODE_MEM:
             default:
                 ret = DatasetFactory.createTxnMem(acl);
                 break;
                 
             case EngineProperties.DS_MODE_TDB1: {
                                     ensureDirectoryExists(path);
                final org.apache.jena.tdb.base.file.Location loc = org.apache.jena.tdb.base.file.Location.create(path);
                ret = org.apache.jena.tdb.TDBFactory.createDataset(loc, acl);
                 
                 break;
             }
             
             case EngineProperties.DS_MODE_TDB2: {
                                     ensureDirectoryExists(path);
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
                    ret = DatasetFactory.createTxnMem();
                    break;

                case EngineProperties.DS_MODE_TDB1: {
                   ensureDirectoryExists(path);
                   final org.apache.jena.tdb.base.file.Location loc = org.apache.jena.tdb.base.file.Location.create(path);
                   ret = org.apache.jena.tdb.TDBFactory.createDataset(loc);

                    break;
                }

                case EngineProperties.DS_MODE_TDB2: {
                   ensureDirectoryExists(path);
                   final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create(path);
                   ret = org.apache.jena.tdb2.TDB2Factory.connectDataset(loc);       
                   break;
                }
            }
         }         
         return ret;
     }
     
}
