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
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;

public class QueryProcessorTest {
	private static QueryProcessor processor;
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, IllegalArgumentException, URISyntaxException{
		SPARQL11Properties properties = new SPARQL11Properties("endpoint.jpar");
		SPARQL11Protocol protocol = new SPARQL11Protocol(properties);
		
		System.out.println(properties.toString());
		
		processor = new QueryProcessor(protocol);
		
		while(true) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			QueryRequest req = new QueryRequest("select ?s ?p ?o where {?s ?p ?o}");
			Response ret = processor.process(req);
			System.out.println(ret.toString());
		}
		
	}
}
