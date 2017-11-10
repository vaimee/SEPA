package engine;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;

public class QueryProcessorTest {
	private static QueryProcessor processor;

	public static void main(String[] args) throws SEPAProtocolException, SEPAPropertiesException{
		SPARQL11Properties properties = new SPARQL11Properties("endpoint.jpar");
		
		System.out.println(properties.toString());
		
		processor = new QueryProcessor(properties,null);
		
		while(true) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(processor.process(new QueryRequest("select ?s ?p ?o where {?s ?p ?o}"),0));	
		}
		
	}
}
