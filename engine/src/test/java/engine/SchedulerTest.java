package engine;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SchedulerTest {
	private static Scheduler scheduler;
	
	public class Handler implements ResponseHandler {

		@Override
		public void sendResponse(Response response) throws IOException {
			System.out.println(response);
			
		}
	}
	
	public static void main(String[] args) throws SEPAPropertiesException{
		EngineProperties properties = new EngineProperties("engine.jpar");
		
		System.out.println(properties.toString());
		
		scheduler = new Scheduler(properties, null);
		Handler handler = new SchedulerTest().new Handler();
		
		while(true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			UpdateRequest update = new UpdateRequest("PREFIX test:<http://sepa/test#> delete {?s ?p ?o} insert {test:s test:p \""+Math.random()+"\"} where {OPTIONAL{?s ?p ?o}}");
			scheduler.schedule(update,handler);
			
			QueryRequest query = new QueryRequest("select * where {?s ?p ?o}");
			scheduler.schedule(query,handler);
			
		}
		
	}


}
