package it.unibo.arces.wot.sepa.engine.processing;

public class UpdateConstruct {

    private final String deleteConstruct;
    private final String insertConstruct;

    UpdateConstruct(String deleteConstruct, String insertConstruct){
        if(deleteConstruct == null || insertConstruct == null){
            throw new IllegalArgumentException("Construct query cannot be null");
        }

        this.deleteConstruct = deleteConstruct;
        this.insertConstruct = insertConstruct;
    }

    /**
     * Get delete construct string. An empty string indicates that there are no deleted
     * triples
     * @return a construct sparql query string
     */
    public String getInsertConstruct() {
        return insertConstruct;
    }

    /**
     * Get delete construct string. An empty string indicates that there are no deleted
     * triples
     * @return a construct sparql query string
     */
    public String getDeleteConstruct() {
        return deleteConstruct;
    }
}
