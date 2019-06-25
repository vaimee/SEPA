package it.unibo.arces.wot.sepa.api.protocol.dtn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.DTNConsumer;
import it.unibo.arces.wot.sepa.pattern.DTNProducer;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.dtn.JAL.BundleEID;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;

public class DTNMain {

	private static final int STOP = 5;
	
	public static void main(String[] args) throws Exception {
		JSAP appProfile = new JSAP("sepatestDTN.jsap");
		String subscribeID = "RANDOM";
		SEPASecurityManager sm = null;
		BundleEID destination = BundleEID.of("ipn:5.152");
		TestConsumer consumer = new TestConsumer(appProfile, subscribeID, sm, destination);
		consumer.subscribe(60);
		
		DTNProducer producer = new DTNProducer(appProfile, subscribeID, sm);
		for (int i = 0; i < STOP; i++) {
			producer.update();
			Thread.sleep(1500);
		}
		
		Thread.sleep(2000);
		
		producer.close();
		consumer.close();
	}

}


class TestConsumer extends DTNConsumer {
	private static final Logger logger = LogManager.getLogger();

	public TestConsumer(JSAP appProfile, String subscribeID, SEPASecurityManager sm, BundleEID destination)
			throws SEPAProtocolException, SEPASecurityException, JALLocalEIDException, JALOpenException,
			JALIPNParametersException, JALRegisterException {
		super(appProfile, subscribeID, sm, destination);
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.info("Sequence: " + notify.getSequence());
		super.onSemanticEvent(notify);
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		logger.info(results.toString());
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		logger.info(results.toString());
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		logger.info(results.toString());
	}

	@Override
	public void onBrokenConnection() {
		logger.info("Broken connection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.info(errorResponse.toString());
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.info("SPUID:"+ spuid.toString());
		logger.info("ALIAS:" + alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.info(spuid.toString());
	}
	
}