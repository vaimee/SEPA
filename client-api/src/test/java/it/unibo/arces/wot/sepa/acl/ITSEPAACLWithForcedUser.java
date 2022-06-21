/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.acl;
import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Lorenzo
 */
public class ITSEPAACLWithForcedUser {
    private static ConfigurationProvider provider;
    private static Sync handler;

    private static final String queryACLADDGROUP_BASE = 
        "INSERT DATA { GRAPH sepaACL:aclGroups {"           +   System.lineSeparator()  +     
        "    sepaACLGroups:group1"                          +   System.lineSeparator()  +     		 
        "    sepaACL:groupName    \"group1\" ;"             +   System.lineSeparator()  +      
        "    sepaACL:accessInformation 	["                  +   System.lineSeparator()  +     
        "       sepaACL:graphName   	mp:graph1;"         +   System.lineSeparator()  +     
        "       sepaACL:allowedRight	sepaACL:update;"    +   System.lineSeparator()  +     
        "       sepaACL:allowedRight	sepaACL:query"      +   System.lineSeparator()  +        
        "   ];"                                             +   System.lineSeparator()  +     
        "   sepaACL:accessInformation 	["                  +   System.lineSeparator()  +                        
        "       sepaACL:graphName   	mp:graph3;"         +   System.lineSeparator()  +     
        "       sepaACL:allowedRight	sepaACL:query"      +   System.lineSeparator()  +     
        "   ]."                                             +   System.lineSeparator()  +   
        ""                                                  +   System.lineSeparator()  +   
        "}}";
            
    private static SPARQL11SEProtocol client;

	@BeforeEach
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {
		provider = new ConfigurationProvider();
		
		handler = new Sync();
		client = new SPARQL11SEProtocol(null,provider.getClientSecurityManager());
		
		Response ret = client.update(provider.buildUpdateRequest("ACL_DELETE_ALL"));
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@AfterEach
	public void endTest() throws IOException, InterruptedException, SEPAProtocolException {		
		client.close();
	}
        
        
        @Test
        public void testAddGroup() {
            try {
                final Response ret = client.update(provider.buildUpdateRequest("ACL_ADD_GROUP_1"));
                assertFalse(ret.isError(),String.valueOf(ret));
            } catch(Exception e )  {
                Assertions.fail(e);
            }
        }
        @Test
        public void testAddUser() {
            try {
                final Response ret1 = client.update(provider.buildUpdateRequest("ACL_ADD_GROUP_1"));
                assertFalse(ret1.isError(),String.valueOf(ret1));
                
                final Response ret2 = client.update(provider.buildUpdateRequest("ACL_ADD_USER_1"));
                assertFalse(ret2.isError(),String.valueOf(ret2));
                
            } catch(Exception e )  {
                Assertions.fail(e);
            }
        }
        
}
