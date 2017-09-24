package engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.processing.UpdateProcessor;

public class UpdateProcessorTest {
	private static UpdateProcessor processor;
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, IllegalArgumentException, URISyntaxException{
		SPARQL11Properties properties = new SPARQL11Properties("endpoint.jpar");
		
		System.out.println(properties.toString());
		
		processor = new UpdateProcessor(properties);
		
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
