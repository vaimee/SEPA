/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import com.google.gson.Gson;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.jena.acl.DatasetACL.aclId;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageJSon implements ACLStorage {
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
    public Map<String, Map<String, Set<aclId>>> load() throws ACLStorageException {
        
        
        try  {
             byte[] bytes = Files.readAllBytes(Paths.get(jsonFile));
            final String fileContent = new String (bytes);
            jsonArchive  = gson.fromJson(fileContent, JSonArchive.class);
            return jsonArchive.aclData;
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
    public static class JSonArchive {
        public  Map<String, Map<String, Set<aclId>>> aclData;
    };
}
