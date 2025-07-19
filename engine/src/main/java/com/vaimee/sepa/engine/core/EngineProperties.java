/* This class implements the configuration properties of the Semantic Event Processing Architecture (SEPA) Engine
 * 
 * Authors:	Luca Roffia (luca.roffia@unibo.it)
 * 			Andrea Bisacchi (andrea.bisacchi5@studio.unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.vaimee.sepa.engine.core;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import javax.net.ssl.SSLContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.RSAKey;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.QueryProperties.QueryHTTPMethod;
import com.vaimee.sepa.api.commons.properties.QueryProperties.QueryResultsFormat;
import com.vaimee.sepa.api.commons.properties.SPARQL11Properties;
import com.vaimee.sepa.api.commons.properties.SPARQL11Properties.ProtocolScheme;
import com.vaimee.sepa.api.commons.properties.UpdateProperties.UpdateHTTPMethod;
import com.vaimee.sepa.api.commons.properties.UpdateProperties.UpdateResultsFormat;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.dependability.authorization.IsqlProperties;
import com.vaimee.sepa.engine.dependability.authorization.JKSUtil;
import com.vaimee.sepa.engine.dependability.authorization.LdapProperties;
import com.vaimee.sepa.logging.Logging;

/**
 * <pre>
 * 
 * ENGINE CONFIGURATION FILE
{
	"parameters": {
		"scheduler": {
			"queueSize": 100,
			"timeout": 3000
		},
		"processor": {
			"updateTimeout": 5000,
			"queryTimeout": 5000,
			"maxConcurrentRequests": 5
		},
		"spu": {
			"timeout": 2000
		},
		"gates": {
			"security": {
				"enabled" : true,
				"type" : "local|ldap|keycloak",
				"tls" : false
			},
			"ports": {
				"http": 8000,
				"ws": 9000
			},
			"paths": {
				"update": "/update",
				"query": "/query",
				"subscribe": "/subscribe",
				"register": "/oauth/register",
				"tokenRequest": "/oauth/token"
			}
		}
	}
}

ENDPOINT CONFIGURATION FILE

{
	"defaultsFileName": "endpoint.jpar",
	"propertiesFile": "endpoint.jpar",
	"host": "in-memory",
	"sparql11protocol": {
		"protocol": "jena_api",
		"port": 8000,
		"query": {
			"path": "/sparql",
			"method": "URL_ENCODED_POST",
			"format": "JSON"
		},
		"update": {
			"path": "/sparql",
			"method": "URL_ENCODED_POST",
			"format": "JSON"
		}
	},
	"graphs": {
		"default_graph_uri": [],
		"named_graph_uri": [],
		"using_graph_uri": [],
		"using_named_graph_uri": []
	}
}
 * </pre>
 */
public class EngineProperties {

//	private static final transient String defaultsFileName = "engine.jpar";

	static private class Parameters {
		public Scheduler scheduler = new Scheduler();
		public Processor processor = new Processor();
		public Spu spu = new Spu();
		public Gates gates = new Gates();

		public Acl   acl = new Acl();
		public DatasetConfiguration dsConfig = new DatasetConfiguration();

		public String toString() {
			return new Gson().toJson(this);
		}
		
		public void storeProperties(String propertiesFile) throws IOException {
			FileWriter out = new FileWriter(propertiesFile);
			out.write(this.toString());
			out.close();
		}
	}

	static private class Scheduler {
		public int queueSize;
		public int timeout;

		public Scheduler(){
			queueSize = 1000;
			timeout = 5000;
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Processor {
		public int updateTimeout;
		public int queryTimeout;
		public int maxConcurrentRequests;
		public boolean reliableUpdate;

		public Processor(){
			reliableUpdate = true;
			updateTimeout = 5000;
			queryTimeout = 30000;
			maxConcurrentRequests = 5;
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Spu {
		public int timeout;

		public Spu(){
			timeout = 5000;
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}
	
	static private class Security {
		public boolean tls;
		public boolean enabled;
		public String type;
		
		public Security(){
			enabled = false;
			type = "local";
			tls = false;
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Gates {
		public Security security = new Security();
		public Paths paths = new Paths();
		public Ports ports = new Ports();
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Paths {
		public String update;
		public String query;
		public String subscribe;
		public String unsubscribe;
		public String register;
		public String tokenRequest;
		JsonObject extensions = new JsonObject();

		public Paths(){
			update       = "/update";
			query        = "/query";
			subscribe    = "/subscribe";
			unsubscribe  = "/unsubscribe";
			register     = "/oauth/register";
			tokenRequest = "/oauth/token";
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Ports {
		public int http;
		public int ws;

		public Ports(){
			http  = 8000;
			ws    = 9000;
		}
		
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	static private class Acl {
		public  boolean     enabled     =   false;
		public  String      type        =   ACL_TYPE_DS;  //allowed: dataset/json
		public  String      mode        =   DS_MODE_MEM;     /*
                                                                    Valid values depends on type :
		 *) type == dataset
                                                                        -) mode = tdb2  tdb2 persistency
                                                                        -) mode = tdb1  tdb1 persistency
                                                                        -) mode = mem   not persistent
		 *) type == json : no value required

		 */
		public String       path = "./acl";             /*
                                                                    Valid values depends on type :
		 *) type == dataset/tdb1|tdb2
                                                                        -) path = path of tdbx persistent data

                                                                    *) type == json : full path of json file

                                                                */


		public String       queryPath       = "/acl/query/";
		public String       updatePath      = "/acl/update/";

	}

	public boolean  isAclEnabled() {
		return parameters.acl.enabled;
	}

	public void setAclEnabled(boolean enable) {
		parameters.acl.enabled=enable;
	}

	public String getAclType() {
		return parameters.acl.type;
	}

	public String getAclMode() {
		return parameters.acl.mode;
	}

	public String getAclPath() {
		return parameters.acl.path;
	}


	public String getAclQueryPath() {
		return parameters.acl.queryPath;
	}
	public String getAclUpdatePath() {
		return parameters.acl.updatePath;
	}

	private static class    DatasetData {
		public  String      mode        =   DS_MODE_MEM ;     /*
                                                                    -) mode = tdb2  tdb2 persistency
                                                                    -) mode = tdb1  tdb1 persistency
                                                                    -) mode = mem   not persistent
		 */
		public String       path        =   "";         //path of dataset is mode is tdb1 or tdb2
	}
	private static class    DatasetConfiguration {
		public boolean          lutt = false;
		public DatasetData      firstDS  = new DatasetData();
		public DatasetData      secondDS = new DatasetData();


	}

	public static final String    DS_MODE_MEM     =   "mem";
	public static final String    DS_MODE_TDB2    =   "tdb2";
	public static final String    DS_MODE_TDB1    =   "tdb1";

	public static final String    ACL_TYPE_DS     =   "dataset";
	public static final String    ACL_TYPE_JSON   =   "json";

	public boolean isLUTTEnabled() {
		return parameters.dsConfig.lutt;
	}

	public void setLUTTEnabled(boolean enable) {
		parameters.dsConfig.lutt=enable;
	}


	public String   getFirstDatasetMode() {
		return parameters.dsConfig.firstDS.mode;
	}
	public String   getFirstDatasetPath() {
		return parameters.dsConfig.firstDS.path;
	}
	public String   getSecondDatasetMode() {
		return parameters.dsConfig.secondDS.mode;
	}
	public String   getSecondDatasetPath() {
		return parameters.dsConfig.secondDS.path;
	}

	public void setAclPath(String s ) {
		parameters.acl.path = s;
	}

	public void setAclMode(String s) {
		parameters.acl.mode = s;
	}
	
	private int wsShutdownTimeout = 5000;

	// Properties files
	private String engineJpar = null; // "engine.jpar";
	private String endpointJpar = null; // "endpoint.jpar";

	// Secure option
	private Optional<Boolean> secure = Optional.empty();

	// JKS defaults
	private String sslStoreName = "sepa.jks";
	private String sslStorePass = "sepa2020";

	private String jwtKeyAlias = "jwt";
	private String jwtKeyStore = "sepa.jks";
	private String jwtKeyStorePass = "sepa2020";
	private String jwtKeyAliasPass = "sepa2020";

	// CA defaults (using PEM certificate provided by Let's Encrypt or a key within
	// the JKS)
	private String caCertificate = null;
	private String caPath = null;
	private String caPassword = null;

	// LDAP
	private String ldapHost = "localhost";
	private int ldapPort = 10389;
	private String ldapDn = "dc=example,dc=com";
	private String ldapUsersDn = null;
	private String ldapUser = null;
	private String ldapPwd = null;

	// Virtuoso LDAP sync
	private String isqlPath = "/usr/local/virtuoso-opensource/bin/";
	private String isqlHost = "localhost";
	private String isqlUser = "dba";
	private String isqlPass = "dba";
	private int isqlPort = 1111;

	// Security management
	SSLContext ssl = null;
	RSAKey jwt = null;
	LdapProperties ldap = null;
	IsqlProperties isql = null;
	
	private SPARQL11Properties endpointProperties;
	private Parameters parameters;
	
	public EngineProperties(String[] args) throws SEPASecurityException, SEPAPropertiesException {
		
		setJparFiles(args);

		try {
			if (engineJpar != null) {
				Logging.getLogger().info("Reading from file: "+engineJpar);
				parameters = new Gson().fromJson(new FileReader(engineJpar), Parameters.class);
			}
			else {
				Logging.getLogger().info("Reading from file: engine.jpar");
				parameters = new Gson().fromJson(new FileReader("engine.jpar"), Parameters.class);
			}
		} catch (Exception e) {
			Logging.getLogger().info("Engine parameters: using defaults");
			parameters = new Parameters();
		}
		
		try {
			if (endpointJpar != null) {
				Logging.getLogger().info("Reading from file: "+endpointJpar);
				endpointProperties = new SPARQL11Properties(URI.create(endpointJpar)); 
			}
			else {
				Logging.getLogger().info("Reading from file: endpoint.jpar");
				endpointProperties = new SPARQL11Properties(URI.create("endpoint.jpar")); 
			}
		} catch (SEPAPropertiesException  e) {
			Logging.getLogger().warn(e.getMessage());
			Logging.getLogger().info("Endpoint parameters: using defaults");
			endpointProperties = new SPARQL11Properties();
		}
		
		parsingArgument(args);
			
		setSecurity();
		
		printParameters();
	}
	
	public SPARQL11Properties getEndpointProperties() {
		return endpointProperties;
	}
	public int getWsShutdownTimeout(){
		return wsShutdownTimeout;
	}
	
	
	public String getSSLCertificate() {
		if (!isSecure())
			return "Security off";
		return jwt.getParsedX509CertChain().get(0).getSubjectX500Principal().getName() + " "
				+ jwt.getParsedX509CertChain().get(0).getNotAfter().toString();
	}

	public void refreshSSLCertificate() {
		try {
			setSecurity();
		} catch (SEPASecurityException e) {
			Logging.getLogger().error(e.getMessage());
		}
	}
	
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("java [JMX] [JVM] [LOG4J] -jar SEPAEngine_X.Y.Z.jar [-help] [-secure=true] [-engine=engine.jpar] [-endpoint=endpoint.jpar] [JKS OPTIONS] [LDAP OPTIONS] [ISQL OPTIONS]");
		System.out.println("Options: ");
		System.out.println("-secure : overwrite the current secure option of engine.jpar");
		System.out.println("-engine : can be used to specify the JSON configuration parameters for the engine (default: engine.jpar)");
		System.out.println("-endpoint : can be used to specify the JSON configuration parameters for the endpoint (default: endpoint.jpar)");
		System.out.println("-help : to print this help");

		System.out.println("");
		System.out.println("JMX:");
		System.out.println("-Dcom.sun.management.config.file=jmx.properties : to enable JMX remote managment");
		System.out.println("");

		System.out.println("JVM:");
		System.out.println("-XX:+UseG1GC");
		System.out.println("");

		System.out.println("LOG4J");
		System.out.println("-Dlog4j.configurationFile=path/to/log4j2.xml");
		System.out.println("");

		System.out.println("JKS OPTIONS:");
		System.out.println("-sslstore <jks> : JKS for SSL CA      			(default: ssl.jks)");
		System.out.println("-sslpass <pwd> : password of the JKS        	(default: sepastore)");
		System.out.println("-jwtstore <jks> : JKS for the JWT key       	(default: jwt.jks)");
		System.out.println("-jwtalias <alias> : alias for the JWT key   	(default: jwt)");
		System.out.println("-jwtstorepass <pwd> : password for the JKS  	(default: sepakey)");
		System.out.println("-jwtaliaspass <pwd> : password for the JWT key  (default: sepakey)");

		System.out.println("LDAP OPTIONS:");
		System.out.println("-ldaphost <name> : host     		         (default: localhost)");
		System.out.println("-ldapport <port> : port                      (default: 10389)");
		System.out.println("-ldapdn <dn> : domain                        (default: dc=sepatest,dc=com)");
		System.out.println("-ldapusersdn <dn> : domain                   (default: null)");
		System.out.println("-ldapuser <usr> : username                   (default: null)");
		System.out.println("-ldappwd <pwd> : password                    (default: null)");

		System.out.println("ISQL OPTIONS:");
		System.out.println("-isqlpath <path> : location of isql     		 (default: /usr/local/virtuoso-opensource/bin/)");
		System.out.println("-isqlhost <host> : host of Virtuoso     		 (default: localhost)");
		System.out.println("-isqluser <user> : user of Virtuoso     		 (default: dba)");
		System.out.println("-isqlpass <pass> : password of Virtuoso     	 (default: dba)");
		
		System.out.println("ENVIRONMENTAL VARIABLES (override other settings):");
		System.out.println("All the options, JKS, LDAP, ISQL can be override (without leading -, e.g. secure, not -secure)");
		
		System.out.println("parameters.gates.ports.http : specify the UDPATE/QUERY port                       (default: 8000)");
		System.out.println("parameters.gates.ports.ws : specify the SUBSCRIBE port                            (default: 9000)");
		System.out.println("parameters.scheduler.queuesize : the size of the scheduler FIFO queue             (default: 1000)");
		System.out.println("parameters.scheduler.timeout : the timeout of scheduling new request (ms)         (default: 5000)");
		System.out.println("parameters.processor.updatetimeout : the timeout of updates (ms)                  (default: 5000)");  
		System.out.println("parameters.processor.querytimeout : the timeout of queries (ms)                   (default: 30000)");
		System.out.println("parameters.gates.paths.update : the path of update HTTP requests                  (default: /update)");
		System.out.println("parameters.gates.paths.query : the path of query HTTP requests                    (default: /query)");
		System.out.println("parameters.gates.paths.subscribe: the path of subscribe websocket requests        (default: /subscribe)");
		System.out.println("parameters.gates.paths.unsubscribe: the path of unsubscribe websocket requests    (default: /unsubscribe)");
		
		System.out.println("host : the host URL (as default uses JENA in memory)                            (default: in-memory)");
		System.out.println("sparql11protocol.protocol : the host protocol (as default uses JENA in memory)  (default: jena-api)");
		System.out.println("sparql11protocol.port : the host HTTP port for update and query                 (default: 8000)");
		System.out.println("sparql11protocol.query.path : the query path                                    (default: /sparql)");
		System.out.println("sparql11protocol.query.method : the query method (GET,POST,URL_ENCODED_POST)    (default: URL_ENCODED_POST)");
		System.out.println("sparql11protocol.query.format : the query results format (JSON,CSV)             (default: JSON)");
		System.out.println("sparql11protocol.update.path : the update path                                  (default: /sparql)");
		System.out.println("sparql11protocol.update.method : the update method (POST,URL_ENCODED_POST)      (default: URL_ENCODED_POST)");
		System.out.println("sparql11protocol.update.format : the query results format (JSON,HTML)           (default: JSON)");

	}
	
	private void setParameter(String key,String value) {
		switch (key.toLowerCase()) {
		case "-capwd":
			caPassword = value;
			break;
		case "-cacertificate":
			caCertificate = value;
			break;
		case "-capath":
			caPath = value;
			break;

		case "-sslstore":
			sslStoreName = value;
			break;
		case "-sslpass":
			sslStorePass = value;
			break;
		case "-jwtalias":
			jwtKeyAlias = value;
			break;
		case "-jwtstore":
			jwtKeyStore = value;
			break;
		case "-jwtstorepass":
			jwtKeyStorePass = value;
		case "-jwtaliaspass":
			jwtKeyAliasPass = value;
			break;
			
		case "-secure":
			secure = Optional.of(Boolean.parseBoolean(value));
			break;

		case "-ldaphost":
			ldapHost = value;
			break;
		case "-ldapport":
			ldapPort = Integer.parseInt(value);
			break;
		case "-ldapdn":
			ldapDn = value;
			break;
		case "-ldapusersdn":
			ldapUsersDn = value;
			break;
		case "-ldapuser":
			ldapUser = value;
			break;
		case "-ldappwd":
			ldapPwd = value;
			break;

		case "-isqlpath":
			isqlPath = value;
			break;
		case "-isqlhost":
			isqlHost = value;
			break;
		case "-isqluser":
			isqlUser = value;
			break;
		case "-isqlpass":
			isqlPass = value;
			break;
		case "-isqlport":
			isqlPort = Integer.parseInt(value);
			break;
			
		case "-parameters.gates.ports.http":
			parameters.gates.ports.http = Integer.parseInt(value);
			break;
		case "-parameters.gates.ports.ws":
			parameters.gates.ports.ws = Integer.parseInt(value);
			break;
		case "-parameters.scheduler.queuesize":
			parameters.scheduler.queueSize = Integer.parseInt(value);
			break;
		case "-parameters.scheduler.timeout":
			parameters.scheduler.timeout = Integer.parseInt(value);
			break;
		case "-parameters.processor.updatetimeout":
			parameters.processor.updateTimeout = Integer.parseInt(value);
			break;
		case "-parameters.processor.querytimeout":
			parameters.processor.queryTimeout = Integer.parseInt(value);
			break;
		case "-parameters.gates.paths.update":
			parameters.gates.paths.update = value;
			break;
		case "-parameters.gates.paths.query":
			parameters.gates.paths.query = value;
			break;
		case "-parameters.gates.paths.subscribe":
			parameters.gates.paths.subscribe = value;
			break;
		case "-parameters.gates.paths.unsubscribe":
			parameters.gates.paths.unsubscribe = value;
			break;
		
		case "-host":
			endpointProperties.setHost(value);
			break;
		case "-engine":
			engineJpar = value;
			break;
		case "-endpoint":
			endpointJpar = value;
			break;
		
		case "-sparql11protocol.protocol":
			if (value.toLowerCase().equals("http")) endpointProperties.setProtocolScheme(ProtocolScheme.http);
			else if (value.toLowerCase().equals("https")) endpointProperties.setProtocolScheme(ProtocolScheme.https);
			else endpointProperties.setProtocolScheme(ProtocolScheme.jena_api);
			break;
		case "-sparql11protocol.port":
			endpointProperties.setPort(Integer.parseInt(value));
			break;
		case "-sparql11protocol.query.path":
			endpointProperties.setQueryPath(value);
			break;
		case "-sparql11protocol.query.method":
			if (value.toLowerCase().equals("get")) endpointProperties.setQueryMethod(QueryHTTPMethod.GET);
			else if (value.toLowerCase().equals("post")) endpointProperties.setQueryMethod(QueryHTTPMethod.POST);
			else endpointProperties.setQueryMethod(QueryHTTPMethod.URL_ENCODED_POST);
			break;
		case "-sparql11protocol.query.format":
			if (value.toLowerCase().equals("json")) endpointProperties.setQueryAcceptHeader(QueryResultsFormat.JSON);
			else if (value.toLowerCase().equals("csv")) endpointProperties.setQueryAcceptHeader(QueryResultsFormat.CSV);
			else endpointProperties.setQueryAcceptHeader(QueryResultsFormat.XML);
			break;
		case "-sparql11protocol.update.path":
			endpointProperties.setUpdatePath(value);
			break;
		case "-sparql11protocol.update.method":
			if (value.toLowerCase().equals("post")) endpointProperties.setUpdateMethod(UpdateHTTPMethod.POST);
			else endpointProperties.setUpdateMethod(UpdateHTTPMethod.URL_ENCODED_POST);
			break;
		case "-sparql11protocol.update.format":
			if (value.toLowerCase().equals("json")) endpointProperties.setUpdateAcceptHeader(UpdateResultsFormat.JSON);
			else endpointProperties.setUpdateAcceptHeader(UpdateResultsFormat.HTML);
			break;
		default:
			break;
		}	
	}
	
	private void setJparFiles(String args[]) {
		for (int i = 0; i < args.length; i = i + 2) {
			if (args[i].equals("-engine")) {
				setParameter(args[i], args[i+1]);
				continue;
			}
			if (args[i].equals("-endpoint")) {
				setParameter(args[i], args[i+1]);
			}
		}
		Map<String, String> envs = System.getenv();
		for(String var : envs.keySet()) {			
			
			if (var.equals("engine")) setParameter("-"+var, envs.get(var));
			else if (var.equals("endpoint")) setParameter("-"+var, envs.get(var));
		}

	}

	private void parsingArgument(String[] args) throws PatternSyntaxException {
		for (int i = 0; i < args.length; i = i + 2) {
			if (args[i].equals("-help")) {
				printUsage();
				return;
			}
			Logging.getLogger().trace("Program arguments "+args[i]+" : "+ args[i+1]);
			setParameter(args[i], args[i+1]);
		}
		
		// Environmental variables (overrides)
		Map<String, String> envs = System.getenv();
		for(String var : envs.keySet()) {			
			Logging.getLogger().trace("Environmental variable "+var+" : "+envs.get(var));
			setParameter("-"+var, envs.get(var));
			setParameter("-"+var.replace("_", "."), envs.get(var));
		}
	}
	
	private void printParameters() {
		Logging.getLogger().trace("--- SSL ---");
		Logging.getLogger().trace("-cacertificate: " + caCertificate);
		Logging.getLogger().trace("-capwd: " + caPassword);
		Logging.getLogger().trace("-capath: " + caPath);
		Logging.getLogger().trace("-sslstore: " + sslStoreName);
		Logging.getLogger().trace("-sslpass: " + sslStorePass);

		Logging.getLogger().trace("--- JWT ---");
		Logging.getLogger().trace("-jwtstore: " + jwtKeyStore);
		Logging.getLogger().trace("-jwtpass: " + jwtKeyStorePass);
		Logging.getLogger().trace("-jwtalias: " + jwtKeyAlias);
		Logging.getLogger().trace("-jwtaliaspass: " + jwtKeyAliasPass);

		Logging.getLogger().trace("--- LDAP ---");
		Logging.getLogger().trace("-ldaphost: " + ldapHost);
		Logging.getLogger().trace("-ldapport: " + ldapPort);
		Logging.getLogger().trace("-ldapdn: " + ldapDn);
		Logging.getLogger().trace("-ldapusersdn: " + ldapUsersDn);
		Logging.getLogger().trace("-ldapuser: " + ldapUser);
		Logging.getLogger().trace("-ldappwd: " + ldapPwd);

		Logging.getLogger().trace("--- ISQL ---");
		Logging.getLogger().trace("-isqlpath: " + isqlPath);
		Logging.getLogger().trace("-isqlhost: " + isqlHost);
		Logging.getLogger().trace("-isqlport: " + isqlPort);
		Logging.getLogger().trace("-isqluser: " + isqlUser);
		Logging.getLogger().trace("-isqlpass: " + isqlPass);
		
		Logging.getLogger().debug("--- Engine/endpoint ---");
		Logging.getLogger().debug("-engine: " + engineJpar);
		Logging.getLogger().debug("-endpoint: " + endpointJpar);
		Logging.getLogger().debug("-secure: " + secure);
		
		Logging.getLogger().debug("--- ENGINE PARAMETERS ---");
		Logging.getLogger().debug("-parameters.gates.ports.http: " + parameters.gates.ports.http);
		Logging.getLogger().debug("-parameters.gates.ports.ws: " + parameters.gates.ports.ws);
		Logging.getLogger().debug("-parameters.scheduler.queuesize: " + parameters.scheduler.queueSize);
		Logging.getLogger().debug("-parameters.scheduler.timeout: " + parameters.scheduler.timeout);
		Logging.getLogger().debug("-parameters.processor.updatetimeout: " + parameters.processor.updateTimeout);
		Logging.getLogger().debug("-parameters.processor.querytimeout: " + parameters.processor.queryTimeout);
		Logging.getLogger().debug("-parameters.gates.paths.update: " + parameters.gates.paths.update);
		Logging.getLogger().debug("-parameters.gates.paths.query: " + parameters.gates.paths.query);
		Logging.getLogger().debug("-parameters.gates.paths.subscribe: " + parameters.gates.paths.subscribe);
		Logging.getLogger().debug("-parameters.gates.paths.unsubscribe: " + parameters.gates.paths.unsubscribe);
		
		Logging.getLogger().debug("--- ENDPOINT PARAMETERS---");
		Logging.getLogger().debug("-host: " + endpointProperties.getHost());
		Logging.getLogger().debug("-sparql11protocol.protocol: " + endpointProperties.getProtocolScheme());
		Logging.getLogger().debug("-sparql11protocol.port: " + endpointProperties.getPort());
		Logging.getLogger().debug("-sparql11protocol.query.path: " + endpointProperties.getQueryPath());
		Logging.getLogger().debug("-sparql11protocol.query.method: " + endpointProperties.getQueryMethod());
		Logging.getLogger().debug("-sparql11protocol.query.format: " + endpointProperties.getQueryAcceptHeader());
		Logging.getLogger().debug("-sparql11protocol.update.path: " + endpointProperties.getUpdatePath());
		Logging.getLogger().debug("-sparql11protocol.update.method: " + endpointProperties.getUpdateMethod());
		Logging.getLogger().debug("-sparql11protocol.update.format: " + endpointProperties.getUpdateAcceptHeader());

		Logging.getLogger().debug("--- EXTENSIONS ---");
		for (String ext : getExtensions().keySet()) {
			Logging.getLogger().debug(ext+" : " + getExtensions().get(ext));
		}
	}

	public HashMap<String,String> getExtensions() {
		HashMap<String,String> ret = new HashMap<String,String>();
		for (String key :  this.parameters.gates.paths.extensions.keySet()) {
			ret.put(key,this.parameters.gates.paths.extensions.get(key).getAsString());
		}
		return ret;
	}
	public void setSecurity() throws SEPASecurityException {
		parameters.gates.security.enabled = (secure.isEmpty() ? false : secure.get());
		
		// OAUTH 2.0 Authorization Manager
		if (isSecure()) {
			ssl = JKSUtil.getSSLContext(sslStoreName, sslStorePass);
			jwt = JKSUtil.getRSAKey(jwtKeyStore, jwtKeyStorePass, jwtKeyAlias, jwtKeyAliasPass);
			ldap = new LdapProperties(ldapHost, ldapPort, ldapDn, ldapUsersDn, ldapUser, ldapPwd, isTls());
			isql = new IsqlProperties(isqlPath, isqlHost, isqlPort, isqlUser, isqlPass);
			if (isLocalEnabled())
				Dependability.enableLocalSecurity(ssl, jwt);
			else if (isLDAPEnabled()) {
				Dependability.enableLDAPSecurity(ssl, jwt, ldap);
			} else if (isKeycìCloakEnabled()) {
				Dependability.enableKeyCloakSecurity(ssl, jwt, ldap, isql);
			}

			// Check that SSL has been properly configured
			Dependability.getSSLContext();
		}
	}

	public void store(EngineProperties properties, String propertiesFile) throws IOException {
		parameters.storeProperties(propertiesFile);
	}

	public String toString() {
		return new Gson().toJson(this);
	}
	

	public boolean isSecure() {
		return this.parameters.gates.security.enabled;
	}
	
	public boolean isTls() {
		return this.parameters.gates.security.tls;
	}
	
	public boolean isLDAPEnabled() {
		return this.parameters.gates.security.type.equals("ldap");
	}
	
	public boolean isLocalEnabled() {
		return this.parameters.gates.security.type.equals("local");
	}
	
	public boolean isKeycìCloakEnabled() {
		return this.parameters.gates.security.type.equals("keycloak");
	}
	
	public int getMaxConcurrentRequests() {
		return this.parameters.processor.maxConcurrentRequests;
	}

	public int getUpdateTimeout() {
		return this.parameters.processor.updateTimeout;
	}

	public int getQueryTimeout() {
		return this.parameters.processor.queryTimeout;
	}

	public int getSchedulingQueueSize() {
		return this.parameters.scheduler.queueSize;
	}

	public int getWsPort() {
		return this.parameters.gates.ports.ws;
	}

	public int getHttpPort() {
		return this.parameters.gates.ports.http;
	}

	public String getUpdatePath() {
		return this.parameters.gates.paths.update;
	}

	public String getSubscribePath() {
		return this.parameters.gates.paths.subscribe;
	}

	public String getUnsubscribePath() {
		return this.parameters.gates.paths.unsubscribe;
	}

	public String getQueryPath() {
		return this.parameters.gates.paths.query;
	}

	public String getRegisterPath() {
		return this.parameters.gates.paths.register;
	}

	public String getTokenRequestPath() {
		return this.parameters.gates.paths.tokenRequest;
	}

	public int getSPUProcessingTimeout() {
		return this.parameters.spu.timeout;
	}

	public boolean isUpdateReliable() {
		return this.parameters.processor.reliableUpdate;
	}

	public int getSchedulerTimeout() {
		return this.parameters.scheduler.timeout;
	}

}
