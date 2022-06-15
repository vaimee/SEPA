/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import com.google.gson.Gson;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.EngineACLException;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.acl.DatasetACL.aclId;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageJSon implements ACLStorageOperations {
    public static final String PARAM_JSONFILE                       = "acl.json.fileName";

    
    
    private static final       Map<String,String>                   paramsInfo = new TreeMap<String,String>();
    
    
    static {
        paramsInfo.put(PARAM_JSONFILE, "Parh of json file where data is stored");
    }
    private final Map<String,Object>    params;
    private final String                jsonFile;
    private JSonArchive                 jsonArchive;
    private final Gson                  gson = new Gson();
    public ACLStorageJSon(Map<String,Object> params) throws ACLStorageException {
        this.params = params;
        final String jsonFile = (String) params.get(PARAM_JSONFILE );
        
        if (jsonFile == null || jsonFile.trim().length() == 0)
            throw new ACLStorageException("Missing or invalid param " + PARAM_JSONFILE , ACLStorageId.aiJSon, params);
        
        this.jsonFile = jsonFile.trim();
        
        
        
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
    public Map<String, SEPAAcl.UserData> loadUsers() throws ACLStorageException {
        
        
        try  {
             byte[] bytes = Files.readAllBytes(Paths.get(jsonFile));
            final String fileContent = new String (bytes);
            jsonArchive  = gson.fromJson(fileContent, JSonArchive.class);
            return jsonArchive.aclUserData;
        } catch(Exception e ) {
            throw new ACLStorageException("File not found " + jsonFile,ACLStorageId.aiJSon, params);
        }
        
        
    }

    @Override
    public void removeUser(String user) throws ACLStorageException {
        write();
    }

    @Override
    public void removeUserPermissions(String user, String graph) throws ACLStorageException {
        write();
    }

    @Override
    public void removeUserPermission(String user, String graph, aclId id) throws ACLStorageException {
        write();
    }

    @Override
    public void addUser(String user) throws ACLStorageException {
        write();
    }

    @Override
    public void addUserPermission(String user, String graph, aclId id) throws ACLStorageException {
        write();
    }

    private void write() throws ACLStorageException {
        final String content = gson.toJson(jsonArchive);
        try (
            final FileOutputStream fos = new FileOutputStream(jsonFile);
            final PrintWriter pw = new PrintWriter(fos);
        ){
            pw.write(content);
        }catch(Exception e ) {
            throw new ACLStorageException("Failed to save data to " + jsonFile,e);
        }
    }

    @Override
    public void addUserToGroup(String user, String group) throws ACLStorageException {
        write();
    }

    @Override
    public void removeUserFromGroup(String user, String group) throws ACLStorageException {
        write();
    }

    @Override
    public void addGraphToUser(String user, String graph,DatasetACL.aclId firstId) {
        write();
    }

    @Override
    public void addGraphToGroup(String group, String graph,DatasetACL.aclId firstId) {
        write();
    }

    @Override
    public SEPAAcl.UserData loadUser(String userName) throws EngineACLException, ACLStorageException {
        final Map<String, SEPAAcl.UserData> tmp = loadUsers();
        final SEPAAcl.UserData ud = tmp.get(userName);
        if (ud == null)
            throw new ACLStorageException("User not found : " + userName,ACLStorageId.aiJSon);
        
        return ud;
    }

    @Override
    public Map<String, Set<aclId>> loadGroup(String groupName) throws EngineACLException, ACLStorageException {
        final Map<String, Map<String, Set<aclId>>> tmp = loadGroups();
        final Map<String, Set<aclId>> gd  = tmp.get(groupName);
        if (gd == null)
            throw new ACLStorageException("Group not found : " + groupName,ACLStorageId.aiJSon);
        
        return gd;
    }

    @Override
    public void register(ACLStorageRegistrableParams params,ACLStorage owner) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response query(QueryRequest req, SEPAUserInfo usr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response update(UpdateRequest req, SEPAUserInfo usr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
 
    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerSecure(ACLStorageRegistrableParams params,ACLStorage owner) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public static class JSonArchive {
        public  Map<String, SEPAAcl.UserData>           aclUserData;
        public  Map<String, Map<String, Set<aclId>>>    aclGroupData;
    };
    
    @Override
    public Map<String, Map<String, Set<aclId>>> loadGroups() throws ACLStorageException {
        
        
        try  {
             byte[] bytes = Files.readAllBytes(Paths.get(jsonFile));
            final String fileContent = new String (bytes);
            jsonArchive  = gson.fromJson(fileContent, ACLStorageJSon.JSonArchive.class);
            return jsonArchive.aclGroupData;
        } catch(Exception e ) {
            throw new ACLStorageException("File not found " + jsonFile,ACLStorage.ACLStorageId.aiJSon, params);
        }
        
        
    }

    @Override
    public void removeGroup(String user) throws ACLStorageException {
        write();
    }

    @Override
    public void removeGroupPermissions(String user, String graph) throws ACLStorageException {
        write();
    }

    @Override
    public void removeGroupPermission(String user, String graph, aclId id) throws ACLStorageException {
        write();
    }

    @Override
    public void addGroup(String user) throws ACLStorageException {
        write();
    }

    @Override
    public void addGroupPermission(String user, String graph, aclId id) throws ACLStorageException {
        write();
    }
    
}



