package it.unibo.arces.wot.sepa.webthings.rfidreader;

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

import jssc.SerialPortList;
import labid.comm.ByteUtils;
import labid.iso15693.ISO15693Reader;
import labid.reader.RFReaderException;

public class LabIdReaderTester {
	private static ISO15693Reader reader;

	public static void main(String[] args) throws IOException, InterruptedException {
		boolean searching = true;
		String[] portNames = null;
		int port = 0;

		Scanner in = new Scanner(System.in);
		while (searching) {
			portNames = SerialPortList.getPortNames();
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

				port = in.nextInt() - 1;
				in.close();

				if (port >= 0) {
					System.out.printf("Selected port: %s\n", portNames[port]);
					searching = false;
				}
			}
		}

		// Create and configure the reader
		reader = new ISO15693Reader();// stream);
		reader.openSerialPort(portNames[port]);

		int N_READS = 10;
		reader.rfReset();
		while (true) {
			HashSet<byte[]> activeTags = new HashSet<byte[]>();

			// reader.rfReset();
			//Thread.sleep(50);

			for (int t = 0; t < N_READS; t++) {
				byte[][] uid = null;
				Thread.sleep(100);
				try {
					uid = reader.inventory();
				} catch (RFReaderException e) {
					System.out.println(e.getMessage());
					continue;
				}
//				if (uid == null)
//					System.out.println(String.format("TAGS[%d]: 0", t));
				// Compose new UID list
				if (uid != null) {
					//System.out.println(String.format("TAGS[%d]: %d", t, uid.length));
					for (int i = 0; i < uid.length; i++) {
						activeTags.add(uid[i]);
						try {
							reader.stayQuiet(uid[i]);
						} catch (RFReaderException e) {
							System.out.println(e.getMessage());
						}
					}
				}
			}

			HashSet<String> tagsPoll = new HashSet<String>();
			for (byte[] tag : activeTags) {
				tagsPoll.add(ByteUtils.toHexString(ByteUtils.revertedCopy(tag), ':'));
				try {
					reader.resetToReady(tag);

				} catch (RFReaderException e) {
					System.out.println(e.getMessage());
				}
				try {
					reader.rfReset();
				} catch (RFReaderException e) {
					System.out.println(e.getMessage());
				}
			}

			// reader.rfOnOff(0);
			// Thread.sleep(1000);
			// reader.rfOnOff(1);
			// reader.rfReset();

			System.out.println(tagsPoll);
		}
	}
}
