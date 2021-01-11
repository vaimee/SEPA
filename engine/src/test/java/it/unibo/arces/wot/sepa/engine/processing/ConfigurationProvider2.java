package it.unibo.arces.wot.sepa.engine.processing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.jwk.RSAKey;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class ConfigurationProvider2 {
	protected final Logger logger = LogManager.getLogger();
	public String a = "";
	
	public ConfigurationProvider2() throws SEPASecurityException {
		File jksFile = new File(getClass().getClassLoader().getResource("endpoint.jpar").getFile());
		//System.out.println("[VERBOSE] endpointJpar EXIST: "+ jksFile.exists());	
//		if( jksFile.exists()) {
//
//			Scanner myReader;
//			try {
//				myReader = new Scanner(jksFile); 
//				while (myReader.hasNextLine()) {
//			        String data = myReader.nextLine();
//					System.out.println("[VERBOSE] endpointJpar: "+ data);		
//		      }
//		      myReader.close();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				System.out.println("[VERBOSE] can't read endpointJpar ");	
//			}
//		    
//		}
		//a="endpoint.jpar";
		a=jksFile.getAbsolutePath();
	}
	
	
}
