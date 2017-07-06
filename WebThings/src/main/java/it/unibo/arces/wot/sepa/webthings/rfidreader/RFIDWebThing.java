package it.unibo.arces.wot.sepa.webthings.rfidreader;

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
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.framework.ThingDescription;
import it.unibo.arces.wot.sepa.framework.interaction.EventPublisher;

import jssc.SerialPortList;

public class RFIDWebThing extends TagsReader {
	private static final Logger logger = LogManager.getLogger("RFIDWebThing");

	// WoT Framework
	private ThingDescription td;
	private EventPublisher event;

	// URIs and names
	private final String pingEventURI = "wot:Ping";
	private final String pingEventName = "RFID Ping";
	private final String tagsPollChangedURI = "wot:RFIDReading";
	private final String tagsPollChangedName = "RFID Reading";
	private final String thingURIPrefix = "wot:LABID_RFID_READER_";
	private final String thingName = "ARCES RFID UID:";

	private CheckerWithHysteresis tagsChecker = new CheckerWithHysteresis(false);

	public RFIDWebThing(String port) throws IOException, UnrecoverableKeyException, KeyManagementException,
			InvalidKeyException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException {
		super(port);

		// Publish the Thing Description
		String thingURI = thingURIPrefix + getReaderUID();
		td = new ThingDescription(thingURI, thingName + getReaderUID());
		td.addEvent(tagsPollChangedURI, tagsPollChangedName, "xsd:string");
		td.addEvent(pingEventURI, pingEventName);

		// Create the event generator
		event = new EventPublisher(thingURI);
	}

	public static void main(String[] args) throws IOException {
		Scanner in = new Scanner(System.in);
		boolean searching = true;
		String portName = "";

		System.out.println("Number of arguments: " + args.length);
		
		if (args.length == 1) {
			portName = args[0];
		} else {
			
			while (searching) {
				// 0 - /dev/tty.usbserial-000012FD
				String[] portNames = SerialPortList.getPortNames();
				if (portNames.length == 0) {
					System.out.println("No serial ports found...press CTRL+C to exit");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						in.close();
						return;
					}
				} else {
					System.out.println("Choose one of the following serial ports: (0 no selection)");
					for (int i = 0; i < portNames.length; i++) {
						System.out.printf("%d - %s\n", i + 1, portNames[i]);
					}

					int port = in.nextInt() - 1;

					if (port >= 0) {
						System.out.printf("Selected port: %s\n", portNames[port]);
						searching = false;
						portName = portNames[port];
					}
				}
			}
		}

		// Web Thing adapter
		RFIDWebThing adapter;
		try {
			adapter = new RFIDWebThing(portName);
		} catch (UnrecoverableKeyException | KeyManagementException | InvalidKeyException | IllegalArgumentException
				| KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchElementException
				| NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | IOException | URISyntaxException e) {
			logger.fatal(e.getMessage());
			in.close();
			return;
		}
		adapter.start();

		System.out.println("RFID adapter is running...");

		System.out.println("Press any key and hit return to exit");
		while (!in.hasNext()) {
		}
		in.close();

		adapter.stop();

		System.out.println("RFID adapter stopped");
		System.exit(0);
	}

	@Override
	public void onPing() {
		logger.info("Event: " + pingEventURI);
		event.post(pingEventURI);
	}

	@Override
	public void onTags(HashSet<String> tags) {

		HashSet<String> activeTags = tagsChecker.checkChanges(tags);
		String tagsPoll = "";
		for (String tag : activeTags) {
			if (tagsPoll.equals(""))
				tagsPoll += tag;
			else
				tagsPoll += "|" + tag;
		}

		if (tagsPoll.equals(""))
			tagsPoll = "EMPTY";

		if (tagsChecker.isChanged()) {
			logger.info("Event: " + tagsPollChangedURI);
			event.post(tagsPollChangedURI, tagsPoll);
		}
	}

}
