/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.commons.response;

/**
 *
 * @author Lorenzo
 */
public class BaseClientResponse extends Response {

    @Override
    public boolean isUpdateResponse() {
        return this instanceof UpdateResponse;
    }
    
}
