package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.engine.dependability.acl.SEPAAcl;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorage;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageFactory;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageOperations;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.acl.ACLException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgoraStudioAclPolicyTest {
    private static final String FARMER = "studio-user-farmer";
    private static final String DEVELOPER = "studio-user-developer";
    private static final String AGRONOMIST = "studio-user-agronomist";
    private static final String STUDENT = "studio-user-student";
    private static final String COOPERATIVE = "studio-user-cooperative";
    private static final String AGENT = "studio-user-agent";
    private static final String NO_ROLE = "studio-user-no-role";

    private static final String AGORA = "http://agora.vaimee.com/graphs/agora-app";
    private static final String SCENARIOS = "http://agora.vaimee.com/graphs/scenarios";
    private static final String CRITERIA = "http://agora.vaimee.com/graphs/criteria-web";
    private static final String API = "http://agora.vaimee.com/graphs/agora-api";
    private static final String DASHBOARD = "http://agora.vaimee.com/graphs/sepa-dashboard";
    private static final String AGRITWIX = "http://agora.vaimee.com/graphs/agritwix";
    private static final String WDA = "http://agora.vaimee.com/graphs/wda";
    private static final String AGENTS = "http://agora.vaimee.com/graphs/agents";
    private static final String PRIVATE = "http://agora.vaimee.com/graphs/private-admin";

    @Test
    void agoraStudioRolesCanOnlyAccessTheirConfiguredGraphs() throws Exception {
        String scope = "read";
        SEPAAcl acl = newEmptyAcl();
        configureAclAsAdmin(acl, scope);
        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);

        insertSeedDataAsAdmin(dataset, scope);

        assertCanQueryOnly(dataset, u(FARMER, scope), scope, AGORA);
        assertCanQueryOnly(dataset, u(AGRONOMIST, scope), scope, AGORA, SCENARIOS, CRITERIA);
        assertCanQueryOnly(dataset, u(STUDENT, scope), scope, AGRITWIX);
        assertCanQueryOnly(dataset, u(COOPERATIVE, scope), scope, AGORA, SCENARIOS, WDA);
        assertCanQueryOnly(dataset, u(AGENT, scope), scope, AGENTS);
        assertCanQueryOnly(dataset, u(NO_ROLE, scope), scope);

        assertEquals(1, countVisibleRows(dataset, DatasetACL.ADMIN_USER, g(PRIVATE, scope)));
        assertCanInsert(dataset, DatasetACL.ADMIN_USER, g(PRIVATE, scope));
    }

    @Test
    void updatePermissionsAreGrantedOnlyToDeveloperAndAgent() throws Exception {
        String scope = "write";
        SEPAAcl acl = newEmptyAcl();
        configureAclAsAdmin(acl, scope);
        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);

        insertSeedDataAsAdmin(dataset, scope);

        assertCanQueryOnly(dataset, u(DEVELOPER, scope), scope, API, DASHBOARD);
        assertCanInsert(dataset, u(DEVELOPER, scope), g(API, scope));
        assertCanInsert(dataset, u(DEVELOPER, scope), g(DASHBOARD, scope));
        assertCannotInsert(dataset, u(DEVELOPER, scope), g(AGORA, scope));

        assertCanInsert(dataset, u(AGENT, scope), g(AGENTS, scope));
        assertCannotInsert(dataset, u(AGENT, scope), g(DASHBOARD, scope));
        assertCannotInsert(dataset, u(FARMER, scope), g(AGORA, scope));
        assertCannotInsert(dataset, u(AGRONOMIST, scope), g(CRITERIA, scope));
        assertCannotInsert(dataset, u(STUDENT, scope), g(AGRITWIX, scope));
        assertCannotInsert(dataset, u(COOPERATIVE, scope), g(WDA, scope));
        assertCannotInsert(dataset, u(NO_ROLE, scope), g(AGORA, scope));
    }

    private SEPAAcl newEmptyAcl() throws Exception {
        ACLStorageOperations storage = ACLStorageFactory.newInstance(
                ACLStorage.ACLStorageId.asiDataset,
                new TreeMap<String, Object>());
        return SEPAAcl.newInstance(storage);
    }

    private void configureAclAsAdmin(SEPAAcl acl, String scope) throws Exception {
        addReadOnlyUser(acl, scope, FARMER, AGORA);
        addReadOnlyUser(acl, scope, AGRONOMIST, AGORA, SCENARIOS, CRITERIA);
        addReadOnlyUser(acl, scope, STUDENT, AGRITWIX);
        addReadOnlyUser(acl, scope, COOPERATIVE, AGORA, SCENARIOS, WDA);
        addReadWriteUser(acl, scope, DEVELOPER, API, DASHBOARD);
        addReadWriteUser(acl, scope, AGENT, AGENTS);
        acl.addUser(u(NO_ROLE, scope));
    }

    private void addReadOnlyUser(SEPAAcl acl, String scope, String user, String... graphs) throws Exception {
        acl.addUser(u(user, scope));
        for (String graph : graphs) {
            acl.addUserPermission(u(user, scope), g(graph, scope), DatasetACL.aclId.aiQuery);
        }
    }

    private void addReadWriteUser(SEPAAcl acl, String scope, String user, String... graphs) throws Exception {
        acl.addUser(u(user, scope));
        for (String graph : graphs) {
            acl.addUserPermission(u(user, scope), g(graph, scope), DatasetACL.aclId.aiQuery);
            acl.addUserPermission(u(user, scope), g(graph, scope), DatasetACL.aclId.aiUpdate);
            acl.addUserPermission(u(user, scope), g(graph, scope), DatasetACL.aclId.aiInsertData);
        }
    }

    private void insertSeedDataAsAdmin(Dataset dataset, String scope) {
        for (String graph : allGraphs(scope)) {
            insertAs(dataset, DatasetACL.ADMIN_USER, graph, "seed");
        }
    }

    private void assertCanQueryOnly(Dataset dataset, String user, String scope, String... allowedGraphs) {
        for (String graph : allGraphs(scope)) {
            int expected = contains(allowedGraphs, graph, scope) ? 1 : 0;
            assertEquals(expected, countVisibleRows(dataset, user, graph), user + " on " + graph);
        }
    }

    private void assertCanInsert(Dataset dataset, String user, String graph) {
        int before = countVisibleRows(dataset, DatasetACL.ADMIN_USER, graph);
        insertAs(dataset, user, graph, "write-" + user.hashCode());
        assertEquals(before + 1, countVisibleRows(dataset, DatasetACL.ADMIN_USER, graph), user + " should write " + graph);
    }

    private void assertCannotInsert(Dataset dataset, String user, String graph) {
        int before = countVisibleRows(dataset, DatasetACL.ADMIN_USER, graph);
        try {
            insertAs(dataset, user, graph, "denied-" + user.hashCode());
        } catch (ACLException ignored) {
            // Denied updates may either be filtered out or rejected by Jena ACL.
        }
        assertEquals(before, countVisibleRows(dataset, DatasetACL.ADMIN_USER, graph), user + " should not write " + graph);
    }

    private void insertAs(Dataset dataset, String user, String graph, String object) {
        try (RDFConnection connection = RDFConnectionFactory.connect(dataset, user)) {
            Txn.executeWrite(connection, () -> connection.update(
                    "INSERT DATA { GRAPH <" + graph + "> { <urn:s> <urn:p> <urn:" + object + "> } }"));
        }
    }

    private int countVisibleRows(Dataset dataset, String user, String graph) {
        try (RDFConnection connection = RDFConnectionFactory.connect(dataset, user)) {
            return Txn.calculateRead(connection, () -> {
                final int[] count = {0};
                connection.querySelect("SELECT * WHERE { GRAPH <" + graph + "> { ?s ?p ?o } }", row -> count[0]++);
                return count[0];
            });
        }
    }

    private String[] allGraphs(String scope) {
        return new String[] {
                g(AGORA, scope),
                g(SCENARIOS, scope),
                g(CRITERIA, scope),
                g(API, scope),
                g(DASHBOARD, scope),
                g(AGRITWIX, scope),
                g(WDA, scope),
                g(AGENTS, scope),
                g(PRIVATE, scope)
        };
    }

    private boolean contains(String[] values, String expected, String scope) {
        for (String value : values) {
            if (g(value, scope).equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private String u(String user, String scope) {
        return user + "-" + scope;
    }

    private String g(String graph, String scope) {
        return graph + "/" + scope;
    }
}
