package it.unibo.arces.wot.sepa.tools;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class TimezoneFix {
	private static final Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, SEPABindingsException, IOException {
		JSAP jsap = new JSAP("base.jsap");
		
		GenericClient client = new GenericClient(jsap);
		
		Bindings fBindings = new Bindings();
		fBindings.addBinding("from", new RDFTermLiteral("2019-07-10T00:00:00", "xsd:dateTime"));
		fBindings.addBinding("to", new RDFTermLiteral("2019-07-12T00:00:00", "xsd:dateTime"));
		fBindings.addBinding("observation", new RDFTermURI("http://wot.arces.unibo.it/monitor#5CCF7F1B599E-temperature"));
		
		Response retResponse = client.query("LOG_QUANTITY", fBindings, 5000);
		
		if (retResponse.isError()) logger.error("Failed to query");
		else {
			QueryResponse queryResponse = (QueryResponse) retResponse;
			for (Bindings bindings : queryResponse.getBindingsResults().getBindings()) {
				String timestampString = bindings.getValue("timestamp");
				if (timestampString.endsWith("Z")) continue;
				
				timestampString += "Z";
				String resultString = bindings.getValue("result");
				
				Bindings fix = new Bindings();
				fix.addBinding("result", new RDFTermURI(resultString));
				fix.addBinding("timestamp", new RDFTermLiteral(timestampString,"xsd:dateTime"));
				
				retResponse = client.update("FIX_LOG", fix, 5000);
				if (retResponse.isError()) logger.error("Failed to update");
			}
		}
		
		client.close();
	}
	
	

}
