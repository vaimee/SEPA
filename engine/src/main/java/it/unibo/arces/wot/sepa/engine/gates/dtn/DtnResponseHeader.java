package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;

import it.unibo.dtn.JAL.BundleTimestamp;

public class DtnResponseHeader {
	
	private BundleTimestamp timestamp;
	
	/**
	 * Creates a reponse header
	 * @param timestamp the timestamp of bundle (used to identify uniquely the bundle)
	 */
	public DtnResponseHeader(BundleTimestamp timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Returns the header size
	 * @return the header size
	 */
	public int getHeaderSize() {
		return Integer.SIZE / 8 * 2;
	}
	
	/**
	 * Inserts the header in a byte buffer
	 * @param buffer the byte buffer
	 */
	public void insertHeaderInByteBuffer(ByteBuffer buffer) {
		buffer.putInt(this.timestamp.getSeconds());
		buffer.putInt(this.timestamp.getSequenceNumber());
	}
	
}
