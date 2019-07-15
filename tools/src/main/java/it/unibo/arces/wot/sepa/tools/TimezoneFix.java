package it.unibo.arces.wot.sepa.tools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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

	private static String[] observationStrings = { "http://wot.arces.unibo.it/monitor#5CCF7F151DC9-temperature" };
	private static int days = 3;
	
	// First date:
	// 2019-06-26T20:21:45.734363^^http://www.w3.org/2001/XMLSchema#dateTime
	private static int[] firstDate = {2019,7,9};
	
	public static String format(Calendar calendar) {
	    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	    fmt.setCalendar(calendar);
	    String dateFormatted = fmt.format(calendar.getTime());

	    return dateFormatted;
	}
	
	public static void main(String[] args) throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException,
			SEPABindingsException, IOException {
		JSAP jsap = new JSAP("base.jsap");

		GenericClient client = new GenericClient(jsap);

		Calendar from = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		Calendar to = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		from.set(firstDate[0], firstDate[1]-1, firstDate[2], 0, 0, 0);
		to.set(firstDate[0], firstDate[1]-1, firstDate[2], 23, 59, 59);

		
		while(days > 0) {
			String fromUTC = format(from);
			String toUTC = format(to);
			
			for (String observation : observationStrings) {

				Bindings fBindings = new Bindings();
				fBindings.addBinding("from", new RDFTermLiteral(fromUTC, "xsd:dateTime"));
				fBindings.addBinding("to", new RDFTermLiteral(toUTC, "xsd:dateTime"));
				fBindings.addBinding("observation", new RDFTermURI(observation));

				Response retResponse = client.query("LOG_QUANTITY", fBindings, 5000);

				if (retResponse.isError())
					logger.error("Failed to query");
				else {
					QueryResponse queryResponse = (QueryResponse) retResponse;
					for (Bindings bindings : queryResponse.getBindingsResults().getBindings()) {
						String timestampString = bindings.getValue("timestamp");
						if (timestampString.endsWith("Z"))
							continue;

						timestampString += "Z";
						String resultString = bindings.getValue("result");

						Bindings fix = new Bindings();
						fix.addBinding("result", new RDFTermURI(resultString));
						fix.addBinding("timestamp", new RDFTermLiteral(timestampString, "xsd:dateTime"));

						logger.info("Fixing timestamp: "+timestampString);
						
						retResponse = client.update("FIX_LOG", fix, 5000);
						if (retResponse.isError())
							logger.error("Failed to update");
					}
				}
			}
			
			days--;
			from.add(Calendar.DAY_OF_MONTH, 1);
			to.add(Calendar.DAY_OF_MONTH, 1);
			
		}
		
		client.close();
	}

}
