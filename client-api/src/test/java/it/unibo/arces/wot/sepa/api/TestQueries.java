package it.unibo.arces.wot.sepa.api;

public class TestQueries {
    //String concatenation it's really bad for the performance. But since this is a test I'd prefer code readability
    static final String SIMPLE_UPDATE = "prefix test:<http://www.vaimee.com/test#> " +
            "insert data {test:Sub test:Pred \"測試\"} ";

    static final String NOTIF_UPDATE = "prefix test:<http://www.vaimee.com/test#> " +
            "insert data {test:Sub test:hasNotification \"Hello there!\"} ";

    static final String SIMPLE_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
            "select ?s ?p ?o " +
            "where {?s ?p ?o}";

    static final String UTF8_RESULT_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
            "select ?s ?p ?o " +
            "where {test:Sub test:Pred ?o}";

    static final String NOTIF_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
            "select ?s ?p ?o " +
            "where {test:Sub test:hasNotification ?o}";
}
