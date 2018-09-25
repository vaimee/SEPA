package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class ReadLdnHandler implements EventHandler {
	
	private HttpAsyncExchange exchange;
	private String spuid;
	private int numeroNotifica;
	
	private static String FILENAME = "/Users/AlessioPazzani/Documents/Tesi/FileSpuid/";
	
	public ReadLdnHandler(HttpAsyncExchange httpExchange, String spuid, int numeroNotifica) throws IllegalArgumentException {
		this.exchange = httpExchange;
		this.spuid = spuid;
		this.numeroNotifica = numeroNotifica;
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		
		if(numeroNotifica <0){ //localhost:8000/ldnServlet/spuid
			
		System.out.println("E' stato richiesto di visionare un particolare file");
		
		String fileName = FILENAME  + spuid;
		Type listType = new TypeToken<List<Object>>(){}.getType();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonReader jsonReader = new JsonReader(new FileReader(fileName));
		List<Object> jsonList = gson.fromJson(jsonReader, listType);

		System.out.println(gson.toJson(jsonList)); //stampo l'intero file letto
		
		ResponseListingOfNotification listOfNotification = new ResponseListingOfNotification();
		List<String> contains = new ArrayList<String>();
		System.out.println("Dimensione JSONList: " + jsonList.size());
		for (int i=0; i<jsonList.size(); i++){
			contains.add("http://example.org/inbox/" + spuid + "/" + i);
		}
		
		
		
		listOfNotification.setContext("http://w3.org/ns/ldp");
		listOfNotification.setId("http://example.org/inbox");
		listOfNotification.setContains(contains);
		
		String responseJson = gson.toJson(listOfNotification);
		//fromJson(stringa, nomeClasse.Class che rappresento) restituisce un oggetto della classe nomeClasse, a partire da una stringa (stringa) che rappresenta un json
		
		NStringEntity entity = new NStringEntity(
				responseJson,
                ContentType.create("application/json-ld", "UTF-8"));
		HttpResponse res = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        res.setEntity(entity);
        
        exchange.submitResponse(new BasicAsyncResponseProducer(res));
        
		} else { //localhost:8000/ldnServlet/spuid/numeroNotifica
		
			String fileName = FILENAME + spuid;
			Type listType = new TypeToken<List<Object>>(){}.getType();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonReader jsonReader = new JsonReader(new FileReader(fileName));
			List<Object> jsonList = gson.fromJson(jsonReader, listType);
			
			if(!jsonList.isEmpty()){ //se il file con nome spuid esiste
				
				Object notifica = jsonList.get(numeroNotifica);
				System.out.println("Notifica: \n\n" + notifica.toString());
				String result = gson.toJson(notifica);
				NStringEntity entity = new NStringEntity(
						result,
		                ContentType.create("application/json-ld", "UTF-8"));
				HttpResponse res = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
		        res.setEntity(entity);
		        
		        exchange.submitResponse(new BasicAsyncResponseProducer(res));
			}
			
		} //else

	} //sendResponse

	@Override
	public void notifyEvent(Notification notify) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendPing(Ping ping) throws IOException {
		// TODO Auto-generated method stub

	}

}
