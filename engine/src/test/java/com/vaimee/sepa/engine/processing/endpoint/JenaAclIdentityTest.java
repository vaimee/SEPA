package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.engine.dependability.acl.SEPAAcl;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorage;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageFactory;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageOperations;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JenaAclIdentityTest {
    @Test
    void jenaDatasetAppliesAclUsingConnectionUser() throws Exception {
        String graph = "http://example.com/graphs/allowed-user";
        SEPAAcl acl = aclWithReadOnlyUserOnGraph("alice-user", graph);
        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);

        try (RDFConnection admin = RDFConnectionFactory.connect(dataset, DatasetACL.ADMIN_USER)) {
            Txn.executeWrite(admin, () -> admin.update("INSERT DATA { GRAPH <" + graph + "> { <urn:s> <urn:p> <urn:o> } }"));
        }

        assertEquals(1, countVisibleRows(dataset, "alice-user", graph));
        assertEquals(0, countVisibleRows(dataset, "bob-user", graph));
    }

    @Test
    void jenaDatasetAclAdminUserBypassesGraphRestrictions() throws Exception {
        String graph = "http://example.com/graphs/allowed-admin";
        SEPAAcl acl = aclWithReadOnlyUserOnGraph("alice-admin", graph);
        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);

        try (RDFConnection admin = RDFConnectionFactory.connect(dataset, DatasetACL.ADMIN_USER)) {
            Txn.executeWrite(admin, () -> admin.update("INSERT DATA { GRAPH <" + graph + "> { <urn:s> <urn:p> <urn:o> } }"));
        }

        assertEquals(1, countVisibleRows(dataset, DatasetACL.ADMIN_USER, graph));
    }

    private SEPAAcl aclWithReadOnlyUserOnGraph(String user, String graph) throws Exception {
        ACLStorageOperations storage = ACLStorageFactory.newInstance(
                ACLStorage.ACLStorageId.asiDataset,
                new TreeMap<String, Object>());
        SEPAAcl acl = SEPAAcl.newInstance(storage);
        acl.addUser(user);
        acl.addUserPermission(user, graph, DatasetACL.aclId.aiQuery);
        return acl;
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
}
