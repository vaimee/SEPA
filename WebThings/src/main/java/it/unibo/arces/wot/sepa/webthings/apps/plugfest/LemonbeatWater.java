package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.framework.Event;
import it.unibo.arces.wot.sepa.framework.interaction.EventListener;

public class LemonbeatWater extends EventListener {
	/** The httpclient. */
	protected CloseableHttpClient httpclient = HttpClients.createDefault();
	
	/** The response handler. */
	protected static ResponseHandler<String> responseHandler;
	
	public LemonbeatWater() throws InvalidKeyException, FileNotFoundException, NoSuchElementException,
			IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException, InterruptedException {
		super();
		this.startListeningForEvent("wot:RFIDReading");
	}
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException, InterruptedException { 
		responseHandler = new ResponseHandler<String>() {
	        @Override
	        public String handleResponse(final HttpResponse response) {
	        	//Body
	        	String body = null;
	        	HttpEntity entity = response.getEntity();
	            
				try {
					body = EntityUtils.toString(entity,Charset.forName("UTF-8"));
				} catch (ParseException | IOException e) {
					return e.getMessage();
				}
				
				return body;
	        }
		};
		LemonbeatWater adapter = new LemonbeatWater();
		adapter.wait();
	}
	
	@Override
	public void onEvent(Set<Event> events) {
		for (Event event: events) {
			if (!event.getEvent().equals("wot:RFIDReading")) continue;
			
			String[] tags = event.getValue().split("\\|");
			if (tags.length != 1) continue;
			switch(tags[0]) {
			case "E0:02:22:0C:47:08:C2:C6":
				//RED close http://192.168.1.144:8080/water/turnoff
				try {
					httpclient.execute(new HttpPost("http://192.168.1.144:8080/water/turnoff"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "E0:02:22:0C:47:08:BA:57":
				//GREEN open http://192.168.1.144:8080/water/turnon
				try {
					httpclient.execute(new HttpPost("http://192.168.1.144:8080/water/turnon"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		
	}

	@Override
	public void onConnectionStatus(Boolean on) {
		
	}

	@Override
	public void onConnectionError(ErrorResponse error) {
		
	}

	
}
