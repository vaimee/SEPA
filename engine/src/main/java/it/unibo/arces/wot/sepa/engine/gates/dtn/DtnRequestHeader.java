package it.unibo.arces.wot.sepa.engine.gates.dtn;

import java.nio.ByteBuffer;

public class DtnRequestHeader {
	
	public static DtnRequestHeader getRequestHeader(ByteBuffer buffer) {
		return new DtnRequestHeader();
	}
	
}
