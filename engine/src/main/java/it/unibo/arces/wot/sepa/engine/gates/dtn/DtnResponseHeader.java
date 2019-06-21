package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import it.unibo.dtn.JAL.BundleTimestamp;

public class DtnResponseHeader {
	
	private BundleTimestamp timestamp;
	private String message;
	private int resultCode;
	private String errorDescription;
	
	/**
	 * Creates a reponse header
	 * @param timestamp the timestamp of bundle (used to identify uniquely the bundle)
	 */
	public DtnResponseHeader(BundleTimestamp timestamp, String message, int resultCode, String errorDescription) {
		this.timestamp = timestamp;
		this.resultCode = resultCode;
		this.message = message;
		this.errorDescription = errorDescription;
	}
	
	/**
	 * Returns the header size
	 * @return the header size
	 */
	public int getHeaderSize() {
		return Integer.SIZE / 8 * (2 + 1 + 1 + 1) + StandardCharsets.UTF_8.encode(this.message).array().length +  StandardCharsets.UTF_8.encode(this.errorDescription).array().length;
	}
	
	/**
	 * Inserts the header in a byte buffer
	 * @param buffer the byte buffer
	 */
	public void insertHeaderInByteBuffer(ByteBuffer buffer) {
		buffer.putInt(this.timestamp.getSeconds());
		buffer.putInt(this.timestamp.getSequenceNumber());
		
		byte[] messageArray = this.message.getBytes(StandardCharsets.UTF_8); 
		buffer.putInt(messageArray.length);
		buffer.put(messageArray);
		
		buffer.putInt(this.resultCode);
		
		byte[] descriptionArray = this.errorDescription.getBytes(StandardCharsets.UTF_8);
		buffer.putInt(descriptionArray.length);
		buffer.put(descriptionArray);
	}
	
}
