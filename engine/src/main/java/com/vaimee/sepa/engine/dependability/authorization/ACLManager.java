package com.vaimee.sepa.engine.dependability.authorization;

import java.io.Console;
import java.util.Scanner;

import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.security.Credentials;
import com.vaimee.sepa.engine.dependability.authorization.identities.ApplicationIdentity;
import com.vaimee.sepa.engine.dependability.authorization.identities.DeviceIdentity;
import com.vaimee.sepa.engine.dependability.authorization.identities.DigitalIdentity;
import com.vaimee.sepa.engine.dependability.authorization.identities.UserIdentity;

public class ACLManager {
	static String host = "localhost";
	static int port = 10389;
	static String base = "dc=sepatest,dc=com";
	static String user = "uid=admin,ou=system";
	static String pwd = "secret";

	public static void main(String[] args) {
		Console console = System.console();
		Scanner in = new Scanner(System.in);

		System.out.println("********************");
		System.out.println("* SEPA ACL Manager *");
		System.out.println("********************");

		LdapSecurityManager ldap;
		String line;
		while (true) {
			System.out.print("Host (return for default: localhost): ");
			line = in.nextLine();
			if (!line.equals(""))
				host = line;

			System.out.print("Port (return for default: 10389): ");
			line = in.nextLine();
			if (!line.equals(""))
				port = Integer.parseInt(line);

			System.out.print("Base (return for default: dc=sepatest,dc=com): ");
			line = in.nextLine();
			if (!line.equals(""))
				base = line;

			System.out.print("User (return for default: uid=admin,ou=system): ");
			line = in.nextLine();
			if (!line.equals(""))
				user = line;

			if (console != null)
				pwd = new String(console.readPassword("Password (default: secret):"));
			else {
				System.out.print("Password (default: secret):");
				line = in.nextLine();
				if (!line.equals(""))
					pwd = line;
			}

			try {
				ldap = new LdapSecurityManager(JKSUtil.getSSLContext("sepa.jks", "sepa2020"),JKSUtil.getRSAKey("sepa.jks", "sepa2020","jwt","sepa2020"),new LdapProperties(host, port, base, null,user, pwd, false));
			} catch (SEPASecurityException e2) {
				System.out.println(e2.getMessage());
				continue;
			}
			break;
		}
		System.out.println("Connected to LDAP!");

		System.out.println("Set SPARQL endpoint credentials");
		System.out.print("User (return for default: SEPATest):");
		line = in.nextLine();
		String user = "SEPATest";
		if (!line.equals("")) user = line;

		if (console != null)
			pwd = new String(console.readPassword("Password (default: SEPATest):"));
		else {
			System.out.print("Password (default: SEPATest):");
			line = in.nextLine();
			pwd = line;
		}

		while (true) {
			System.out.println("Available actions: ");
			System.out.println("1 - Register application");
			System.out.println("2 - Register device");
			System.out.println("3 - Register user");
			System.out.println("4 - Change SPARQL endpoint credentials");
			System.out.println("5 - Show SPARQL endpoint credentials");
			System.out.println("6 - Exit");

			System.out.print("Select: ");
			String action = in.nextLine();

			if (action.equals("6"))
				break;

			DigitalIdentity identity = null;
			String client_secret = null;

			switch (action) {
			case "1":
				System.out.print("UID: ");
				String uid = in.nextLine();
				identity = new ApplicationIdentity(uid, new Credentials(user, pwd));
				break;
			case "2":
				System.out.print("UID: ");
				uid = in.nextLine();
				identity = new DeviceIdentity(uid, new Credentials(user, pwd));
				break;
			case "3":
				System.out.print("Name: ");
				String cn = in.nextLine();
				System.out.print("Surname: ");
				String sn = in.nextLine();
				System.out.print("email: ");
				uid = in.nextLine();
				identity = new UserIdentity(uid, cn, sn, new Credentials(user, pwd));

				if (console != null)
					client_secret = new String(console.readPassword("Password: "));
				else {
					System.out.print("Password: ");
					line = in.nextLine();
					client_secret = line;
				}
				break;
			case "4":
				System.out.println("Change SPARQL endpoint credentials");
				System.out.print("User: ");
				user = in.nextLine();
				System.out.print("Password: ");
				pwd = in.nextLine();
				continue;
			case "5":
				System.out.println("SPARQL endpoint credentials");
				System.out.println("---------------------------");
				System.out.println("User: <" + user + ">");
				System.out.println("Password: <" + pwd + ">");
				System.out.println("---------------------------");
				continue;
			default:
				System.out.println("Wrong selection: " + action);
				continue;
			}

			try {
				if (action.equals("3")) {
					if (!ldap.storeCredentials(identity, client_secret)) {
						System.out.print("Entity already exists! Do you want to replace it? (y/n): ");
						if (in.nextLine().toLowerCase().startsWith("n"))
							continue;

						ldap.removeCredentials(identity);
						ldap.storeCredentials(identity, client_secret);
					}
				} else
					ldap.addAuthorizedIdentity(identity);
			} catch (SEPASecurityException e) {
				try {
					if (!action.equals("4")) {
						System.out.print("Entity already exists! Do you want to replace it? (y/n): ");
						if (in.nextLine().toLowerCase().startsWith("n"))
							continue;

						ldap.removeAuthorizedIdentity(identity.getUid());
						ldap.addAuthorizedIdentity(identity);
					} else {
						System.out.println("Failed to create entity: " + identity);
						continue;
					}
				} catch (SEPASecurityException e1) {
					System.out.println("Entity creation failed");
					continue;
				}
			}

			System.out.println("Entity created!");
		}

		in.close();
	}
}
