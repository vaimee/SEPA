package it.unibo.arces.wot.sepa.engine.gates.dtn;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class DtnGateQuery extends DtnGate {

	public DtnGateQuery(EngineProperties properties, Scheduler scheduler) {
		super(scheduler, properties.getQueryDemuxDTN(), properties.getQueryDemuxIPN());
	}
	
	@Override
	protected InternalRequest getInternalRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		return new InternalQueryRequest(sparql, defaultGraphUri, namedGraphUri);
	}

}
