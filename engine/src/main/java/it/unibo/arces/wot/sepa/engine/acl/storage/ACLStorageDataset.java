/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl.UserData;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageDataset implements ACLStorageOperations { 
    public static final String PARAM_DATASETPATH                    = "acl.dataset.path";
    public static final String PARAM_DATASETPERSISTENCY             = "acl.dataset.persistency";
    
    public static final String PARAM_DATASETPERSISTENCY_VALUE_TDB1  = "tdb1";
    public static final String PARAM_DATASETPERSISTENCY_VALUE_TDB2  = "tdb2";
    public static final String PARAM_DATASETPERSISTENCY_VALUE_NONE  = "none";
    
    
    private static final       Map<String,String>                   paramsInfo = new TreeMap<>();
    private static final       Map<String,DatasetACL.aclId>         aclLookupMap = new TreeMap<>();
    private static final       Map<DatasetACL.aclId,String>         aclRevLookupMap = new TreeMap<>();
    
    
    //here are defined all ontologies
    private static final  String    SEPACL_NS_PFIX              =          "<http://acl.sepa.com/>";
    private static final  String    SEPACL_NS_GRP_PFIX          =          "<http://groups.acl.sepa.com/>";
    private static final  String    SEPACL_GRAPH_NAME           =          "sepaACL:acl";
    private static final  String    SEPACL_GRAPH_GROUP_NAME     =          "sepaACL:aclGroups";
    
    private static String encodeUriRight(DatasetACL.aclId id) {
        final String ret = aclRevLookupMap.get(id);
        return ret;
    }
    private static DatasetACL.aclId decodeUriRight(String uri) {

        final DatasetACL.aclId ret = aclLookupMap.get(uri);
        return ret;
    }
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
    
    private static void addAclIdElement(String name, DatasetACL.aclId id) {
        aclLookupMap.put(name, id);
        final String BASE = SEPACL_NS_PFIX.substring(1,SEPACL_NS_PFIX .length() - 1);
        
        aclRevLookupMap.put(id, name.substring(BASE.length()));
    }
    static {
        final String BASE = SEPACL_NS_PFIX.substring(1,SEPACL_NS_PFIX .length() - 1);
        final String QUERY  = BASE +  "query";
        final String UPDATE  = BASE +  "update";
        final String CLEAR  = BASE +  "clear";
        final String CREATE  = BASE +  "create";
        final String DROP  = BASE +  "drop";
        final String INSERTDATA  = BASE +  "insertData";
        final String DELETEDATA  = BASE +  "deleteData";

        addAclIdElement(QUERY, DatasetACL.aclId.aiQuery);
        addAclIdElement(UPDATE, DatasetACL.aclId.aiUpdate);
        addAclIdElement(CLEAR , DatasetACL.aclId.aiClear);
        addAclIdElement(CREATE, DatasetACL.aclId.aiCreate);
        addAclIdElement(DROP, DatasetACL.aclId.aiDrop);
        addAclIdElement(INSERTDATA, DatasetACL.aclId.aiDeleteData);
        addAclIdElement(DELETEDATA, DatasetACL.aclId.aiInsertData);
        
        
        
    }
    
    private final Map<String,Object>    params;
    private final Dataset               storageDataset;
    private final RDFConnection         dsConnection;
    
    public ACLStorageDataset(Map<String,Object> params ) throws ACLStorageException{
        this.params = params;
        
        if (params == null) {
            storageDataset = DatasetFactory.createTxnMem();
        } else {

            final String persName = (String) params.get(PARAM_DATASETPERSISTENCY );

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
        
        dsConnection = RDFConnectionFactory.connect(storageDataset);
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
    public void addUser(String user) throws ACLStorageException {
        final String insertQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "INSERT DATA { GRAPH " + SEPACL_GRAPH_NAME   +"  { " + System.lineSeparator()   +
                "    sepaACL:$$USERNAME " + System.lineSeparator()   +		 
                "        sepaACL:userName    \"$$USERNAME\" ; " + System.lineSeparator()   +
                "        sepaACL:accessInformation 	[]" + System.lineSeparator()   +
                "        }}";
        
        final String finalInsertQuery = insertQuery.replaceAll("\\$\\$USERNAME", user);
        
        LocalDatasetActions.insertData(storageDataset, finalInsertQuery);
    }
    @Override
    public void addUserPermission(String user, String graph,DatasetACL.aclId id) throws ACLStorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private Map<String,String> loadUserList() {
        final String selectQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "SELECT * WHERE { GRAPH " + SEPACL_GRAPH_NAME + " { ?user sepaACL:userName ?value }}";
        
        final Map<String,String> ret = new TreeMap<>();
        
        final  ResultSet rs = LocalDatasetActions.query(storageDataset, selectQuery, dsConnection);
        
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final String userUri = qs.get("user").toString();
            final String userName = qs.getLiteral("value").toString();
            
            ret.put(userUri, userName);
            
        }

        return ret;
    }

    private Map<String,String> loadGroupList() {
        final String selectQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "SELECT * WHERE { GRAPH " + SEPACL_GRAPH_GROUP_NAME + " { ?group sepaACL:groupName ?value }}";
        
        final Map<String,String> ret = new TreeMap<>();
        
        final  ResultSet rs = LocalDatasetActions.query(storageDataset, selectQuery, dsConnection);
        
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final String groupUri = qs.get("group").toString();
            final String groupName = qs.getLiteral("value").toString();
            
            ret.put(groupUri, groupName);
            
        }

        return ret;
    }
    
    private UserData loadUserData(String userUri, String userName) {
        final UserData ret = new UserData();
        final String selectQuery = 
            "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()   +
            "select * WHERE { GRAPH " + SEPACL_GRAPH_NAME +"  { " + System.lineSeparator()   +
            "  <$$USERNAME> sepaACL:accessInformation [sepaACL:graphName ?graphName] ." + System.lineSeparator()   +
            "  <$$USERNAME> sepaACL:accessInformation [sepaACL:allowedRight ?right] ." + System.lineSeparator()   +
            "  <$$USERNAME> sepaACL:userName ?user ." + System.lineSeparator()   +
            "  OPTIONAL {" + System.lineSeparator()   +
            "    <$$USERNAME> sepaACL:memberOf ?groupName" + System.lineSeparator()   +
            "  }" + System.lineSeparator()   +
            "  }}                " + System.lineSeparator()  ;
        
        final String finalSelectQuery = selectQuery.replaceAll("\\$\\$USERNAME", userUri);
        final  ResultSet rs = LocalDatasetActions.query(storageDataset, finalSelectQuery, dsConnection);
        
        
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final String  groupName = qs.get("groupName") != null ? qs.get("groupName").toString() : null;
            final String  graphName = qs.get("graphName").toString();
            final String  allowedRight = qs.get("right").toString();
            if (groupName != null)
                ret.memberOf.add(groupName);
            
            Set<DatasetACL.aclId> r = ret.graphACLs.get(graphName);
            if (r == null) {
                r = new TreeSet<>();
                ret.graphACLs.put(graphName, r);
                
            }
            final DatasetACL.aclId aclRight = decodeUriRight(allowedRight);
            r.add(aclRight);
        }
        
        
        return ret;
    }
    
    private Map<String,Set<DatasetACL.aclId>> loadGroupData(String groupUri, String groupName) {
        final Map<String,Set<DatasetACL.aclId>> ret  = new TreeMap<>();
        final String selectQuery = 
            "PREFIX sepaACL: "+ SEPACL_NS_PFIX                          + System.lineSeparator()   +
             "select * WHERE { GRAPH " + SEPACL_GRAPH_GROUP_NAME   +"  { "    + System.lineSeparator()   +
            "   <$$GROUPNAME> sepaACL:accessInformation ?bnode. "        + System.lineSeparator()   +
            "   ?bnode sepaACL:allowedRight		?right.  "        + System.lineSeparator()   +
            "   ?bnode sepaACL:graphName                ?graphName. "     + System.lineSeparator()   +
            "}}";                
                 
        
        final String finalSelectQuery = selectQuery.replaceAll("\\$\\$GROUPNAME", groupUri);
        final  ResultSet rs = LocalDatasetActions.query(storageDataset, finalSelectQuery, dsConnection);
        
        
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            final String  graphName = qs.get("graphName").toString();
            final String  allowedRight = qs.get("right").toString();
            
            Set<DatasetACL.aclId> r = ret.get(graphName);
            if (r == null) {
                r = new TreeSet<>();
                ret.put(graphName, r);
                
            }
            final DatasetACL.aclId aclRight = decodeUriRight(allowedRight);
            r.add(aclRight);
        }
        
        
        return ret;
    }
    
    @Override
    public Map<String,SEPAAcl.UserData>  loadUsers() {
        final  Map<String,SEPAAcl.UserData> ret = new TreeMap<>();
        
        /*
            Passes;
            1) read all users
        
        */
        final Map<String,String> users = loadUserList();
        for(final Map.Entry<String,String> user : users.entrySet()) {
            final String uri = user.getKey();
            final String name = user.getValue();
            final UserData ud = loadUserData(uri, name);
            ret.put(name, ud);
            
        }
        
        return ret;
    }
    
    @Override
    public Map<String,Map<String,Set<DatasetACL.aclId>>>  loadGroups() {
        final  Map<String,Map<String,Set<DatasetACL.aclId>>> ret = new TreeMap<>();
        
      /*
            Passes;
            1) read all users
        
        */
        final Map<String,String> groups = loadGroupList();
        for(final Map.Entry<String,String> group : groups.entrySet()) {
            final String uri = group.getKey();
            final String name = group.getValue();
            final Map<String,Set<DatasetACL.aclId>> gd = loadGroupData(uri, name);
            ret.put(name, gd);
            
        }
        
        return ret;

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


    @Override
    public void removeGroup(String group) throws ACLStorageException {
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                 + System.lineSeparator()   +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                       + System.lineSeparator()   +
                "DELETE WHERE { GRAPH sepaACL:aclGroups {"                          + System.lineSeparator()   +
                  "sepaACLGroups:$$GROUPNAME ?p ?o 		."                  + System.lineSeparator()   +
                  "sepaACLGroups:$$GROUPNAME sepaACL:accessInformation ?bnode."     + System.lineSeparator()   +
                  "?bnode ?a ?b "                                                   + System.lineSeparator()   +
                "}}";
        final String finalQuery = baseQuery.replaceAll("\\$\\$GROUPNAME", group);
        
        LocalDatasetActions.update(storageDataset, finalQuery, dsConnection);
    }

    @Override
    public void removeGroupPermissions(String group, String graph) throws ACLStorageException {
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                     + System.lineSeparator()            +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                           + System.lineSeparator()            +

                "DELETE WHERE"                                                          + System.lineSeparator()            +		 
                "{ "                                                                    + System.lineSeparator()            +		 
                  "GRAPH " + SEPACL_GRAPH_GROUP_NAME        +"  { "               + System.lineSeparator()    +
                        "sepaACLGroups:$$GROUPNAME sepaACL:accessInformation ?bnode."   + System.lineSeparator()            +		 
                        "?bnode sepaACL:graphName <$$GRAPHNAME>."                       + System.lineSeparator()            +		 
                        "?bnode ?p ?o."                                                 + System.lineSeparator()            +		 
                  "} "                                                                  + System.lineSeparator()            +		 
                "}                ";
        
        final String finalQuery = baseQuery.replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$GRAPHNAME", graph);
        
        
        LocalDatasetActions.update(storageDataset, finalQuery, dsConnection);
        
    }

    @Override
    public void removeGroupPermission(String group, String graph, DatasetACL.aclId id) throws ACLStorageException {
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                             + System.lineSeparator()            +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                                   + System.lineSeparator()            +

                "DELETE"                                                                        + System.lineSeparator()            +		 
                "{ "                                                                            + System.lineSeparator()            +		 
                  "GRAPH " + SEPACL_GRAPH_GROUP_NAME        +"  { "                             + System.lineSeparator()            +
                    "?bnode  sepaACL:allowedRight	sepaACL:$$RIGHTNAME."                           + System.lineSeparator()            +		 
                  "} "                                                                          + System.lineSeparator()            +		 
                "}"                                                                             + System.lineSeparator()            +		 
                "WHERE {"                                                                       + System.lineSeparator()            +		 
	                "GRAPH " + SEPACL_GRAPH_GROUP_NAME        +"  { "                       + System.lineSeparator()            +
		                "sepaACLGroups:$$GROUPNAME sepaACL:accessInformation ?bnode."   + System.lineSeparator()            +		 
                                 "?bnode sepaACL:graphName  	<$$GRAPHNAME>."                 + System.lineSeparator()            +		                 
	                "}"                                                                     + System.lineSeparator()            +		 
                "}                ";
        
        final String finalQuery = baseQuery.replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$GRAPHNAME", graph)
                                            .replaceAll("\\$\\$RIGHTNAME", encodeUriRight(id));
        
        
        LocalDatasetActions.update(storageDataset, finalQuery, dsConnection);
        
    }

    @Override
    public void addGroup(String group) throws ACLStorageException {
        final String insertQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                     + System.lineSeparator()            +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX           + System.lineSeparator()            +
                "INSERT DATA { GRAPH " + SEPACL_GRAPH_GROUP_NAME        +"  { " + System.lineSeparator()    +
                "    sepaACLGroups:$$GROUPNAME "                        + System.lineSeparator()            +		 
                "        sepaACL:groupName    \"$$GROUPNAME\" ; "      + System.lineSeparator()            +
//                "        sepaACL:accessInformation 	[]"             + System.lineSeparator()            +
                "        }}";
        
        final String finalInsertQuery = insertQuery.replaceAll("\\$\\$GROUPNAME", group);
        
        LocalDatasetActions.insertData(storageDataset, finalInsertQuery);

    }

    @Override
    public void addGroupPermission(String group, String graph, DatasetACL.aclId id) throws ACLStorageException {
/*        
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                             + System.lineSeparator()   +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                                   + System.lineSeparator()   +                
                "INSERT "                                                                       + System.lineSeparator()   +		 
                "{ "                                                                            + System.lineSeparator()   +		 
                "  GRAPH " + SEPACL_GRAPH_GROUP_NAME   +" { "                                   + System.lineSeparator()   +		 
                "    ?bnode sepaACL:graphName  	<$$GRAPHNAME>;"                                 + System.lineSeparator()   +		 
                "    sepaACL:allowedRight	sepaACL:$$RIGHTNAME."                           + System.lineSeparator()   +		 
                "  } "                                                                          + System.lineSeparator()   +		 
                "}"                                                                             + System.lineSeparator()   +		 
                "WHERE {"                                                                       + System.lineSeparator()   +		 
                "	GRAPH " + SEPACL_GRAPH_GROUP_NAME    +" {"                              + System.lineSeparator()   +		 
                "		sepaACLGroups:$$GROUPNAME sepaACL:accessInformation ?bnode."    + System.lineSeparator()   +		 
                "	}"                                                                      + System.lineSeparator()   +		 
                "}                "                                                             + System.lineSeparator()  ;
        
*/
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                             + System.lineSeparator()   +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                                   + System.lineSeparator()   +                
                "INSERT "                                                                       + System.lineSeparator()   +		 
                "{ "                                                                            + System.lineSeparator()   +		 
                "  GRAPH " + SEPACL_GRAPH_GROUP_NAME   +" { "                                   + System.lineSeparator()   +		 
                "    ?bnode sepaACL:allowedRight	sepaACL:$$RIGHTNAME."                   + System.lineSeparator()   +		 
                "  } "                                                                          + System.lineSeparator()   +		 
                "}"                                                                             + System.lineSeparator()   +		 
                "WHERE {"                                                                       + System.lineSeparator()   +		 
                "  GRAPH " + SEPACL_GRAPH_GROUP_NAME   +" { "                                   + System.lineSeparator()   +		 		 
                "		sepaACLGroups:$$GROUPNAME sepaACL:accessInformation ?bnode. "   + System.lineSeparator()   +		 
                "		?bnode sepaACL:graphName  <$$GRAPHNAME>;"                       + System.lineSeparator()   +		 
                "	}"                                                                      + System.lineSeparator()   +		 
                "}";
        
        final String finalQuery = baseQuery.replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$GRAPHNAME", graph)
                                            .replaceAll("\\$\\$RIGHTNAME", encodeUriRight(id));
        
        
        LocalDatasetActions.update(storageDataset, finalQuery, dsConnection);
    }

    @Override
    public void addUserToGroup(String user, String group) throws ACLStorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeUserFromGroup(String user, String group) throws ACLStorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addGraphToUser(String user, String graph,DatasetACL.aclId firstId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addGraphToGroup(String group, String graph,DatasetACL.aclId firstId) {
        final String baseQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                                         + System.lineSeparator()   +
                "PREFIX sepaACLGroups: " + SEPACL_NS_GRP_PFIX                               + System.lineSeparator()   +                
                "INSERT  DATA"                                                              + System.lineSeparator()  +
                "{ "                                                                        + System.lineSeparator()  +
                "  GRAPH " + SEPACL_GRAPH_GROUP_NAME   +" { "                               + System.lineSeparator()   +		 
                "        sepaACLGroups:$$GROUPNAME sepaACL:groupName \"$$GROUPNAME\" ; "    + System.lineSeparator()  +
                "        sepaACL:accessInformation 	[ "                                 + System.lineSeparator()   +		 
                "			sepaACL:graphName   	<$$GRAPHNAME>;"             + System.lineSeparator()   +		 
                "			sepaACL:allowedRight	sepaACL:$$RIGHTNAME"       + System.lineSeparator()   +		                 

                "       	]."                                                         + System.lineSeparator()   +		 
                "  } "                                                                      + System.lineSeparator()  +
                "}                ";
        
        final String finalQuery = baseQuery .replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$GRAPHNAME", graph)
                                            .replaceAll("\\$\\$RIGHTNAME", encodeUriRight(firstId));
        
        
        LocalDatasetActions.insertData(storageDataset, finalQuery);
        
                
    }
    

        
}

