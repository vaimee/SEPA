package engine;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;

import java.io.File;
import java.net.URL;

public class ConfigurationProvider {

    public static SPARQL11SEProperties GetTestEnvConfiguration() throws SEPAPropertiesException {
        SPARQL11SEProperties result;
        final String configuaration = System.getProperty("testConfiguration");
        if( configuaration != null){
            final File confFile = new File(configuaration);
            result = new SPARQL11SEProperties(confFile);
        }else{
            URL config = Thread.currentThread().getContextClassLoader().getResource("dev.jsap");
            result = new SPARQL11SEProperties(new File(config.getPath()));
        }
        return result;
    }
}
