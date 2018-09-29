package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.Map.Entry;

import com.google.gson.JsonElement;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class ObservationRemover extends Producer {

	public ObservationRemover(String jsap) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAP(jsap), "REMOVE_OBSERVATION");
	}
	
	public void remove(String observation) throws SEPASecurityException, IOException, SEPAPropertiesException {
		this.setUpdateBindingValue("observation", new RDFTermURI(observation));
		update();
	}
	
	public void removeAll() throws SEPASecurityException, IOException, SEPAPropertiesException {
		for (Entry<String, JsonElement> entry : getApplicationProfile().getExtendedData().get("semantic-mappings").getAsJsonObject().entrySet()) {	
			remove(entry.getValue().getAsJsonObject().get("observation").getAsString());
		}
	}

}
