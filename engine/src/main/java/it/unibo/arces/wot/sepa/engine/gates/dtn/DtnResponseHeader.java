package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;

import it.unibo.dtn.JAL.BundleTimestamp;

public class DtnResponseHeader {
	
	private BundleTimestamp timestamp;
	
	public DtnResponseHeader(BundleTimestamp timestamp) {
		this.timestamp = timestamp;
	}
	
	public int getHeaderSize() {
		return Integer.BYTES * 2;
	}
	
	public void insertHeaderInByteBuffer(ByteBuffer buffer) {
		buffer.putInt(this.timestamp.getSeconds());
		buffer.putInt(this.timestamp.getSequenceNumber());
	}
	
}
