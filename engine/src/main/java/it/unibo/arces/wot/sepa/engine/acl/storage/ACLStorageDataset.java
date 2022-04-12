/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageDataset implements ACLStorage{ 
    public static final String PARAM_DATASETPATH                    = "acl.dataset.path";
    public static final String PARAM_DATASETPERSISTENCY             = "acl.dataset.persistency";
    
    public static final String PARAM_DATASETPERSISTENCY_VALUE_TDB1  = "tdb1";
    public static final String PARAM_DATASETPERSISTENCY_VALUE_TDB2  = "tdb2";
    public static final String PARAM_DATASETPERSISTENCY_VALUE_NONE  = "none";
    
    
    private static final       Map<String,String>                   paramsInfo = new TreeMap<String,String>();
    
    
    static {
        paramsInfo.put(PARAM_DATASETPATH, "Parh of persistent dataset, if persitency has been enabled");
        paramsInfo.put(
                PARAM_DATASETPERSISTENCY, 
                "Type of dataset persistency, can be one of the following : " + 
                PARAM_DATASETPERSISTENCY_VALUE_NONE + "," +
                PARAM_DATASETPERSISTENCY_VALUE_TDB1 + "," + 
                PARAM_DATASETPERSISTENCY_VALUE_TDB2
        );
    }
    private final Map<String,Object>    params;
    private final Dataset      storageDataset;
    
    public ACLStorageDataset(Map<String,Object> params ) throws ACLStorageException{
        final String persName = (String) params.get(PARAM_DATASETPERSISTENCY );
        this.params = params;
        if (persName == null || persName.trim().length() == 0 ) {
            //goes to mem dataset
            storageDataset = DatasetFactory.createTxnMem();
        } else {
            final String dsPath = (String) params.get(PARAM_DATASETPATH );
            if (dsPath == null || dsPath.trim().length() == 0 ) 
                throw new ACLStorageException("Invalid or missing " + PARAM_DATASETPATH,ACLStorageId.asiDataset, params);
            
            switch(persName.trim().toLowerCase()) {
                case PARAM_DATASETPERSISTENCY_VALUE_NONE:
                    storageDataset = DatasetFactory.createTxnMem();
                    break;
                case PARAM_DATASETPERSISTENCY_VALUE_TDB1: {
                    final org.apache.jena.tdb.base.file.Location loc = org.apache.jena.tdb.base.file.Location.create(dsPath);
                    storageDataset = org.apache.jena.tdb.TDBFactory.createDataset(loc);
                    
                    break;
                }
                case PARAM_DATASETPERSISTENCY_VALUE_TDB2: {
                    final org.apache.jena.dboe.base.file.Location loc = org.apache.jena.dboe.base.file.Location.create(dsPath);
                    storageDataset = org.apache.jena.tdb2.TDB2Factory.connectDataset(loc);       

                    break;
                }                
                default:
                    throw new ACLStorageException("Invalid " + PARAM_DATASETPERSISTENCY  + " = " + persName ,ACLStorageId.asiDataset, params);                    
            }
        }
    }

    @Override
    public void removeUser(String user) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeUserPermissions(String user,String graph) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addUser(String user) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addUserPermission(String user, String graph,DatasetACL.aclId id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String,Map<String,Set<DatasetACL.aclId>>>  load() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public Map<String, String> getParamsInfo() {
        return paramsInfo;
    }

    @Override
    public void removeUserPermission(String user, String graph, DatasetACL.aclId id) throws ACLStorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
