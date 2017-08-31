package it.unibo.arces.wot.sepa.apps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RoadLightEx extends RoadExperiment {

	public RoadLightEx() throws FileNotFoundException, NoSuchElementException, IOException, UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException {
		super();
	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void runExperiment() {
		super.runExperiment();	
	}

	@Override
	public void dataset() {
		roads = new int[]{1,1,1,1};
		roadSizes = new int[]{10,25,50,100};
		super.dataset();	
	}

	@Override
	public void subscribe() {
		//Road subscriptions
		roadSubscriptionRoads = new int[]{1,2,3,4};
		
		//Lamp subscriptions
		lampSubscriptionRoads = new int[][]{{1,1},{2,2},{3,3},{4,4}};
		lampSubscriptionLamps = new int[][]{{1,10},{1,25},{1,50},{1,100}};
		super.subscribe();
	}
	
	public static void main(String[] args)  {
		RoadLightEx benchmark = null;
		try {
			benchmark = new RoadLightEx();
		} catch (NoSuchElementException | IOException | UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidKeyException | NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
			
			e.printStackTrace();
			System.exit(1);
		}
		try {
			benchmark.run(true,true,5000);
		} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException
				| NoSuchAlgorithmException | CertificateException | IOException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
			
			e.printStackTrace();
			System.exit(1);
		}
	}

}
