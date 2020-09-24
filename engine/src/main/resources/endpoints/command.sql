DB.DBA.USER_CREATE ('sepatest', 'virtuoso');
DB.DBA.RDF_DEFAULT_USER_PERMS_SET ('nobody', 0);
DB.DBA.RDF_DEFAULT_USER_PERMS_SET ('sepatest', 0);
GRANT SPARQL_UPDATE TO "sepatest";
DB.DBA.RDF_GRAPH_USER_PERMS_SET ('http://sepatest', 'sepatest', 3);
