package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ResponseAndNotificationListener {
		public void notify(Response response);
}
