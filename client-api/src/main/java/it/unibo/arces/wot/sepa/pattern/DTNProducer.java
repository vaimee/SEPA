package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.DTNProtocol;
import it.unibo.arces.wot.sepa.commons.response.Response;
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
	
	@Override
	public final Response update() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return this.update(-1);
	}
	
	@Override
	public final Response update(int timeout) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		appProfile.setDTN(true);
		 
		 Response result = super.update(timeout);
		 
		 appProfile.setDTN(false);
		 
		 return result;
	 }

}
