package engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
//import java.util.Observable;
//import java.util.Observer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SchedulerTest {
	private static Scheduler scheduler;
	
	public class ResponseObserver implements ResponseHandler {

		@Override
		public void sendResponse(Response response) throws IOException {
			System.out.println(response.toString());
			
		}	
	}
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, IllegalArgumentException, URISyntaxException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
		EngineProperties properties = new EngineProperties("engine.jpar");
		
		System.out.println(properties.toString());
		
		scheduler = new Scheduler(properties);
		ResponseObserver observer = new SchedulerTest().new ResponseObserver();
//		scheduler.addObserver(observer);
		
		while(true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			UpdateRequest update = new UpdateRequest("PREFIX test:<http://sepa/test#> delete {?s ?p ?o} insert {test:s test:p \""+Math.random()+"\"} where {OPTIONAL{?s ?p ?o}}");
			scheduler.schedule(update,observer);
			
			QueryRequest query = new QueryRequest("select * where {?s ?p ?o}");
			scheduler.schedule(query,observer);
			
		}
		
	}


}
