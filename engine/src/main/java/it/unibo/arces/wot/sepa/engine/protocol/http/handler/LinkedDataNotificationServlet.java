package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class LinkedDataNotificationServlet implements HttpAsyncRequestHandler<HttpRequest>{
	
	private Scheduler scheduler;

	public LinkedDataNotificationServlet(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		
		String query = "";
		
		if(httpExchange.getRequest().getRequestLine().getMethod().toUpperCase().equals("GET")) {
			System.out.println("LINE: "  + httpExchange.getRequest().getRequestLine().toString());          
			String url = httpExchange.getRequest().getRequestLine().getUri();
			StringTokenizer token = new StringTokenizer(url, " ");
			String path = token.nextToken(); ///ldnServlet?subscribe=select * where { ?a ?b ?c }
			path = URLDecoder.decode(path, "UTF-8");
			int i = path.indexOf('?');
			
			query = i > 0 ? path.substring(i+1) : "" ;
			
			Map<String, String> parseQuery = LinkedDataNotificationServlet.getQueryMap(query);
			
			//se è una subscribe
			if(parseQuery.containsKey("subscribe")){
			query = parseQuery.get("subscribe"); //subscribe=select * where { ?a ?b ?c }
			
			SubscribeRequest subscribeRequest = new SubscribeRequest(query);
			scheduler.schedule(subscribeRequest, new ldnHandler(httpExchange));
			
			} else { //case is not subscribe
			
			String[] split = path.split("\\/");
			String spuid = split[2];	//regex	
			int numeroNotifica = split.length > 3 ? Integer.parseInt(split[3]) : -1;

			QueryRequest queryRequest = new QueryRequest(query);
			scheduler.schedule(queryRequest, new ReadLdnHandler(httpExchange, spuid, numeroNotifica));
			
			}
					
		} else {
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Wrong format: " + httpExchange.getRequest().getRequestLine());
			return;
		}
		
	}
		
	public static Map<String, String> getQueryMap(String query)  
	{  
		Map<String, String> map = new HashMap<String, String>();
		if(!query.isEmpty()){
		    String[] params = query.split("&");  
		    for (String param : params)  
		    {  
		        String name = param.split("=")[0]; 
		        System.out.println("Key: " + name);
		        String value = param.split("=")[1]; 
		        System.out.println("Value: " + value);
		        map.put(name, value);  
		    }  
		}
		return map;
	}

}
