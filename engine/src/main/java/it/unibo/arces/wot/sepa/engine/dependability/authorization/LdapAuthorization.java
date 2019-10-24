/* LDAP based authorization
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class LdapAuthorization implements IAuthorization {
	protected static Logger logger = LogManager.getLogger();

	private final LdapConnection ldap;
	private final String ldapRoot;
	private final String pwd;
	private final String user;

	public LdapAuthorization(String host, int port, String base, String user, String pwd) throws LdapException {
		if (user != null && pwd == null)
			throw new LdapException("Password is null for user: " + user);
		ldap = new LdapNetworkConnection(host, port);
		this.user = user;
		this.pwd = pwd;
		ldapRoot = base;
	}

	private void bind() throws SEPASecurityException {
		if (user != null)
			try {
				ldap.bind(user, pwd);
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		else
			try {
				ldap.bind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
	}

	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {

			bind();

			cursor = ldap.search("uid=" + uid + ",ou=credentials," + ldapRoot, "(objectclass=*)",
					SearchScope.OBJECT, "*");

			if (cursor.next()) {
				if(cursor.get().get("javaSerializedData") != null) {
					return Credentials.deserialize(cursor.get().get("javaSerializedData").getBytes());
				}				
			}
		}
		catch(LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		}finally {
			if (cursor != null)
				try {
					cursor.close();
				} catch (IOException e) {
					throw new SEPASecurityException(e);
				}
		}
		
		return null;
	}
	
	@Override
	public void addIdentity(DigitalIdentity identity) throws SEPASecurityException {
		try {
			Entry entry = new DefaultEntry("uid=" + identity.getUid() + ",ou=authorizedIdentities," + ldapRoot);
			entry.add("ObjectClass", "uidObject");
			entry.add("ObjectClass", "top");
			entry.add("ObjectClass", "javaSerializedObject");
			entry.add("ObjectClass", identity.getObjectClass());

			entry.add("cn", "Authorized Digital Identity");
			entry.add("uid", identity.getUid());

			entry.add("javaClassName", identity.getEndpointCredentials().getClass().getName());
			entry.add("javaSerializedData", identity.getEndpointCredentials().serialize());

			bind();
			ldap.add(entry);
			ldap.unBind();
		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		}
	}

	@Override
	public void removeIdentity(String id) throws SEPASecurityException {
		bind();

		EntryCursor cursor = null;
		try {
			ldap.delete("uid=" + id + ",ou=authorizedIdentities," + ldapRoot);
		} catch (LdapException e) {

		} finally {
			if (cursor != null)
				try {
					cursor.close();
				} catch (IOException e) {

				}
			try {
				ldap.unBind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public void storeCredentials(DigitalIdentity identity, String client_secret) throws SEPASecurityException {
		byte[] password = PasswordUtil.createStoragePassword(client_secret.getBytes(),
				LdapSecurityConstants.HASH_METHOD_SSHA);

		EntryCursor cursor = null;
		try {
			bind();

			cursor = ldap.search("uid=" + identity.getUid() + ",ou=credentials," + ldapRoot, "(objectclass=*)",
					SearchScope.OBJECT, "*");

			if (!cursor.next()) {
				Entry entry = new DefaultEntry("uid=" + identity.getUid() + ",ou=credentials," + ldapRoot);
				entry.add("ObjectClass", "top");
				entry.add("ObjectClass", identity.getObjectClass());
				entry.add("ObjectClass", "uidObject");
				entry.add("ObjectClass", "simpleSecurityObject");
				entry.add("ObjectClass", "javaSerializedObject");
				
				entry.add("cn", "Authorized Digital Identity " + identity.getUid());
				entry.add("uid", identity.getUid());
				entry.add("userPassword", password);

				entry.add("javaClassName", identity.getEndpointCredentials().getClass().getName());
				entry.add("javaSerializedData", identity.getEndpointCredentials().serialize());
				
				ldap.add(entry);
			} else {
				Modification replace = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE,
						"userPassword", password);
				
				try {
					ldap.modify("uid=" + identity.getUid() + ",ou=credentials," + ldapRoot,replace);
				} catch (LdapException e1) {
					throw new SEPASecurityException(e1);
				}
			}
			ldap.unBind();
		} catch (LdapException | CursorException e) {
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
	public boolean checkCredentials(String uid, String secret) throws SEPASecurityException {
		EntryCursor cursor = null;

		try {
			bind();
			cursor = ldap.search("uid=" + uid + ",ou=credentials," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
					"*");
			if (cursor.next()) {
				return PasswordUtil.compareCredentials(secret.getBytes(),
						cursor.get().get("userPassword").get().getBytes());
			} else
				return false;
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					ldap.unBind();
					cursor.close();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void removeCredentials(DigitalIdentity identity) throws SEPASecurityException {
		try {
			bind();
			ldap.delete("uid=" + identity.getUid() + ",ou=credentials," + ldapRoot);
			ldap.unBind();
		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		}
	}

	@Override
	public boolean containsCredentials(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=" + uid + ",ou=credentials," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
					"*");
			return cursor.next();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				ldap.unBind();
				if (cursor != null)
					cursor.close();
			} catch (IOException | LdapException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException {
		EntryCursor cursor = null;

		bind();

		try {
			if (id == null) {
				cursor = ldap.search("uid=default,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
						SearchScope.OBJECT, "*");
				if (!cursor.next())
					throw new SEPASecurityException("uid=default,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");

			} else {
				if (id.equals("SEPATest")) {
					cursor = ldap.search("uid=" + id + ",uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
							"*");	
				}
				else
					cursor = ldap.search("uid=" + id + ",ou=credentials," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
						"*");

				if (!cursor.next())
					throw new LdapException("uid=" + id + ",ou=credentials," + ldapRoot + " NOT FOUND");

				Entry entry = cursor.get();

				if (!entry.containsAttribute("pwdGraceExpire")) {
					cursor.close();

					if (entry.hasObjectClass("applicationProcess")) {
						cursor = ldap.search("uid=application,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=application,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
					} else if (entry.hasObjectClass("device")) {
						cursor = ldap.search("uid=device,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=device,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
					} else if (entry.hasObjectClass("account")) {
						cursor = ldap.search("uid=account,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
								SearchScope.OBJECT, "*");

						if (!cursor.next())
							throw new LdapException("uid=account,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
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
					ldap.unBind();
					cursor.close();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}

	}

	@Override
	public void setTokenExpiringPeriod(String uid, long period) throws SEPASecurityException {
		try {
			bind();
			Modification pwdGraceExpire = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE,
					"pwdGraceExpire", String.format("%d", period));
			Modification pwdPolicy = new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, "objectClass",
					"pwdPolicy");
			Modification pwdAttribute = new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, "pwdAttribute",
					"userPassword");
			ldap.modify("uid=" + uid + ",ou=credentials," + ldapRoot, pwdGraceExpire, pwdPolicy, pwdAttribute);
			ldap.unBind();
		} catch (LdapException e2) {
			throw new SEPASecurityException(e2);
		}
	}

	@Override
	public String getIssuer() throws SEPASecurityException {
		bind();
		EntryCursor cursor = null;
		try {
			cursor = ldap.search("uid=issuer,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=issuer,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("host") == null)
				throw new SEPASecurityException("issuer host not found");

			return cursor.get().get("host").getString();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				if (cursor != null)
					cursor.close();
				ldap.unBind();
			} catch (LdapException | IOException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public void setIssuer(String issuer) throws SEPASecurityException {
		Modification replaceGn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "host", issuer);
		try {
			bind();
			ldap.modify("uid=issuer,ou=jwt," + ldapRoot, replaceGn);
			ldap.unBind();
		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		}
	}

	@Override
	public String getHttpsAudience() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=httpsAudience,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=httpsAudience,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("host") == null)
				throw new SEPASecurityException("httpsAudience host not found");

			return cursor.get().get("host").getString();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}

		}
	}

	@Override
	public void setHttpsAudience(String audience) throws SEPASecurityException {
		Modification replaceGn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "host", audience);
		try {
			bind();
			ldap.modify("uid=httpsAudience,ou=jwt," + ldapRoot, replaceGn);
			ldap.unBind();
		} catch (LdapException e1) {
			throw new SEPASecurityException(e1);
		}
	}

	@Override
	public String getWssAudience() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			cursor = ldap.search("uid=wssAudience,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=wssAudience,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("host") == null)
				throw new SEPASecurityException("uid=wssAudience,ou=jwt," + ldapRoot + " host NOT FOUND");

			return cursor.get().get("host").getString();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setWssAudience(String audience) throws SEPASecurityException {
		Modification replaceGn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "host", audience);
		try {
			bind();
			ldap.modify("uid=wssAudience,ou=jwt," + ldapRoot, replaceGn);
			ldap.unBind();
		} catch (LdapException e1) {
			throw new SEPASecurityException(e1);
		}
	}

	@Override
	public String getSubject() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=subject,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=subject,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("host") == null)
				throw new SEPASecurityException("uid=subject,ou=jwt," + ldapRoot + " host NOT FOUND");

			return cursor.get().get("host").getString();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setSubject(String subject) throws SEPASecurityException {
		Modification replaceGn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "host", subject);
		try {
			bind();
			ldap.modify("uid=subject,ou=jwt," + ldapRoot, replaceGn);
			ldap.unBind();
		} catch (LdapException e1) {
			throw new SEPASecurityException(e1);
		}
	}

	@Override
	public boolean isAuthorized(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			cursor = ldap.search("uid=" + uid + ",ou=authorizedIdentities," + ldapRoot, "(objectclass=*)",
					SearchScope.OBJECT, "*");
			return cursor.next();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void addToken(String uid, SignedJWT token) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=" + uid + ",ou=tokens," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next()) {
				ldap.add(new DefaultEntry("uid=" + uid + ",ou=tokens," + ldapRoot, "ObjectClass: top",
						"ObjectClass: account", "ObjectClass: javaSerializedObject", "javaClassName: "+ token.getClass().getName(),
						"javaSerializedData: " + token.serialize()));
			} else {
				Modification replaceGn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE,
						"javaSerializedData", token.serialize());
				try {
					ldap.modify("uid=" + uid + ",ou=tokens," + ldapRoot, replaceGn);
				} catch (LdapException e1) {
					throw new SEPASecurityException(e1);
				}
			}
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public boolean containsToken(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			cursor = ldap.search("uid=" + uid + ",ou=tokens," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			return cursor.next();
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public Date getTokenExpiringDate(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=" + uid + ",ou=tokens," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=" + uid + ",ou=tokens," + ldapRoot + " NOT FOUND");
			SignedJWT jwt = SignedJWT.parse(cursor.get().get("javaSerializedData").getString());
			return jwt.getJWTClaimsSet().getExpirationTime();
		} catch (LdapException | CursorException | ParseException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public SignedJWT getToken(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			cursor = ldap.search("uid=" + uid + ",ou=tokens," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=" + uid + ",ou=tokens," + ldapRoot + " NOT FOUND");
			return SignedJWT.parse(cursor.get().get("javaSerializedData").getString());
		} catch (LdapException | CursorException | ParseException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public DigitalIdentity getIdentity(String uid) throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=" + uid + ",ou=authorizedIdentities," + ldapRoot, "(objectclass=*)",
					SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=" + uid + ",ou=authorizedIndentities," + ldapRoot + " NOT FOUND");

			Credentials credentials = null;
			if (cursor.get().contains("objectClass", "javaSerializedData")) {
				credentials = Credentials.deserialize(cursor.get().get("javaSerializedData").getBytes());
			}
						
			if (cursor.get().contains("objectClass", "device"))
				return new DeviceIdentity(uid,credentials);
			else if (cursor.get().contains("objectClass", "applicationProcess"))
				return new ApplicationIdentity(uid,credentials);
			else
				throw new SEPASecurityException("Digital identity class NOT FOUND");
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setDeviceExpiringPeriod(long period) throws SEPASecurityException {
		try {
			Modification expiring = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "pwdGraceExpire");

			bind();

			ldap.modify("uid=device,uid=expiring,ou=jwt," + ldapRoot, expiring);

		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				ldap.unBind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public long getDeviceExpiringPeriod() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=device,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
					"*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=device,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("pwdGraceExpire") == null)
				throw new SEPASecurityException(
						"uid=device,uid=expiring,ou=jwt," + ldapRoot + " pwdGraceExpire NOT FOUND");

			return Long.parseLong(cursor.get().get("pwdGraceExpire").getString());
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setApplicationExpiringPeriod(long period) throws SEPASecurityException {
		try {
			Modification expiring = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "pwdGraceExpire");

			bind();

			ldap.modify("uid=application,uid=expiring,ou=jwt," + ldapRoot, expiring);

		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				ldap.unBind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public long getApplicationExpiringPeriod() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=application,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)",
					SearchScope.OBJECT, "*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=application,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("pwdGraceExpire") == null)
				throw new SEPASecurityException(
						"uid=application,uid=expiring,ou=jwt," + ldapRoot + " pwdGraceExpire NOT FOUND");

			return Long.parseLong(cursor.get().get("pwdGraceExpire").getString());
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setUserExpiringPeriod(long period) throws SEPASecurityException {
		try {
			Modification expiring = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "pwdGraceExpire");

			bind();

			ldap.modify("uid=user,uid=expiring,ou=jwt," + ldapRoot, expiring);

		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				ldap.unBind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		}

	}

	@Override
	public long getUserExpiringPeriod() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=user,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
					"*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=user,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("pwdGraceExpire") == null)
				throw new SEPASecurityException(
						"uid=user,uid=expiring,ou=jwt," + ldapRoot + " pwdGraceExpire NOT FOUND");

			return Long.parseLong(cursor.get().get("pwdGraceExpire").getString());
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}

	@Override
	public void setDefaultExpiringPeriod(long period) throws SEPASecurityException {
		try {
			Modification expiring = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "pwdGraceExpire");

			bind();

			ldap.modify("uid=default,uid=expiring,ou=jwt," + ldapRoot, expiring);

		} catch (LdapException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				ldap.unBind();
			} catch (LdapException e) {
				throw new SEPASecurityException(e);
			}
		}
	}

	@Override
	public long getDefaultExpiringPeriod() throws SEPASecurityException {
		EntryCursor cursor = null;
		try {
			bind();
			cursor = ldap.search("uid=default,uid=expiring,ou=jwt," + ldapRoot, "(objectclass=*)", SearchScope.OBJECT,
					"*");
			if (!cursor.next())
				throw new SEPASecurityException("uid=default,uid=expiring,ou=jwt," + ldapRoot + " NOT FOUND");
			if (cursor.get().get("pwdGraceExpire") == null)
				throw new SEPASecurityException(
						"uid=default,uid=expiring,ou=jwt," + ldapRoot + " pwdGraceExpire NOT FOUND");

			Attribute attr = cursor.get().get("pwdGraceExpire");
			return Long.parseLong(attr.getString());
		} catch (LdapException | CursorException e) {
			throw new SEPASecurityException(e);
		} finally {
			if (cursor != null)
				try {
					cursor.close();
					ldap.unBind();
				} catch (IOException | LdapException e) {
					throw new SEPASecurityException(e);
				}
		}
	}



}
