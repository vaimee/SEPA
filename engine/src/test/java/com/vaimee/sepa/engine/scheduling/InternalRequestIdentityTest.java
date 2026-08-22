package com.vaimee.sepa.engine.scheduling;

import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.api.commons.security.Credentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalRequestIdentityTest {

    @Test
    void internalQueryRequestExposesAuthorizationIdentity() throws Exception {
        ClientAuthorization authorization = new ClientAuthorization(
                new Credentials("endpoint-user", "endpoint-password"),
                "zitadel-sub-123");

        InternalQueryRequest request = new InternalQueryRequest("ASK { ?s ?p ?o }", null, null, authorization);

        assertEquals("zitadel-sub-123", request.getIdentity());
    }
}
