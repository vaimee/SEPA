package it.unibo.arces.wot.sepa.engine.processing.endpoint;


import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import java.nio.charset.Charset;
import java.util.Random;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.Engine;


/*
 * NOTE: This is just a support class, which will create a huge quantity of quads
 * to study the DB size when it is written to the file system
 */
public class DataGenerator {


	private static int tripleSeed =0;
	private static final boolean exsternalSEPA = false;
	private static final int maxTripleForUpdate = 5000;

	private static final UpdateHTTPMethod updateMethod = UpdateHTTPMethod.POST;
	private static final QueryHTTPMethod queryMethod = QueryHTTPMethod.POST;
	private static final String scheme = "http";
	private static final String host = "localhost";
	private static final int port = 8000;
	private static final String updatePath ="/sparql" ;//"/update";
	private static final String queryPath ="/sparql" ;//"/query";

	private static final String sparqlCountQuad = 
			"SELECT (COUNT(?quad) AS ?count) WHERE  {"
					+ "	GRAPH ?g { \r\n"
					+ "		?s ?p ?o .\r\n"
					+ "	}\r\n"
					+ "BIND(CONCAT(STR(?g),CONCAT(STR(?s),CONCAT(STR(?p),STR(?o)))) AS ?quad)"
					+ "}";

	private static final String prefixs = 
			"PREFIX chat:<http://wot.arces.unibo.it/chat/>\r\n"
					+ "PREFIX schema:<http://schema.org/>\r\n"
					+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\r\n";

	private static SPARQL11Protocol client;


	public static void main(String[] args) throws Exception {
		if(!exsternalSEPA) {
			Engine engine =new Engine(new String[] {});
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		client=new SPARQL11Protocol(null);
		
		T1();
		T2();
		T3();
		T4();
		T5();
		T6();
		
		System.out.println("FINISH!!!!");
		
	}


	private static void T1() throws Exception{
		//GENERATE BIG GRAPH 1.000.000 triple
		generateGraphSafe(1,1000000);
	}
	
	private static void T2() throws Exception{
		//GENERATE MEDIUM GRAPH 1.000 triple
		generateGraphSafe(2,1000);
	}
	
	private static void T3() throws Exception{
		//GENERATE SMALL GRAPH 10 triple
		generateGraphSafe(3,10);
	}
	
	private static void T4() throws Exception{ 
		int seed=4;
		//GENERATE SMALL GRAPH 10 triple
		seed=generateGraphs(seed,1,10);
		//GENERATE MEDIUM GRAPH 1.000 triple
		seed=generateGraphs(seed,1,1000);
		//GENERATE BIG GRAPH 1.000.000 triple
		generateGraphSafe(seed,1000000);
	}
	
	private static void T5() throws Exception{
		//GENERATE 100 GRAPH each with 10000 triples
		for(int x=0;x<100;x++) {
			generateGraphSafe(x,10000);
		}
	}
	
	private static void T6() throws Exception{
		//GENERATE 100 GRAPH each with different count of triple
		//the first one has 100 triple
		//the 50° has 100*50=5000 triple
		//the last one has 100*100=10000 triple
		for(int x=0;x<100;x++) {
			generateGraphSafe(x,100*(x+1));
		}
	}

	public static void generateGraphSafe(int seed,int triplesCount) throws Exception {
		int actualTripleCount;
		int remainTripleCount;
		if(triplesCount>maxTripleForUpdate) {
			actualTripleCount=maxTripleForUpdate;
			remainTripleCount=actualTripleCount-triplesCount;
		}else {
			actualTripleCount=triplesCount;
			remainTripleCount=0;
		}
		String graphs ="\nGRAPH <http://sepa.test/g"+seed+"> {\n";
		for(int y=0;y<actualTripleCount;y++) {
			graphs+=generateRandomTriple()+"\n";
		}
		graphs+="}\n";
		String sparqlUpdate = "INSERT DATA {"+graphs+"}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		if(responseUpdate.isError()) {
			throw new Exception("INSERT DATA fail!");
		}
		if(remainTripleCount>0) {
			generateGraphSafe(seed,remainTripleCount);
		}
	}

	public static int generateGraphs(int seed,int graphsCount, int triplesCount) throws Exception{
		int _seed = seed;
		String graphs = "";
		for(int x=0;x<graphsCount;x++) {
			graphs+="\nGRAPH <http://sepa.test/g"+_seed+"> {\n";
			_seed++;
			for(int y=0;y<triplesCount;y++) {
				graphs+=generateRandomTriple()+"\n";
			}
			graphs+="}\n";
		}
		String sparqlUpdate = "INSERT DATA {"+graphs+"}";

		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		if(responseUpdate.isError()) {
			throw new Exception("INSERT DATA fail!");
		}
		return _seed;
	}
	
	private static String generateRandomTriple() {
		tripleSeed++;
		int randomSizeObj = new Random().nextInt(500)+1;
		if(randomSizeObj>100) {
			return "<http://"+generateRandomString(10)+"><http://"+generateRandomString(4)+"> \""+generateRandomString(randomSizeObj)+"\" .";
		}else {
			return "<http://"+generateRandomString(10)+"><http://"+generateRandomString(4)+"><http://"+tripleSeed+"/"+generateRandomString(randomSizeObj)+"> .";
		}
	
	}

	private static String generateRandomString(int length) {
		int leftLimit = 97; // numeral '0'-48
		int rightLimit = 122; // letter 'z'
		Random random = new Random();
		return  random.ints(leftLimit, rightLimit + 1)
				//.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(length)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}


	private static UpdateRequest generateUpdate(String sparql) {
		return new UpdateRequest(
				updateMethod,
				scheme,
				host,
				port,
				updatePath,
				sparql,
				null, 
				null,
				null
				);
	}




}



