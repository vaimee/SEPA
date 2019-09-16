package it.unibo.arces.wot.sepa.engine.gates.dtn;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class DtnGateUpdate extends DtnGate {

	public DtnGateUpdate(EngineProperties properties, Scheduler scheduler) {
		super(scheduler, properties.getUpdateDemuxDTN(), properties.getUpdateDemuxIPN());
	}

	@Override
	protected InternalRequest getInternalRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		return new InternalUpdateRequest(sparql, defaultGraphUri, namedGraphUri);
	}

}
