package it.unibo.arces.wot.sepa.apps.randomnumbers;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class RandomNumberGenerator extends Producer {
	private static final Logger logger = LogManager.getLogger("RandomNumber");

	private final String baseURI = "rnd:RandomNumber-";
	Bindings forcedBindings = new Bindings();

	public RandomNumberGenerator() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException  {
		super(new ApplicationProfile("randomNumbers.jsap"), "RANDOM_NUMBER");
		
		new Thread() {
			public void run() {
				while(true) {
					try {
						Thread.sleep(getApplicationProfile().getExtendedData().get("sleep").getAsInt());
					} catch (InterruptedException e) {
						return;
					}
					
					String value = String.format("%.3f", 100 * Math.random()).replace(",", ".");
					String number = baseURI + UUID.randomUUID();
					forcedBindings.addBinding("number", new RDFTermURI(number));
					
					// Update!
					logger.info(baseURI+" generate random value: "+value);
					forcedBindings.addBinding("value", new RDFTermLiteral(value));
					update(forcedBindings);
				}
			}
		}.start();
	}
}
