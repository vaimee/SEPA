/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import static it.unibo.arces.wot.sepa.engine.acl.storage.CommonFuncs.initACLDataset;
import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initGroupsQuery;
import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initQuery;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @Test SEPA Acl operations
 */
public class SEPAAclTest {
    
    
    public SEPAAclTest() {
        
    }
    
    @Test 
    public void testNewInstanceDatasetTDB2() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();
        final String dsName = "./run/SEPAAclTdb2";
        
        try {
            Files.createDirectory(Path.of("./run"));
        } catch(Exception e ) {
            
        }
        
        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB2);
        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,dsName);
        
        initACLDataset(dsName,true);

        testNewInstanceDataset(paramMap,false);
    }
    
    @Test 
    public void testNewInstanceDatasetTDB1() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();
        final String dsName = "./run/SEPAAclTdb1";
        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB1);
        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,dsName);
        try {
            Files.createDirectory(Path.of("./run"));
        } catch(Exception e ) {
            
        }
        
        initACLDataset(dsName,false);
        
        testNewInstanceDataset(paramMap,false);
    }
    
    @Test 
    public void testNewInstanceDatasetMemory() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();
        
        testNewInstanceDataset(paramMap,true);
    }

    private void testNewInstanceDataset(final Map<String,Object> paramMap,boolean fMem ) throws Exception {
        System.out.println(this.getClass().getName() + "testNewInstanceDataset()");
        final ACLStorageOperations obj  = ACLStorageFactory.newInstance(ACLStorage.ACLStorageId.asiDataset, paramMap);
        assertNotEquals(null, obj);
        
        if (obj instanceof ACLStorageDataset && fMem) {
            //do a force load
            try {
                final Field f = ACLStorageDataset.class.getDeclaredField("storageDataset");
                f.setAccessible(true);
                final  Dataset ds = ( Dataset) f.get(obj);
                LocalDatasetActions.insertData(ds, initQuery);
                LocalDatasetActions.insertData(ds, initGroupsQuery);
                
            } catch(Exception e ) {
                System.err.println(e);
                fail("Unable to access datset field");
                return;
            }
            
        }
        
        final SEPAAcl as = SEPAAcl.getInstance(obj);
        
        testNewInstance(as);

    }
    
    
    private void testNewInstance(DatasetACL acl) {
        
    }
    
}
