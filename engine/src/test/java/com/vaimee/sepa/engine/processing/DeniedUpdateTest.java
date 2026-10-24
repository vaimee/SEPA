package com.vaimee.sepa.engine.processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.SPARQL11Properties;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.core.EngineProperties;
import com.vaimee.sepa.engine.core.EventHandler;
import com.vaimee.sepa.engine.dependability.acl.EngineACLException;
import com.vaimee.sepa.engine.scheduling.InternalSubscribeRequest;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.engine.scheduling.Scheduler;

/**
 * An update refused by the ACL must not block the next ones (#118).
 *
 * The SPUs of an update wait for its post-update after their pre-update. The
 * ACL refusal is a RuntimeException thrown by the endpoint: if it skips the
 * abort, the SPUs keep waiting and the next update waits for them forever.
 */
public class DeniedUpdateTest {
	private static final String GRAPH = "urn:test:denied-update";

	/** Refuses every update as the ACL does, with an EngineACLException (a RuntimeException). */
	private static class DenyingUpdateProcessor extends UpdateProcessor {
		DenyingUpdateProcessor(SPARQL11Properties properties) throws SEPAProtocolException, SEPASecurityException {
			super(properties);
		}

		@Override
		public Response process(InternalUpdateRequest req) throws SEPASecurityException, IOException {
			throw new EngineACLException("User test cannot access " + GRAPH);
		}
	}

	private static final EventHandler NO_CLIENT = new EventHandler() {
		@Override
		public void notifyEvent(Notification notify) {
		}

		@Override
		public boolean ping() {
			return true;
		}
	};

	@Test
	public void aDeniedUpdateDoesNotBlockTheNextOnes() throws Exception {
		EngineProperties properties = new EngineProperties(new String[0]);
		EngineBeans.setEngineProperties(properties);
		Processor processor = new Processor(properties, new Scheduler(properties));

		// A subscription on the graph, so that the updates run its SPU
		Response subscribed = processor.processSubscribe(new InternalSubscribeRequest(
				"SELECT * WHERE { GRAPH <" + GRAPH + "> { ?s ?p ?o } }", "denied-update", null, null, NO_CLIENT,
				new ClientAuthorization()));
		assertFalse(subscribed.isError(), subscribed.toString());

		Field field = Processor.class.getDeclaredField("updateProcessor");
		field.setAccessible(true);
		UpdateProcessor endpoint = (UpdateProcessor) field.get(processor);

		field.set(processor, new DenyingUpdateProcessor(properties.getEndpointProperties()));
		assertThrows(EngineACLException.class, () -> processor.processUpdate(update(1)));

		field.set(processor, endpoint);
		Response next = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> processor.processUpdate(update(2)),
				"The update after a denied one is blocked");
		assertFalse(next.isError(), next.toString());
	}

	private static InternalUpdateRequest update(int value) throws Exception {
		return new InternalUpdateRequest("INSERT DATA { GRAPH <" + GRAPH + "> { <urn:test:s> <urn:test:p> " + value + " } }",
				null, null, new ClientAuthorization());
	}
}
