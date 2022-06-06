/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint.ar;

import it.unibo.arces.wot.sepa.commons.response.Response;
import java.util.Set;
import org.apache.jena.sparql.core.Quad;

/**
 *
 * @author Lorenzo
 */
public class UpdateResponseWithAR extends Response {
    public final Set<Quad>     updatedTuples;
    public final Set<Quad>     removedTuples;    
    public UpdateResponseWithAR(Set<Quad> removed, Set<Quad> updated) {
        updatedTuples = updated;
        removedTuples = removed;
    }
}