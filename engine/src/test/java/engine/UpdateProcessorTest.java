package engine;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.processing.UpdateProcessor;

public class UpdateProcessorTest {
	private static UpdateProcessor processor;
	
	public static void main(String[] args) throws SEPAPropertiesException, SEPAProtocolException {
		SPARQL11Properties properties = new SPARQL11Properties("endpoint.jpar");
		
		System.out.println(properties.toString());
		
		processor = new UpdateProcessor(properties,null);
		
		while(true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(processor.process(new UpdateRequest("PREFIX test:<http://sepa/test#> delete {?s ?p ?o} insert {test:s test:p \""+Math.random()+"\"} where {?s ?p ?o}"),0));
		}
		
	}
}
