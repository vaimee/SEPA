package it.unibo.arces.wot.sepa.engine.processing;

import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.UpdateParserFactory;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.modify.request.*;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SPARQLAnalyzer {

    String test = null;

    public void setString(String s) {
        test = s;
    }

    class MyTransform extends TransformCopy
    {
        @Override
        public Op transform(OpBGP opBGP)
        {
            // create a new construct query
            Query q = QueryFactory.make();
            q.setQueryConstructType();

            // parse the bgp
            BasicPattern b = opBGP.getPattern();
            Iterator<Triple> opIterator = b.iterator();
            Template ttt = new Template(b);
            q.setConstructTemplate(ttt);
            ElementGroup body = new ElementGroup();
            ElementUnion union = new ElementUnion();

            while (opIterator.hasNext()){
                Triple bb = opIterator.next();

                // for the query
                ElementTriplesBlock block = new ElementTriplesBlock(); // Make a BGP
                block.addTriple(bb);
                body.addElement(block);
                logger.debug(bb.toString());

                // union
                union.addElement(block);

            }

            q.setQueryPattern(body);
            q.setQueryPattern(union);

            setString(q.toString());
            logger.debug(q.toString());

            return opBGP;
        }
    }

    class ToConstructUpdateVisitor extends UpdateVisitorBase{
        private UpdateConstruct result = new UpdateConstruct("","");
        @Override
        public void visit(UpdateDataInsert updateDataInsert) {
            Query  insertQuery = createBaseConstruct(new QuadAcc(updateDataInsert.getQuads()));
            String insertString = insertQuery.isUnknownType() ? "" : insertQuery.serialize() + "WHERE{}";
            result = new UpdateConstruct("",insertString);
        }



        @Override
        public void visit(UpdateDataDelete updateDataDelete) {
            Query deleteQuery = createBaseConstruct(new QuadAcc(updateDataDelete.getQuads()));
            String deleteString = deleteQuery.isUnknownType() ? "" : deleteQuery.serialize()+"WHERE{}";
            result = new UpdateConstruct(deleteString,"");
        }

        @Override
        public void visit(UpdateDeleteWhere updateDeleteWhere) {
            Query updateDeleteQuery = createBaseConstruct(new QuadAcc(updateDeleteWhere.getQuads()));
            if(!updateDeleteQuery.isUnknownType()) {
                ElementGroup where = new ElementGroup();
                for (Quad q : updateDeleteWhere.getQuads()) {
                    where.addTriplePattern(q.asTriple());
                }
                updateDeleteQuery.setQueryPattern(where);
                result = new UpdateConstruct(updateDeleteQuery.serialize(), "");
            }
        }

        @Override
        public void visit(UpdateModify updateModify) {
            String insertString = "";
            String deleteString = "";

            if(updateModify.hasDeleteClause() && !updateModify.getDeleteAcc().getQuads().isEmpty()){
                Template constructDelete = new Template(updateModify.getDeleteAcc());
                Query constructQueryDelete = new Query();
                constructQueryDelete.setQueryConstructType();
                constructQueryDelete.setConstructTemplate(constructDelete);
                constructQueryDelete.setQueryPattern(updateModify.getWherePattern());
                deleteString = constructQueryDelete.toString();
            }

            if(updateModify.hasInsertClause() && !updateModify.getInsertAcc().getQuads().isEmpty()){
                Template constructInsert = new Template(updateModify.getInsertAcc());
                Query constructQueryInsert = new Query();
                constructQueryInsert.setQueryConstructType();
                constructQueryInsert.setConstructTemplate(constructInsert);
                constructQueryInsert.setQueryPattern(updateModify.getWherePattern());
                insertString = constructQueryInsert.serialize();
            }

            result = new UpdateConstruct(deleteString,insertString);

        }

        @Override
        public void visit(UpdateClear update) {
            String deleteConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"+update.getGraph().getURI()+"> { ?s ?p ?o } . }";
            result = new UpdateConstruct(deleteConstruct,"");
        }

        @Override
        public void visit(UpdateDrop update) {
            String deleteConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"+update.getGraph().getURI()+"> { ?s ?p ?o } . }";
            result = new UpdateConstruct(deleteConstruct,"");
        }

        @Override
        public void visit(UpdateCopy update) {
            String deleteConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"+update.getDest().getGraph().getURI()+"> { ?s ?p ?o } . }";
            String insertConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"+update.getSrc().getGraph().getURI()+"> { ?s ?p ?o } . }";
            result = new UpdateConstruct(deleteConstruct,insertConstruct);
        }

        @Override
        public void visit(UpdateAdd update) {
            String insertConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"+update.getDest().getGraph().getURI()+"> { ?s ?p ?o } . }";
            result = new UpdateConstruct("",insertConstruct);
        }

        //TODO: Move

        public UpdateConstruct getResult() {
            return result;
        }

        private Query createBaseConstruct(  QuadAcc quads) {
            Query result = new Query();
            if(!quads.getQuads().isEmpty()){
                Template construct = new Template(quads);
                result = new Query();
                result.setQueryConstructType();
                result.setConstructTemplate(construct);
            }
            return result;
        }


    }

    // attributes
    private  String sparqlText;
    private final static Logger logger = LogManager.getLogger("SPARQLAnalyzer");

    // Constructor
    SPARQLAnalyzer(String request){
        // store the query text
        sparqlText = request;
    }


    UpdateConstruct getConstruct() {
        UpdateRequest updates = UpdateFactory.create(sparqlText);
        for(Update up : updates){
            ToConstructUpdateVisitor updateVisitor = new ToConstructUpdateVisitor();
            up.visit(updateVisitor);
            return updateVisitor.getResult();
        }
        throw new IllegalArgumentException("No valid operation found");
    }

    // LUTT generator
    List<TriplePath> getLutt(){

        // debug print
        logger.debug("Analyzing query " + sparqlText);

        // create a variable for the LUTT
        List<TriplePath> lutt = new ArrayList();

        // extract basic graph patterns
        Query q = QueryFactory.create(sparqlText);
        Element e = q.getQueryPattern();

        // This will walk through all parts of the query
        ElementWalker.walk(e,

                // For each element...
                new ElementVisitorBase() {

                    // ...when it's a block of triples...
                    public void visit(ElementPathBlock el) {

                        // ...go through all the triples...
                        Iterator<TriplePath> triples = el.patternElts();
                        while (triples.hasNext()) {

                            // get the current triple pattern
                            TriplePath t = triples.next();
                            lutt.add(t);

                            // debug print
                            logger.debug("Found Triple Pattern: " + t.getSubject() + " " + t.getPredicate() + " " + t.getObject());		            }
                    }
                }
        );

        // return!
        return lutt;
    }

    // Construct Generator
    String getConstructFromQuery() throws ParseException {

        // This method allows to derive the CONSTRUCT query
        // from the SPARQL SUBSCRIPTION

        // get the algebra from the query

        Query qqq = QueryFactory.create(sparqlText, Syntax.syntaxSPARQL);
        Op op = Algebra.compile(qqq);

        // get the algebra version of the construct query and
        // convert it back to query
        Transform transform = new MyTransform() ;
        op = Transformer.transform(transform, op) ;
        Query q = OpAsQuery.asQuery(op);
        // return
        return test;

    }

}