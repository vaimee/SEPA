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

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Does the ACL filter apply to GRAPH ?g, or only to graphs named explicitly?
 *
 * AgoraStudioAclPolicyTest establishes that a query against a named graph the
 * user cannot see returns zero rows rather than failing. That leaves open the
 * question this test answers: whether a pattern that ranges over graphs with a
 * variable is likewise reduced to the permitted ones, or whether it reads the
 * whole dataset.
 *
 * The answer decides whether AgoraWasteMap can give each producer their own
 * graph and still let a buyer discover listings with one query, or whether
 * published listings have to be collected into a single shared graph.
 */
class VariableGraphAclTest {

    private static final String PRODUCER_A = "http://byebyproduct.org/data/producer/aaa/published";
    private static final String PRODUCER_B = "http://byebyproduct.org/data/producer/bbb/published";
    private static final String PRODUCER_A_DRAFTS = "http://byebyproduct.org/data/producer/aaa/drafts";

    private static final String BUYER = "buyer-sub-1";
    private static final String PRODUCER_A_USER = "producer-a-sub";

    @Test
    void variableGraphPatternIsReducedToThePermittedGraphs() throws Exception {
        SEPAAcl acl = newEmptyAcl();

        // The buyer may read both published graphs, and neither draft graph.
        String buyer = BUYER + "-read";
        acl.addUser(buyer);
        acl.addUserPermission(buyer, PRODUCER_A, DatasetACL.aclId.aiQuery);
        acl.addUserPermission(buyer, PRODUCER_B, DatasetACL.aclId.aiQuery);

        // Producer A may read only their own two graphs.
        String producer = PRODUCER_A_USER + "-read";
        acl.addUser(producer);
        acl.addUserPermission(producer, PRODUCER_A, DatasetACL.aclId.aiQuery);
        acl.addUserPermission(producer, PRODUCER_A_DRAFTS, DatasetACL.aclId.aiQuery);

        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);
        for (String graph : new String[] { PRODUCER_A, PRODUCER_B, PRODUCER_A_DRAFTS }) {
            insertAs(dataset, DatasetACL.ADMIN_USER, graph);
        }

        Set<String> adminSees = graphsVisibleTo(dataset, DatasetACL.ADMIN_USER);
        Set<String> buyerSees = graphsVisibleTo(dataset, buyer);
        Set<String> producerSees = graphsVisibleTo(dataset, producer);

        System.out.println("=== SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o } } ===");
        System.out.println("  admin      -> " + adminSees);
        System.out.println("  buyer      -> " + buyerSees);
        System.out.println("  producer A -> " + producerSees);

        assertEquals(3, adminSees.size(), "admin should see every graph");

        // The question under test.
        assertEquals(Set.of(PRODUCER_A, PRODUCER_B), buyerSees,
                "a variable-graph query should return the buyer's permitted graphs only");
        assertEquals(Set.of(PRODUCER_A, PRODUCER_A_DRAFTS), producerSees,
                "a variable-graph query should return the producer's own graphs only");

        // The draft must not leak to the buyer, which is the property the whole
        // graph partition exists to enforce.
        assertTrue(!buyerSees.contains(PRODUCER_A_DRAFTS), "drafts must stay private");
    }

    @Test
    void unionDefaultGraphDoesNotBypassTheAcl() throws Exception {
        SEPAAcl acl = newEmptyAcl();
        String buyer = BUYER + "-union";
        acl.addUser(buyer);
        acl.addUserPermission(buyer, PRODUCER_A, DatasetACL.aclId.aiQuery);

        Dataset dataset = JenaDatasetFactory.newInstance("mem", "", acl);
        insertAs(dataset, DatasetACL.ADMIN_USER, PRODUCER_A);
        insertAs(dataset, DatasetACL.ADMIN_USER, PRODUCER_A_DRAFTS);

        // A pattern with no GRAPH clause at all: if the store is configured with
        // a union default graph this would be the way round the partition.
        int rows = countRows(dataset, buyer, "SELECT * WHERE { ?s ?p ?o }");
        System.out.println("=== SELECT * WHERE { ?s ?p ?o } as buyer -> " + rows + " row(s) ===");
        assertEquals(0, rows, "the default graph should not expose named-graph triples");
    }

    private SEPAAcl newEmptyAcl() throws Exception {
        ACLStorageOperations storage = ACLStorageFactory.newInstance(
                ACLStorage.ACLStorageId.asiDataset, new TreeMap<String, Object>());
        return SEPAAcl.newInstance(storage);
    }

    private void insertAs(Dataset dataset, String user, String graph) {
        try (RDFConnection connection = RDFConnectionFactory.connect(dataset, user)) {
            Txn.executeWrite(connection, () -> connection.update(
                    "INSERT DATA { GRAPH <" + graph + "> { <urn:s> <urn:p> <urn:o> } }"));
        }
    }

    private Set<String> graphsVisibleTo(Dataset dataset, String user) {
        Set<String> graphs = new TreeSet<>();
        try (RDFConnection connection = RDFConnectionFactory.connect(dataset, user)) {
            Txn.executeRead(connection, () -> connection.querySelect(
                    "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }",
                    row -> graphs.add(row.getResource("g").getURI())));
        }
        return graphs;
    }

    private int countRows(Dataset dataset, String user, String query) {
        try (RDFConnection connection = RDFConnectionFactory.connect(dataset, user)) {
            return Txn.calculateRead(connection, () -> {
                final int[] count = { 0 };
                connection.querySelect(query, row -> count[0]++);
                return count[0];
            });
        }
    }
}
