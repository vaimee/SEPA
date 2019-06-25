package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.api.protocols.dtn.DTNSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.dtn.JAL.BundleEID;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;

public abstract class DTNConsumer extends AbstractConsumer {

	public DTNConsumer(JSAP appProfile, String subscribeID, SEPASecurityManager sm, BundleEID destination)
			throws SEPAProtocolException, SEPASecurityException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		super(appProfile, subscribeID, sm, new DTNSubscriptionProtocol(destination));
	}
	
}
