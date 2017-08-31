package it.unibo.arces.wot.sepa.apps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class LCDProducer extends Producer {

	public LCDProducer(ApplicationProfile appProfile, String updateID)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile, updateID);
	}

	private static void justText(Producer client) {
		Bindings bind = new Bindings();
		while (true) {
			bind = new Bindings();
			bind.addBinding("value", new RDFTermLiteral("Let Things Talk"));
			client.update(bind);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bind = new Bindings();
			bind.addBinding("value", new RDFTermLiteral("Vaimee!"));
			client.update(bind);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void superCar(Producer client) {
		Bindings bind = new Bindings();
		
		HashMap<Integer, String> graphics = new HashMap<Integer, String>();
		graphics.put(0, "*              *");
		graphics.put(1, "**            **");
		graphics.put(2, "***          ***");
		graphics.put(3, "****        ****");
		graphics.put(4, "*****      *****");
		graphics.put(5, "******    ******");
		graphics.put(6, "*******  *******");
		graphics.put(7, "****************");
		
		while (true) {
			for (int i = 0; i < 8; i++) {
				bind = new Bindings();
				bind.addBinding("value", new RDFTermLiteral(graphics.get(i)));
				client.update(bind);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for (int i = 7; i > 0; i--) {
				bind = new Bindings();
				bind.addBinding("value", new RDFTermLiteral(graphics.get(i)));
				client.update(bind);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
	}
	
	public static void main(String[] args)
			throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException,
			InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		ApplicationProfile appProfile = new ApplicationProfile("wot-plugfest.jsap");

		LCDProducer client = new LCDProducer(appProfile, "LCD");

		superCar(client);
		justText(client);
		
	}

}
