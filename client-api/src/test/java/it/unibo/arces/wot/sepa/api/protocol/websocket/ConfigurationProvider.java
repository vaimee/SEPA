package it.unibo.arces.wot.sepa.api.protocol.websocket;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import java.net.URL;

public class ConfigurationProvider {

    public static JSAP GetTestEnvConfiguration() throws SEPAPropertiesException, SEPASecurityException {
        String jsapFileName = null;
        
        if( System.getProperty("testConfiguration") != null){
        		jsapFileName = System.getProperty("testConfiguration");
        }
        else if (System.getenv("testConfiguration") != null) {
        		jsapFileName = System.getenv("testConfiguration");
        }else if (System.getProperty("secure") != null){
        		jsapFileName = System.getenv("sepatest-secure.jsap");
        }else{
        		jsapFileName = System.getenv("sepatest.jsap");
        }
        
        URL config = Thread.currentThread().getContextClassLoader().getResource(jsapFileName);
        
        return new JSAP(config.getPath());
    }
}
