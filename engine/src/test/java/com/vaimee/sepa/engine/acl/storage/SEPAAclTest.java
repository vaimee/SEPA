/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.acl.storage;

import com.vaimee.sepa.engine.dependability.acl.SEPAAcl;

import static com.vaimee.sepa.engine.acl.storage.CommonFuncs.initACLDataset;
import static com.vaimee.sepa.engine.acl.storage.Constants.GRAPH1;
import static com.vaimee.sepa.engine.acl.storage.Constants.GRAPH3;
import static com.vaimee.sepa.engine.acl.storage.Constants.GRAPH5;
import static com.vaimee.sepa.engine.acl.storage.Constants.USER1;
import static com.vaimee.sepa.engine.acl.storage.Constants.USER2;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.vaimee.sepa.engine.dependability.acl.storage.*;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.junit.jupiter.api.Test;

/**
 *
 * Test SEPA Acl operations
 *
 */
public class SEPAAclTest {
    
//     private final String initQuery = "PREFIX sepaACL: <http://acl.sepa.com/>"          + System.lineSeparator() +
//                            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() +
//                            "INSERT DATA { GRAPH sepaACL:acl { "                        + System.lineSeparator() +
//                            "   sepaACL:monger"                                         + System.lineSeparator() +
//                            "       sepaACL:userName    \"monger\" ;"                   + System.lineSeparator() +
//                            "       sepaACL:memberOf	\"group1\" ;"                   + System.lineSeparator() +
//                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() +
//                            "           sepaACL:graphName   	mp:graph1;"             + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:query;"         + System.lineSeparator() +
//                            "       ];"                                                 + System.lineSeparator() +
//                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() +
//                            "           sepaACL:graphName   	mp:graph5;"             + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:query  "        + System.lineSeparator() +
//                            "       ]."                                                 + System.lineSeparator() +
//
//                            "   sepaACL:gonger"                                         + System.lineSeparator() +
//                            "       sepaACL:userName    \"gonger\" ;"                   + System.lineSeparator() +
//                            "       sepaACL:memberOf	\"group2\" ;"                   + System.lineSeparator() +
//                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() +
//                            "           sepaACL:graphName   	mp:graph2;"             + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:update;"        + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:insertData;"    + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:deleteData;"    + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:query"          + System.lineSeparator() +
//                            "   ];"                                                     + System.lineSeparator() +
//                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() +
//                            "           sepaACL:graphName   	mp:graph6;"             + System.lineSeparator() +
//                            "           sepaACL:allowedRight	sepaACL:query     "     + System.lineSeparator() +
//                            "   ]."                                                     + System.lineSeparator() +
//
//                            "}}"                                                        + System.lineSeparator();
//
//     private final String initGroupsQuery = "PREFIX sepaACL: <http://acl.sepa.com/>"            + System.lineSeparator() +
//                            "PREFIX sepaACLGroups: <http://groups.acl.sepa.com/>"               + System.lineSeparator() +
//                            "PREFIX mp: <http://mysparql.com/>"                                 + System.lineSeparator() +
//                            ""                                                                  + System.lineSeparator() +
//                            "INSERT DATA { GRAPH sepaACL:aclGroups {"                           + System.lineSeparator() +
//                            "    sepaACLGroups:group1		 "                              + System.lineSeparator() +
//                            "    sepaACL:groupName    \"group1\" ; "                            + System.lineSeparator() +
//                            "    sepaACL:accessInformation 	["                              + System.lineSeparator() +
//                            "       sepaACL:graphName   	mp:graph1;"                     + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:update;"                + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:query  "                + System.lineSeparator() +
//                            "   ];"                                                             + System.lineSeparator() +
//                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() +
//                            "       sepaACL:graphName   	mp:graph3;"                     + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:query  "                + System.lineSeparator() +
//                            "   ]."                                                             + System.lineSeparator() +
//
//                            ""                                                                  + System.lineSeparator() +
//                            ""                                                                  + System.lineSeparator() +
//                            "   sepaACLGroups:group2"                                           + System.lineSeparator() +
//                            "   sepaACL:groupName    \"group2\" ; "                             + System.lineSeparator() +
//                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() +
//                            "       sepaACL:graphName   	mp:graph2;"                     + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:update;"                + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:query     "             + System.lineSeparator() +
//                            "   ];"                                                             + System.lineSeparator() +
//                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() +
//                            "       sepaACL:graphName   	mp:graph4;"                     + System.lineSeparator() +
//                            "       sepaACL:allowedRight	sepaACL:query     "             + System.lineSeparator() +
//                            "   ]."                                                             + System.lineSeparator() +
//
//                            ""                                                                  + System.lineSeparator() +
//                            ""                                                                  + System.lineSeparator() +
//                            "}}";
    
    public SEPAAclTest() {
        
    }

    @Test
    public void testNewInstanceDatasetMemory() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();

        testNewInstanceDataset(paramMap,true);
    }
    
    //@Test
    public void testNewInstanceDatasetTDB2() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();
        final String dsName = "./run/SEPAAclTdb2";
        
        try {
            Files.createDirectory(Path.of("./run"));
        } catch(Exception ignored) {
            
        }
        
        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB2);
        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,dsName);
        
        initACLDataset(dsName,true,Constants.initQuery,Constants.initGroupsQuery);

        testNewInstanceDataset(paramMap,false);
    }
    
    //@Test
    public void testNewInstanceDatasetTDB1() throws Exception {
        final Map<String,Object> paramMap = new TreeMap<>();
        final String dsName = "./run/SEPAAclTdb1";

        try {
            Files.createDirectory(Path.of("./run"));
        } catch(Exception ignored) {

        }

        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB1);
        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,dsName);

        initACLDataset(dsName,false,Constants.initQuery,Constants.initGroupsQuery);
        
        testNewInstanceDataset(paramMap,false);
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
                LocalDatasetActions.insertData(ds, Constants.initQuery);
                LocalDatasetActions.insertData(ds, Constants.initGroupsQuery);
                
            } catch(Exception e ) {
                System.err.println(e);
                fail("Unable to access dataset field");
                return;
            }
            
        }
        
        final SEPAAcl as = SEPAAcl.newInstance(obj);
        
        testNewInstance(as);
    }
    
    
    private void testNewInstance(SEPAAcl acl) {
        //pass through group AND USER
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH1), USER1));
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH1), USER1));
        //pass through group only
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH3), USER1));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH3), USER1));
        //pass through user only
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH5), USER1));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH5), USER1));
        
        //removes group from user
        acl.removeUserFromGroup(USER1, Constants.GROUP1);
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH1), USER1));

        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH1), USER1));

        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH3), USER1));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH3), USER1));
        assertTrue(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH5), USER1));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiUpdate,expandDataGraph(GRAPH5), USER1));
        
        //cross check
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH1), USER2));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH3), USER2));
        assertFalse(acl.checkGraphBase(DatasetACL.aclId.aiQuery,expandDataGraph(GRAPH5), USER2));
    }
    
    private String expandUri(String pfixedUri, String pfixName, String pfixValue) {
        return pfixedUri.replaceAll(pfixName + ":", pfixValue);
    }
    private String expandDataGraph(String graphName) {
        return expandUri(graphName,"mp","http://mysparql.com/");
    }
}
