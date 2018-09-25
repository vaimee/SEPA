package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;



public class ldnHandler implements EventHandler{
	
	private HttpAsyncExchange exchange;
	private static String FILENAME = "/Users/AlessioPazzani/Documents/Tesi/FileSpuid/";
	private BufferedWriter b;

	public ldnHandler(HttpAsyncExchange httpExchange) throws IllegalArgumentException {
		this.exchange = httpExchange;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sendResponse(Response response) throws IOException {
		org.json.simple.JSONArray ja = new org.json.simple.JSONArray();
		SubscribeResponse responseSubscribe = (SubscribeResponse)response;
	    String spuidString = responseSubscribe.getSpuid();
	    
	    StringTokenizer token = new StringTokenizer(spuidString);
	    String spuid = "null";
	    token.nextToken("/");
	    while(token.hasMoreTokens()){
	    	spuid = token.nextToken();
	    }
	    
	    File file = new File(FILENAME + spuid);
	    file.createNewFile();
	    
	    String stringa = "{\"@context\":\"http://www.w3.org/ns/ldp\"," + "\n" +
	    		"\"@id\":\"http://+ sepaldnexample.it/profile/" + spuid  + "\","+ "\n" + //soggetto
	    		"\"inbox\":\"http://ldnResult/inbox/\"}"; //oggetto
	    		
	
		NStringEntity entity = new NStringEntity(
				stringa, //qui dovrei sostituire stringa a spuid
                ContentType.create("application/json-ld", "UTF-8"));
		HttpResponse res = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        res.setEntity(entity);
        res.addHeader("Link", "<http://arces.wot.it" + "/inbox/" + spuid + ">; rel=\"http://www.w3.org/ns/ldp#inbox\"");
        /* 
         * metodo 1
         * JSONObject jo = new JSONObject();
         * jo.put("\"@context\":", "\"http://www.w3.org/ns/ldp\"");
         * jo.put("\"@id\"", "\"http://" + response.getAsJsonObject().toString() + "/profile\"");
         * jo.put("\"inbox\"", "\"http://" + spuid + "/inbox/\"");
         * ja.add(jo);
         * JSONObject mainObj = new JSONObject();
         * mainObj.put("Elenco", ja);
        */
        
        Type listType = new TypeToken<List<Object>>(){}.getType();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
      
        FileWriter w = new FileWriter(FILENAME + spuid);
        b = new BufferedWriter (w);
        
        ja.add((responseSubscribe.getAsJsonObject()));
               
        String daScrivere = gson.toJson(ja, listType);
        b.write(daScrivere);
                
        b.close();
        
		exchange.submitResponse(new BasicAsyncResponseProducer(res));
		
	}

	@Override
	public void notifyEvent(Notification notify) throws IOException {

		String spuidString = notify.getSpuid();
	    
	    StringTokenizer token = new StringTokenizer(spuidString);
	    String spuid = "null";
	    token.nextToken("/");
	    while(token.hasMoreTokens()){
	    	spuid = token.nextToken();
	    }
		String fileName = FILENAME + spuid;
		
		Type listType = new TypeToken<List<Object>>(){}.getType();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonReader jsonReader = new JsonReader(new FileReader(fileName));
		//JSONArray data = gson.fromJson(reader, Review.class);
		List<Object> jsonList = gson.fromJson(jsonReader, listType);
		
		System.out.println(gson.toJson(jsonList));
				
		jsonList.add(notify.getAsJsonObject());
		
		String result = gson.toJson(jsonList); //trasformo l'elenco degli oggetti in un'unica stringa
		
		/*Passaggi utili al fine di avere anche la notifica, aggiunta, indentata bene */
		jsonList = gson.fromJson(result, listType);
		result = gson.toJson(jsonList);
		
		FileWriter w;
	    w=new FileWriter(fileName);

	    BufferedWriter b;
	    b=new BufferedWriter (w);

	    b.write(result); //sovrascrittura del file
	    
	    b.close();
		
		System.out.println("Spuid notifica: " + spuid);
				
	}

	@Override
	public void sendPing(Ping ping) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
