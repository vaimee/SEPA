/* This class implements a JENA base in-memory RDF data set
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.engine.processing.endpoint;

import com.google.gson.*;
import com.vaimee.sepa.api.commons.request.QueryRequest;
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.*;
import com.vaimee.sepa.api.commons.sparql.*;
import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.dependability.acl.SEPAUserInfo;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.UpdateResult;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.system.Txn;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Special Graph Names
 * URI	Meaning
 * urn:x-arq:UnionGraph	The RDF merge of all the named graphs in the datasets of the query.
 * urn:x-arq:DefaultGraph	The default graph of the dataset, used when the default graph of the query is the union graph.
 */

public class JenaInMemoryEndpoint implements SPARQLEndpoint {
    private static Dataset dataset;
    private static final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);

    public JenaInMemoryEndpoint() {
        if (done.compareAndSet(false,true)) {
            JenaSystem.init();

            ARQ.init();

            Context c = ARQ.getContext();
            if (c == null) {
                throw new IllegalStateException("ARQ context is null: ARQ not initialized");
            }

            GeoSPARQLConfig.setupMemoryIndex();

            dataset = JenaDatasetFactory.newInstance(EngineBeans.getFirstDatasetMode(), EngineBeans.getFirstDatasetPath(), true);
        }
    }

    @Override
    public Response query(QueryRequest req, SEPAUserInfo usr) {
        AtomicReference<Response> response = new AtomicReference<>();

        try {
            final RDFConnection conn =
                    (usr != null && usr.userName != null && usr.userName.trim().length() > 0) ?
                            RDFConnectionFactory.connect(dataset, usr.userName) :
                            RDFConnectionFactory.connect(dataset);
            Txn.executeRead(conn, () -> {
                Query query = QueryFactory.create(req.getSPARQL());
                //TODO: Consider all query types (ASK, CONSTRUCT, ...)
                if (query.isSelectType()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ResultSet rs = conn.query(QueryFactory.create(req.getSPARQL())).execSelect();
                    ResultSetFormatter.outputAsJSON(out, rs);
                    try {
                        response.set(new QueryResponse(out.toString(StandardCharsets.UTF_8.name())));
                    } catch (UnsupportedEncodingException e) {
                        response.set(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode(), "Failed to query Jena in memory endpoint",e.getMessage()));
                    }
                } else if (query.isAskType()) {
                    boolean res = conn.queryAsk(query);
                    JsonObject json = new JsonObject();
                    json.add("boolean", new JsonPrimitive(res));
                    json.add("head", new JsonObject());
                    response.set(new QueryResponse(json.toString()));
                } else if (query.isConstructType()) {
                    Model res = conn.queryConstruct(query);
                    response.set(new QueryResponse(modelToBindingsResults(res).toString()));
                }
            });

            return response.get();
        }
        catch (Exception e) {
            return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode(), "Failed to query Jena in memory endpoint",e.getMessage());
        }
    }

    public BindingsResults modelToBindingsResults(Model res) {
        StringWriter sw = new StringWriter();
        res.write(sw, "RDF/JSON");

        JsonObject json;
        try {
            json = new Gson().fromJson(sw.toString(),JsonObject.class);
        } catch (JsonParseException e) {
            json = null;
        }

        ArrayList<String> vars = new ArrayList<>();
        vars.add("subject");
        vars.add("predicate");
        vars.add("object");

        ArrayList<Bindings> bindings = new ArrayList<>();

        assert json != null;

        for (String subject : json.keySet()) {
            RDFTerm s;
            try{
                s = new RDFTermURI(subject);
            }catch (Exception e) {
                s = new RDFTermBNode(subject);
            }

            JsonObject predicates = json.getAsJsonObject(subject);
            for (String predicate : predicates.keySet()) {
                JsonArray objects =  predicates.getAsJsonArray(predicate);
                for (JsonElement obj : objects) {
                    String type = obj.getAsJsonObject().getAsJsonPrimitive("type").getAsString();
                    String value = obj.getAsJsonObject().getAsJsonPrimitive("value").getAsString();

                    Bindings b = new Bindings();
                    b.addBinding("subject",s);
                    b.addBinding("predicate",new RDFTermURI(predicate));

                    if(type.equals("uri")) b.addBinding("object",new RDFTermURI(value));
                    else if(type.equals("bnode")) b.addBinding("object",new RDFTermBNode(value));
                    else {
                        String datatype = (obj.getAsJsonObject().has("datatype") ? obj.getAsJsonObject().getAsJsonPrimitive("datatype").getAsString() : null);
                        String lang = (obj.getAsJsonObject().has("lang") ? obj.getAsJsonObject().getAsJsonPrimitive("lang").getAsString() : null);
                        b.addBinding("object",new RDFTermLiteral(value,datatype,lang));
                    }

                    bindings.add(b);
                }
            }
        }

        return new BindingsResults(vars,bindings);
    }

    @Override
    public Response update(UpdateRequest req, SEPAUserInfo usr) {
        try (final RDFConnection conn =
                     (usr != null && usr.userName != null && usr.userName.trim().length() > 0) ?
                             RDFConnectionFactory.connect(dataset, usr.userName) :
                             RDFConnectionFactory.connect(dataset);
        ) {
            final Set<Quad> added = new TreeSet<>(new QuadComparator());
            final Set<Quad> removed = new TreeSet<>(new QuadComparator());
            Txn.executeWrite(conn, () -> {
                final List<UpdateResult> lur = conn.update(req.getSPARQL());
                if (lur != null) {
                    for (final UpdateResult ur : lur) {
                        if (ur.deletedTuples != null) {
                            for (final Quad q : ur.deletedTuples) {
                                removed.add(q);
                            }
                        }

                        if (ur.addedTuples != null) {
                            for (final Quad q : ur.addedTuples) {
                                added.add(q);
                            }
                        }

                    }

                }

            });

            return new UpdateResponseWithAR(added, removed);
        }
    }

    private class QuadComparator implements Comparator<Quad> {

        @Override
        public int compare(Quad o1, Quad o2) {
            return o1.toString().compareTo(o2.toString());
        }

    }

}
