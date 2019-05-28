package it.unibo.arces.wot.sepa.engine.gates.dtn;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class DtnGateQuery extends DtnGate {

	private static final String DEMUXSTRING = "/sepa/query";
	private static final int DEMUXIPN = 151;
	
	public DtnGateQuery(Scheduler scheduler) {
		super(scheduler, DEMUXSTRING, DEMUXIPN);
	}
	
	@Override
	protected InternalRequest getInternalRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		return new InternalQueryRequest(sparql, defaultGraphUri, namedGraphUri);
	}

}
