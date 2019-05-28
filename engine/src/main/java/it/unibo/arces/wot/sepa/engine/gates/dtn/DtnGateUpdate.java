package it.unibo.arces.wot.sepa.engine.gates.dtn;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class DtnGateUpdate extends DtnGate {

	private static final String DEMUXSTRING = "/sepa/update";
	private static final int DEMUXIPN = 150;
	
	public DtnGateUpdate(Scheduler scheduler) {
		super(scheduler, DEMUXSTRING, DEMUXIPN);
	}

	@Override
	protected InternalRequest getInternalRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		return new InternalUpdateRequest(sparql, defaultGraphUri, namedGraphUri);
	}

}
