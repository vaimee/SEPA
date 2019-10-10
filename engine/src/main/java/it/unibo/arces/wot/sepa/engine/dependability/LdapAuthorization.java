package it.unibo.arces.wot.sepa.engine.dependability;

import java.io.IOException;
//import java.util.List;
import java.util.Date;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class LdapAuthorization implements IClientAuthorization {
	protected static Logger logger = LogManager.getLogger();

	private LdapConnection ldap = null;
	private String ldapRoot = null;

	public LdapAuthorization(String host, int port, String base, String user, String pwd) throws LdapException {
		ldap = new LdapNetworkConnection(host, port);
		ldap.bind(user, pwd);
		ldapRoot = base;
	}

	public LdapAuthorization(String host, int port, String base) throws LdapException {
		ldap = new LdapNetworkConnection(host, port);
		ldap.bind();
		ldapRoot = base;
	}

	@Override
	public void addAuthorizedIdentity(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAuthorizedIdentity(String id) {
		// TODO Auto-generated method stub

	}

//	@Override
//	public List<String> getAuthorizedIdentities() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public void storeCredentials(String client_id, String client_secret) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isClientRegistered(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean passwordCheck(String id, String secret) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException {
		EntryCursor cursor = null;

		try {
			if (id == null) {
				cursor = ldap.search("uid=default,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
						SearchScope.OBJECT, "*");
				if (!cursor.next())
					throw new SEPASecurityException("uid=" + id + ",ou=credentials NOT FOUND for id:" + id);

			} else {
				cursor = ldap.search("uid=" + id + ",ou=credentials," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
						"*");

				if (!cursor.next())
					throw new LdapException("uid=" + id + ",ou=credentials NOT FOUND for id:" + id);

				Entry entry = cursor.get();

				if (!entry.containsAttribute("pwdGraceExpire")) {
					cursor.close();

					if (entry.hasObjectClass("applicationProcess")) {
						cursor = ldap.search("uid=application,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=application,uid=expiring,ou=jwt NOT FOUND for id:" + id);
					} else if (entry.hasObjectClass("device")) {
						cursor = ldap.search("uid=device,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=device,uid=expiring,ou=jwt NOT FOUND for id:" + id);
					} else if (entry.hasObjectClass("account")) {
						cursor = ldap.search("uid=account,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=account,uid=expiring,ou=jwt NOT FOUND for id:" + id);
					} else
						throw new LdapException("ClassObject for " + id
								+ " MUST BE one of the following: device, applicationProcess, account");
				}
			}

			Entry entry = cursor.get();
			Attribute attr = entry.get("pwdGraceExpire");
			return Long.parseLong(attr.get().getString());
		} catch (LdapException | CursorException | IOException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
				} catch (IOException e) {
					throw new SEPASecurityException(e);
				}
		}

	}

	@Override
	public void setTokenExpiringPeriod(String id, long period) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getIssuer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIssuer(String is) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHttpsAudience() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHttpsAudience(String audience) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getWssAudience() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWssAudience(String audience) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSubject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSubject(String sub) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isAuthorized(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addToken(String id, SignedJWT token) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsToken(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Date getExpiringTime(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignedJWT getToken(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
