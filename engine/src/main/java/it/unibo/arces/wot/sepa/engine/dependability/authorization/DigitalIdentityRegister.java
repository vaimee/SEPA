package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.util.Scanner;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class DigitalIdentityRegister {
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.println("Digital Identity Register");
		
		System.out.println("Select the identity type:");
		System.out.println("1 - Application");
		System.out.println("2 - Device");
		System.out.print("Type: ");
		String type = in.nextLine();
		
		System.out.print("UID: ");
		String uid = in.nextLine();
		
		System.out.println("SPARQL endpoint credentials");
		System.out.print("User: ");
		String user = in.nextLine();
		System.out.print("Password: ");
		String password = in.nextLine();
		
		DigitalIdentity identity = null;
		switch(type) {
		case "1":
			identity = new ApplicationIdentity(uid,new Credentials(user,password));
			break;
		case "2":
			identity = new DeviceIdentity(uid,new Credentials(user,password));
			break;
		}
		
		LdapAuthorization ldap = new LdapAuthorization();
		try {
			ldap.addIdentity(identity);
		} catch (SEPASecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				System.out.print("Entity already exists! Do you want to replace it? (y/n): ");
				if (in.nextLine().toLowerCase().startsWith("n")) {
					System.out.println("Exit");
					in.close();
					return;
				}
				ldap.removeIdentity(identity.getUid());
				ldap.addIdentity(identity);
			} catch (SEPASecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println("Entity creation failed");
				in.close();
				return;
			}
		}
		
		in.close();
		System.out.println("Entity created");
	}
}
