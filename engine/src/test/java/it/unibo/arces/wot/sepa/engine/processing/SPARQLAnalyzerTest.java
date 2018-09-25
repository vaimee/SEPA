package it.unibo.arces.wot.sepa.engine.processing;

import org.junit.Test;

import static org.junit.Assert.*;

public class SPARQLAnalyzerTest {
    private static final int DELETE_OUT = 1;
    private static final int INSERT_OUT = 2;
    private static final int INPUT = 0;

    private String [][] io = new String[][]{
            {"INSERT {<a://a> <a://b> <a://c>}WHERE{}","","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{}"},
            {"DELETE WHERE{?a <a://p> ?c}","CONSTRUCT{?a<a://p>?c.}WHERE{?a<a://p>?c}",""},
            {"CLEAR GRAPH <a://a>","CONSTRUCT{?s?p?o}WHERE{GRAPH<a://a>{?s?p?o}.}",""},
            {"DELETE {<a://a> <a://b> <a://c>}WHERE{?a ?b ?c}","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{?a?b?c}",""},
            {"DELETE {<a://a> <a://b> <a://c>} INSERT {<a://a> <a://b> <a://c>}WHERE{}","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{}","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{}"},
            {"DELETE {<a://a> <a://b> <a://c>} INSERT {<a://a> <a://b> <a://c>}WHERE{?a ?b ?c}","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{?a?b?c}","CONSTRUCT{<a://a><a://b><a://c>.}WHERE{?a?b?c}"},
            {"INSERT DATA{<a://a> <a://b> <a://c>. <a://e> <a://f> <a://g>}","","CONSTRUCT{<a://a><a://b><a://c>.<a://e><a://f><a://g>.}WHERE{}"},
            {"DELETE DATA{<a://a> <a://b> <a://c>. <a://e> <a://f> <a://g>}","CONSTRUCT{<a://a><a://b><a://c>.<a://e><a://f><a://g>.}WHERE{}",""},
    };

//TODO Test move copy add operations

    @Test
    public void setString() {
    }

    @Test
    public void getConstruct() {
        for(String[] test : io){
            SPARQLAnalyzer sparqlAnalyzer = new SPARQLAnalyzer(test[INPUT]);

            UpdateConstruct construct = sparqlAnalyzer.getConstruct();
            assertEquals(test[DELETE_OUT], construct.getDeleteConstruct().replaceAll("\\s+",""));

            assertEquals(test[INSERT_OUT], construct.getInsertConstruct().replaceAll("\\s+",""));
        }
    }

    @Test
    public void getLutt() {
    }

    @Test
    public void getConstructFromQuery() {
    }
}