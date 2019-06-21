package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.DTNProtocol;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;

public class DTNProducer extends AbstractProducer {

	public DTNProducer(JSAP appProfile, String updateID, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		super(appProfile, updateID, sm, new DTNProtocol());
	}

}
