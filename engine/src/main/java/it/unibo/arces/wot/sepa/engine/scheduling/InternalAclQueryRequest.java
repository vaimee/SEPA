/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import java.util.Set;

/**
 *
 * @author Lorenzo
 */
public class InternalAclQueryRequest extends InternalQueryRequest  {
	public InternalAclQueryRequest(
            String              sparql, 
            Set<String>         defaultGraphUri, 
            Set<String>         namedGraphUri,
            ClientAuthorization auth
        ) throws SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
	}

	public InternalAclQueryRequest(
                String              sparql, 
                Set<String>         defaultGraphUri, 
                Set<String>         namedGraphUri,
                ClientAuthorization auth, 
                String              mediaType
        ) throws SEPASparqlParsingException {
            super(sparql, defaultGraphUri, namedGraphUri, auth, mediaType);

		
	}
    
}
