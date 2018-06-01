package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

import java.net.URL;

public class ConfigurationProvider {

    public static ApplicationProfile GetTestEnvConfiguration() throws SEPAPropertiesException {
        ApplicationProfile result;
        final String configuaration = System.getProperty("testConfiguration");
        if( configuaration != null){
            result = new ApplicationProfile(configuaration);
        }else{
            URL config = Thread.currentThread().getContextClassLoader().getResource("dev.jsap");
            result = new ApplicationProfile(config.getPath());
        }
        return result;
    }
}
