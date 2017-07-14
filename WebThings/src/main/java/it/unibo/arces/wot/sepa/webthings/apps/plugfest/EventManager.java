package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.framework.elements.Event;
import it.unibo.arces.wot.framework.interaction.EventListener;
import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.COLOR;

public abstract class EventManager extends EventListener {
	protected static final Logger logger = LogManager.getLogger("WoTDemoContextManager");

	// Events
	private static final String REED_EVENT = "wot:ReedSensorValueChangedEvent";
	private static final String RFID_EVENT = "wot:RFIDReading";

	public EventManager(Context context)
			throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException,
			NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException,
			KeyManagementException, KeyStoreException, CertificateException, URISyntaxException, InterruptedException {
		super();

		if (context == null)
			throw new IllegalArgumentException("Context is null");
		this.context = context;
		
		onEmpty();
	}

	public void startListeningForEvents() throws InvalidKeyException, UnrecoverableKeyException, KeyManagementException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
			KeyStoreException, CertificateException, IOException, URISyntaxException, InterruptedException {
		
		startListeningForEvent(RFID_EVENT);
		startListeningForEvent(REED_EVENT);
	}

	public abstract void onColors(Set<COLOR> colors);

	public abstract void onPlayCards(boolean win);

	public abstract void onEmpty();

	public abstract void onReedEvent(boolean on);
	
	public abstract void onRFIDTags(String[] tags);
	
	public abstract void onRFIDTag(String tag);

	// Context reference
	private Context context;

	@Override
	public void onEvent(Set<Event> events) {
		
		for (Event event : events) {
			logger.debug(event.getValue());
		
			if (event.getEvent().equals(RFID_EVENT)) {
				if (event.getValue().equals("EMPTY")) {
					onEmpty();
					continue;
				}
				
				String[] tags = event.getValue().split("\\|");
				
				switch (context.getActiveContextType()) {
				case USERS:
					if (tags.length < 1) onRFIDTags(tags);
					else onRFIDTag(tags[0]);
					break;
				case COLORS:
					
					
					Set<COLOR> colors = new HashSet<COLOR>();
					for (String tag : tags) {
						COLOR c = context.getColor(tag);
						if (c != null)
							colors.add(c);
					}
					onColors(colors);
					break;
				case CARDS:
					if (context.isJolly(event.getValue())) {
						onPlayCards(true);
					} else {
						onPlayCards(false);
					}
					break;
				}
			}
			if (event.getEvent().equals(REED_EVENT)) {
				onReedEvent(Boolean.parseBoolean(event.getValue()));
			}
		}
	}
}
