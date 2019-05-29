package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;

public class DtnRequestHeader {
	
	// XXX insert here the request header parameters
	
	/**
	 * Creates a request header from the buffer inserted in a bundle
	 * @param buffer the data inside the bundle
	 * @return the request header created
	 */
	public static DtnRequestHeader getRequestHeader(ByteBuffer buffer) {
		return new DtnRequestHeader();
	}
	
}
