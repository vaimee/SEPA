package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * An update response with Added and Removed triples.
 */
public class UpdateResponseWithAR extends UpdateResponse {

    private final BindingsResults added;
    private final BindingsResults removed;

    public UpdateResponseWithAR(UpdateResponse ret, BindingsResults added, BindingsResults removed) {
        super(ret);
        if(added == null || removed == null){
            throw new IllegalArgumentException("Bindings cannot be null");
        }
        this.added = added;
        this.removed = removed;
    }

    public BindingsResults getRemoved() {
        return removed;
    }

    public BindingsResults getAdded() {
        return added;
    }
}
