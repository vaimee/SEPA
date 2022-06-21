/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.QueryResponse;

/**
 *
 * @author Lorenzo
 */
interface  QueryResponseValidator {
    boolean validate(QueryResponse resp);
}
