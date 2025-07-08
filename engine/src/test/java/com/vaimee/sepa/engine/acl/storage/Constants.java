/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.acl.storage;

/**
 *
 * @author Lorenzo
 *
 *  *
 *  * USER     |  GRAPHS |
 *  *          |    G1   |    G2     |   G3    |     G4    |     G5     |     G6
 *  * monger   |     R   |           |         |           |      R     |
 *  * GROUP1   |    RW   |           |    R    |           |            |
 *  *
 *  * gonger   |         |    RW ID  |         |    R      |            |     R
 *  * GROUP2   |         |    RW     |         |           |            |
 */
class Constants {
     public static final String initQuery = "PREFIX sepaACL: <http://acl.sepa.com/>"    + System.lineSeparator() +   
                            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
                            "INSERT DATA { GRAPH sepaACL:acl { "                        + System.lineSeparator() + 
                            "   sepaACL:monger"                                         + System.lineSeparator() + 		
                            "       sepaACL:userName    \"monger\" ;"                   + System.lineSeparator() + 
                            "       sepaACL:memberOf	\"group1\" ;"                   + System.lineSeparator() + 
                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() + 
                            "           sepaACL:graphName   	mp:graph1;"             + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:update;"        + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:query"          + System.lineSeparator() + 
                            "       ]."                                                 + System.lineSeparator() + 
                            "   sepaACL:gonger"                                         + System.lineSeparator() + 
                            "       sepaACL:userName    \"gonger\" ;"                   + System.lineSeparator() + 
                            "       sepaACL:memberOf	\"group2\" ;"                   + System.lineSeparator() +              
                            "       sepaACL:accessInformation 	["                      + System.lineSeparator() + 
                            "           sepaACL:graphName   	mp:graph2;"             + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:update;"        + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:insertData;"    + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:deleteData;"    + System.lineSeparator() + 
                            "           sepaACL:allowedRight	sepaACL:query"          + System.lineSeparator() + 
                            "   ]"                                                      + System.lineSeparator() + 
                            "}}"                                                        + System.lineSeparator();
     
     public static final String initGroupsQuery = "PREFIX sepaACL: <http://acl.sepa.com/>"      + System.lineSeparator() + 
                            "PREFIX sepaACLGroups: <http://groups.acl.sepa.com/>"               + System.lineSeparator() + 
                            "PREFIX mp: <http://mysparql.com/>"                                 + System.lineSeparator() + 
                            ""                                                                  + System.lineSeparator() + 
                            "INSERT DATA { GRAPH sepaACL:aclGroups {"                           + System.lineSeparator() + 
                            "    sepaACLGroups:group1		 "                              + System.lineSeparator() + 
                            "    sepaACL:groupName    \"group1\" ; "                            + System.lineSeparator() + 
                            "    sepaACL:accessInformation 	["                              + System.lineSeparator() + 
                            "       sepaACL:graphName   	mp:graph1;"                     + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:update;"                + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:query  "                + System.lineSeparator() +       
                            "   ];"                                                             + System.lineSeparator() + 
                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() + 
                            "       sepaACL:graphName   	mp:graph3;"                     + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:query  "                + System.lineSeparator() +       
                            "   ]."                                                             + System.lineSeparator() + 
             
                            ""                                                                  + System.lineSeparator() + 
                            ""                                                                  + System.lineSeparator() + 
                            "   sepaACLGroups:group2"                                           + System.lineSeparator() + 
                            "   sepaACL:groupName    \"group2\" ; "                             + System.lineSeparator() + 
                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() + 
                            "       sepaACL:graphName   	mp:graph2;"                     + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:update;"                + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:query     "             + System.lineSeparator() + 
                            "   ];"                                                             + System.lineSeparator() + 
                            "   sepaACL:accessInformation 	["                              + System.lineSeparator() + 
                            "       sepaACL:graphName   	mp:graph4;"                     + System.lineSeparator() + 
                            "       sepaACL:allowedRight	sepaACL:query     "             + System.lineSeparator() + 
                            "   ]."                                                             + System.lineSeparator() + 
             
                            ""                                                                  + System.lineSeparator() + 
                            ""                                                                  + System.lineSeparator() + 
                            "}}";
     
    public static  final String USER1= "monger";
    public static  final String USER2= "gonger";
    public static  final String USER3= "deaduser";
    public static  final String GROUP1= "group1";
    public static  final String GROUP2= "group2";
    public static  final String GROUP3= "deadgroup";
    
    public static  final String NEWUSER = "newUser";
    public static  final String NEWGRAPH = "http://it.trivo.com/newGraph";
    public static  final String NEWGRAPH2 = "http://it.trivo.com/newGraph2";
    public static  final String NEWGROUP = "newGroup";
    
        
    public static  final String GRAPH1 = "mp:graph1";
    public static  final String GRAPH2 = "mp:graph2";
    public static  final String GRAPH3 = "mp:graph3";    
    public static  final String GRAPH4 = "mp:graph4";         
    public static  final String GRAPH5 = "mp:graph5";                   
    public static  final String GRAPH6 = "mp:graph6";             
    
}
