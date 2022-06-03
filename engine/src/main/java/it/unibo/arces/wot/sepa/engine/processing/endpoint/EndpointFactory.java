/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_EX_JENA;
import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_REMOTE;
import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_STD_JENA;

/**
 *
 * @author Lorenzo
 */
public class EndpointFactory {
    public static SPARQLEndpoint newInstance(final String protocolSchema) {
        SPARQLEndpoint ret = null;
        switch(protocolSchema.toLowerCase().trim()) {
            default:
            case PROTOCOL_SCHEMA_STD_JENA:
                ret = new JenaInMemoryEndpoint();
                break;
            case PROTOCOL_SCHEMA_EX_JENA:
                ret = new SjenarEndpoint();
                break;
            case PROTOCOL_SCHEMA_REMOTE:
                ret = new RemoteEndpoint();
                break;
        }
        return ret;
    }
}
