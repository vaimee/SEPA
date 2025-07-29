package com.vaimee.sepa.engine.dependability.authorization;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.logging.Logging;

public class SyncLdap implements IUsersSync {

	private final LdapNetworkConnection ldap;
	private final LdapProperties prop;
	
	private EntryCursor cursor = null;
	private final String endpointPassword;
	private final String endpointPasswordUid;
	private final String usersUid;
	
	public SyncLdap(LdapProperties prop) throws SEPASecurityException {
		this.prop = prop;	
		this.ldap = new LdapNetworkConnection(prop.getHost(), prop.getPort());
		
		endpointPasswordUid = "uid=endpointUsersPassword," +prop.getUsersDN()+","+prop.getBase();
		usersUid = "ou=users," + (prop.getUsersDN() == null ? "" : prop.getUsersDN()+",") + prop.getBase();
		
		Logging.log("ldap","endpointPasswordUid: "+endpointPasswordUid);
		Logging.log("ldap","usersUid: "+usersUid);
		
		endpointPassword = retrievePassword();
		
		new Thread() {
			public void run() {
				try {
					sync();
				} catch (SEPASecurityException e) {
					Logging.error(e.getMessage());
				}
			}
		}.start();
	}
	
	public String getEndpointUsersPassword() {
		return endpointPassword;
	}
	
	private String retrievePassword() throws SEPASecurityException {
		String ret = null;
		
		try {
			bind();
			
			Logging.log("ldap","[LDAP] Sync LDAP "+ldap.getConfig().getLdapHost()+":"+ldap.getConfig().getLdapPort()+" Base DN: " + endpointPasswordUid);

			EntryCursor cursor = ldap.search(endpointPasswordUid, "(objectclass=simpleSecurityObject)", SearchScope.OBJECT);
			
			if (cursor.next()) {
				// Password has to be store as "plain text"
				ret = new String(cursor.get().get("userPassword").get().getBytes());
				Logging.log("ldap","userPassword: "+ret);				
			}
			else throw new SEPASecurityException(endpointPasswordUid+" not found in LDAP");
		} catch (LdapException | CursorException  e) {
			throw new SEPASecurityException(e.getMessage());
		} finally {
			unbind();				
		}
		
		return ret;	
	}

	public JsonObject sync() throws SEPASecurityException {
		JsonObject ret = new JsonObject();
		
		try {
			bind();
			
			Logging.log("ldap","[LDAP] Sync LDAP "+ldap.getConfig().getLdapHost()+":"+ldap.getConfig().getLdapPort()+" Base DN: " + usersUid);

			EntryCursor cursor = ldap.search(usersUid, "(objectclass=inetOrgPerson)", SearchScope.ONELEVEL);
			
			for (org.apache.directory.api.ldap.model.entry.Entry entry: cursor) {
				Logging.log("ldap",entry.toString("--"));
				
				if (entry.get("uid") == null) {
					Logging.log("ldap","Missing *uid*");
					continue;
				}
				if (entry.get("description") == null) {
					Logging.log("ldap","Missing *description* "+entry.get("uid"));
					continue;
				}
				String uid = entry.get("uid").getString();
				String description = entry.get("description").getString();
			
				ret.add(uid,new Gson().fromJson(description,JsonObject.class)); 
			}
		} catch (LdapException | SEPASecurityException  e) {
			Logging.error("[LDAP] LdapException|CursorException : " + e.getMessage());
		} finally {
			unbind();
				
		}
		
		return ret;
	}

	private void bind() throws SEPASecurityException {
		try {
			if (prop.isTls()) ldap.startTls();
		} catch (LdapException e1) {
			Logging.error(e1.getMessage());
		}
		
		if (prop.getUser() != null)
			try {
				ldap.bind(prop.getUser(), prop.getPass());
			} catch (LdapException e) {
				Logging.error("[LDAP] Exception on binding: " + e.getMessage());
				throw new SEPASecurityException("Exception on LDAP binding: " + e.getMessage());
			}
		else
			try {
				ldap.bind();
			} catch (LdapException e) {
				Logging.error("[LDAP] Exception on binding: " + e.getMessage());
				throw new SEPASecurityException("Exception on LDAP binding: " + e.getMessage());
			}
	}

	private void unbind() throws SEPASecurityException {
		try {
			if (cursor != null)
				cursor.close();
			ldap.unBind();
		} catch (IOException e) {
			Logging.error("[LDAP] IOException: " + e.getMessage());
			throw new SEPASecurityException(e.getMessage());
		} catch (LdapException e) {
			Logging.error("[LDAP] LdapException: " + e.getMessage());
			throw new SEPASecurityException(e.getMessage());
		} finally {
			cursor = null;
		}
	}
}
