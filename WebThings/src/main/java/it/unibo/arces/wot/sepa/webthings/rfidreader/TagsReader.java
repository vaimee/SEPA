package it.unibo.arces.wot.sepa.webthings.rfidreader;

import java.io.IOException;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import labid.comm.ByteUtils;
import labid.comm.SerialStream;
import labid.iso15693.ISO15693Reader;
import labid.reader.RFReaderException;
import labid.reader.ReaderConfiguration;

public abstract class TagsReader {
	private static final Logger logger = LogManager.getLogger("TagsReader");

	private final long PING_PERIOD = 5000;
	private final int N_READS = 10; //10
	private final long DELAY = 10; //100
	
	private static ISO15693Reader reader;
	private ReaderConfiguration settings;

	private InventoryThread inventory = new InventoryThread();
	private boolean running = true;
	private String uidStr = "";
	
	public abstract void onPing();
	public abstract void onTags(HashSet<String> tags);
	
	public TagsReader(String port) throws IOException  {
		// Open serial port
		SerialStream stream = new SerialStream();
		stream.Open(port, 115200);

		// Create and configure the reader
		reader = new ISO15693Reader(stream);

		reader.setDefaultConfiguration();
		settings = reader.getReaderConfiguration();

		// Create the HW UID
		byte[] UID = { 0, 0, 0, 0 };
		try {
			UID = reader.getReaderUID();
		} catch (RFReaderException e) {
			logger.warn(e.getMessage());
		}
		
		for (byte digit : UID) {
			uidStr += String.format("%2X", digit);
		}
	}
	
	public String getReaderUID(){
		return uidStr;
	}

	public void start() throws IOException {
		running = true;
		inventory.start();
	}

	public void stop() throws IOException {
		running = false;
		reader.close();
	}
	
	@SuppressWarnings("unused")
	private void setReaderConfiguration() throws RFReaderException {
		settings.AfiEnabled = false;
		settings.AutoRfOff = false;
		settings.Baudrate = 115;

		settings.BeepOnFailure = false;
		settings.BeepOnSuccess = false;

		settings.DefaultProtocol = (byte) 0xB0; // 0xA0: ISO14443a 0xB0:
												// ISO15693
		settings.DualSubcarrier = false; // FALSE!!!
		settings.HighDataRate = true; // TRUE!!!

		settings.SecurityStatus = false;
		settings.TI = false; // Texas Instrument tags only
		settings.TimeSlot1 = false;
		settings.VCDDataRate256 = true;

		settings.Scan_Enabled = false; //
		settings.Scan_ISO14443A = false; //

		settings.Scan_AsciiOutput = false; //
		settings.Scan_Fast = false; //

		settings.Scan_FirstBlock = 0; // ISO15693
		settings.Scan_IgnoreLast = false; // ISO15693
		settings.Scan_NBlocks = 0; // ISO15693
		settings.Scan_ReadDataBlocks = false; // ISO15693
		settings.Scan_ReadUid = false; // ISO15693
		settings.Scan_SingleRead = false; // ISO15693
		settings.Scan_WriteOk = false; // ISO15693 LIOK

		settings.MSB_first_ISO15693_DataBlocks = false; // ISO15693
		settings.MSB_first_ISO15693_UID = false; // ISO15693

		reader.setReaderConfiguration(settings, false);
	}

	class InventoryThread extends Thread {
		
		private boolean isAlive = false;

		private HashSet<String> readTags(int nReads, long delay) throws InterruptedException, RFReaderException {

			HashSet<byte[]> activeTags = new HashSet<byte[]>();

			for (int t = 0; t < nReads; t++) {
				byte[][] uid = null;
				Thread.sleep(delay);
				try {
					uid = reader.inventory();
				} catch (RFReaderException e) {
					logger.warn(e.getMessage());
					continue;
				}

				// Compose new UID list
				if (uid != null) {

					for (int i = 0; i < uid.length; i++) {
						activeTags.add(uid[i]);
						try {
							reader.stayQuiet(uid[i]);
						} catch (RFReaderException e) {
							logger.warn(e.getMessage());
						}
					}
				}
			}

			isAlive = true;

			HashSet<String> tagsPoll = new HashSet<String>();
			for (byte[] tag : activeTags) {
				tagsPoll.add(ByteUtils.toHexString(ByteUtils.revertedCopy(tag), ':'));
				try {
					reader.resetToReady(tag);

				} catch (RFReaderException e) {
					logger.warn(e.getMessage());
				}
				try {
					reader.rfReset();
				} catch (RFReaderException e) {
					logger.warn(e.getMessage());
				}
			}

			return tagsPoll;
		}

		public void run() {
			new Thread() {
				@Override
				public void run() {
					while (running) {
						try {
							Thread.sleep(PING_PERIOD);
						} catch (InterruptedException e) {
							logger.debug("Ping thread exit!");
							return;
						}

						if (isAlive) {
							isAlive = false;
							onPing();
						}
					}
				}
			}.start();

			try {
				reader.rfReset();
			} catch (RFReaderException e1) {
				logger.error(e1.getMessage());
			}

			while (running) {
				// Read tags
				HashSet<String> current = null;
				try {
					current = readTags(N_READS, DELAY);
				} catch (RFReaderException | InterruptedException e) {
					return;
				}

				// Notify tags
				onTags(current);
			}
		}

	}
}
