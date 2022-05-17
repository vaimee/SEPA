/* This class is the main entry point of the Semantic Event Processing Architecture (SEPA) Engine
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

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
package it.unibo.arces.wot.sepa.engine.core;

import java.util.Optional;

import java.util.regex.PatternSyntaxException;

import javax.net.ssl.SSLContext;

import com.nimbusds.jose.jwk.RSAKey;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityMonitor;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.IsqlProperties;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.JKSUtil;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.LdapProperties;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpGate;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpsGate;
import it.unibo.arces.wot.sepa.engine.gates.websocket.SecureWebsocketServer;
import it.unibo.arces.wot.sepa.engine.gates.websocket.WebsocketServer;
import it.unibo.arces.wot.sepa.engine.processing.Processor;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.logging.Logging;

/**
 * This class represents the SPARQL Subscription Broker (Core) of the SPARQL
 * Event Processing Architecture (SEPA)
 *
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.10.0
 */

public class Engine implements EngineMBean {
	private final static String version = "1.0.20220517";

	private EngineProperties properties = null;

	// Primitives scheduler/dispatcher
	private Scheduler scheduler = null;

	// Primitives scheduler/dispatcher
	private Processor processor = null;

	// SPARQL 1.1 Protocol handler
	private HttpGate httpGate = null;

	// SPARQL 1.1 SE Protocol handler
	private WebsocketServer wsServer = null;
	private HttpsGate httpsGate = null;
	private int wsShutdownTimeout = 5000;

	// Properties files
	private String engineJpar = "engine.jpar";
	private String endpointJpar = "endpoint.jpar";

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

	private void printUsage() {
		System.out.println("Usage:");
		System.out.println(
				"java [JMX] [JVM] [LOG4J] -jar SEPAEngine_X.Y.Z.jar [-help] [-secure=true] [-engine=engine.jpar] [-endpoint=endpoint.jpar] [JKS OPTIONS] [LDAP OPTIONS] [ISQL OPTIONS]");
		System.out.println("Options: ");
		System.out.println("-secure : overwrite the current secure option of engine.jpar");
		System.out.println(
				"-engine : can be used to specify the JSON configuration parameters for the engine (default: engine.jpar)");
		System.out.println(
				"-endpoint : can be used to specify the JSON configuration parameters for the endpoint (default: endpoint.jpar)");
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
		System.out.println(
				"-isqlpath <path> : location of isql     		 (default: /usr/local/virtuoso-opensource/bin/)");
		System.out.println("-isqlhost <host> : host of Virtuoso     		 (default: localhost)");
		System.out.println("-isqluser <user> : user of Virtuoso     		 (default: dba)");
		System.out.println("-isqlpass <pass> : password of Virtuoso     	 (default: dba)");
	}

	private void parsingArgument(String[] args) throws PatternSyntaxException {
		for (int i = 0; i < args.length; i = i + 2) {
			if (args[i].equals("-help")) {
				printUsage();
				return;
			}

			switch (args[i].toLowerCase()) {
			case "-capwd":
				caPassword = args[i + 1];
				break;
			case "-cacertificate":
				caCertificate = args[i + 1];
				break;
			case "-capath":
				caPath = args[i + 1];
				break;

			case "-sslstore":
				sslStoreName = args[i + 1];
				break;
			case "-sslpass":
				sslStorePass = args[i + 1];
				break;
			case "-jwtalias":
				jwtKeyAlias = args[i + 1];
				break;
			case "-jwtstore":
				jwtKeyStore = args[i + 1];
				break;
			case "-jwtstorepass":
				jwtKeyStorePass = args[i + 1];
			case "-jwtaliaspass":
				jwtKeyAliasPass = args[i + 1];
				break;

			case "-engine":
				engineJpar = args[i + 1];
				break;
			case "-endpoint":
				endpointJpar = args[i + 1];
				break;

			case "-secure":
				secure = Optional.of(Boolean.parseBoolean(args[i + 1]));
				break;

			case "-ldaphost":
				ldapHost = args[i + 1];
				break;
			case "-ldapport":
				ldapPort = Integer.parseInt(args[i + 1]);
				break;
			case "-ldapdn":
				ldapDn = args[i + 1];
				break;
			case "-ldapusersdn":
				ldapUsersDn = args[i + 1];
				break;
			case "-ldapuser":
				ldapUser = args[i + 1];
				break;
			case "-ldappwd":
				ldapPwd = args[i + 1];
				break;

			case "-isqlpath":
				isqlPath = args[i + 1];
				break;
			case "-isqlhost":
				isqlHost = args[i + 1];
				break;
			case "-isqluser":
				isqlUser = args[i + 1];
				break;
			case "-isqlpass":
				isqlPass = args[i + 1];
				break;
			case "-isqlport":
				isqlPort = Integer.parseInt(args[i + 1]);
				break;
			default:
				break;
			}
		}

		Logging.logger.debug("--- SSL ---");
		Logging.logger.debug("-cacertificate: " + caCertificate);
		Logging.logger.debug("-capwd: " + caPassword);
		Logging.logger.debug("-capath: " + caPath);
		Logging.logger.debug("-sslstore: " + sslStoreName);
		Logging.logger.debug("-sslpass: " + sslStorePass);

		Logging.logger.debug("--- JWT ---");
		Logging.logger.debug("-jwtstore: " + jwtKeyStore);
		Logging.logger.debug("-jwtpass: " + jwtKeyStorePass);
		Logging.logger.debug("-jwtalias: " + jwtKeyAlias);
		Logging.logger.debug("-jwtaliaspass: " + jwtKeyAliasPass);

		Logging.logger.debug("--- Engine/endpoint ---");
		Logging.logger.debug("-engine: " + engineJpar);
		Logging.logger.debug("-endpoint: " + endpointJpar);
		Logging.logger.debug("-secure: " + secure);

		Logging.logger.debug("--- LDAP ---");
		Logging.logger.debug("-ldaphost: " + ldapHost);
		Logging.logger.debug("-ldapport: " + ldapPort);
		Logging.logger.debug("-ldapdn: " + ldapDn);
		Logging.logger.debug("-ldapusersdn: " + ldapUsersDn);
		Logging.logger.debug("-ldapuser: " + ldapUser);
		Logging.logger.debug("-ldappwd: " + ldapPwd);

		Logging.logger.debug("--- ISQL ---");
		Logging.logger.debug("-isqlpath: " + isqlPath);
		Logging.logger.debug("-isqlhost: " + isqlHost);
		Logging.logger.debug("-isqlport: " + isqlPort);
		Logging.logger.debug("-isqluser: " + isqlUser);
		Logging.logger.debug("-isqlpass: " + isqlPass);
	}

	private void setSecurity() throws SEPASecurityException {
		// OAUTH 2.0 Authorization Manager
		if (properties.isSecure()) {
			ssl = JKSUtil.getSSLContext(sslStoreName, sslStorePass);
			jwt = JKSUtil.getRSAKey(jwtKeyStore, jwtKeyStorePass, jwtKeyAlias, jwtKeyAliasPass);
			ldap = new LdapProperties(ldapHost, ldapPort, ldapDn, ldapUsersDn, ldapUser, ldapPwd, properties.isTls());
			isql = new IsqlProperties(isqlPath, isqlHost, isqlPort,isqlUser, isqlPass);
			if (properties.isLocalEnabled())
				Dependability.enableLocalSecurity(ssl, jwt);
			else if (properties.isLDAPEnabled()) {
				Dependability.enableLDAPSecurity(ssl, jwt, ldap);
			} else if (properties.isKeycìCloakEnabled()) {
				Dependability.enableKeyCloakSecurity(ssl, jwt, ldap, isql);
			}

			// Check that SSL has been properly configured
			Dependability.getSSLContext();
		}
	}

	public Engine(String[] args) {
		System.out.println("##########################################################################################");
		System.out.println("#                           ____  _____ ____   _                                         #");
		System.out.println("#                          / ___|| ____|  _ \\ / \\                                        #");
		System.out.println("#                          \\___ \\|  _| | |_) / _ \\                                       #");
		System.out.println("#                           ___) | |___|  __/ ___ \\                                      #");
		System.out.println("#                          |____/|_____|_| /_/   \\_\\                                     #");
		System.out.println("#                                                                                        #");
		System.out.println("#                     SPARQL Event Processing Architecture                               #");
		System.out.println("#                                                                                        #");
		System.out.println("#                                                                                        #");
		System.out.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");
		System.out.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out.println("#                                                                                        #");
		System.out.println("#                                                                                        #");
		System.out.println("# @prefix git: <https://github.com/> .                                                   #");
		System.out.println("# @prefix dc: <http://purl.org/dc/elements/1.1/> .                                       #");
		System.out.println("#                                                                                        #");
		System.out.println("# git:arces-wot/sepa dc:title 'SEPA' ;                                                   #");
		System.out.println("# dc:creator git:lroffia ;                                                               #");
		System.out.println("# dc:contributor git:relu91 ;                                                            #");
		System.out.println("# dc:format <https://java.com> ;                                                         #");
		System.out.println("# dc:publisher <https://github.com> .                                                    #");
		System.out.println("##########################################################################################");
		System.out.println("");

		// Command arguments
		parsingArgument(args);

		// Beans
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		EngineBeans.setVersion(version);

		// Dependability monitor
		new DependabilityMonitor();

		try {
			// Initialize SPARQL 1.1 SE processing service properties
			properties = secure.isPresent() ? EngineProperties.load(engineJpar, secure.get())
					: EngineProperties.load(engineJpar);

			EngineBeans.setEngineProperties(properties);

			SPARQL11Properties endpointProperties = new SPARQL11Properties(endpointJpar);

			setSecurity();

			// SPARQL 1.1 SE request scheduler
			scheduler = new Scheduler(properties);
			Dependability.setScheduler(scheduler);
			
			// SEPA Processor
			processor = new Processor(endpointProperties, properties, scheduler);
			

			// SPARQL protocol service
			int port = endpointProperties.getPort();
			String portS = "";
			if (port != -1)
				portS = String.format(":%d", port);

			String queryMethod = "";
			switch (endpointProperties.getQueryMethod()) {
			case POST:
				queryMethod = " (Method: POST)";
				break;
			case GET:
				queryMethod = " (Method: GET)";
				break;
			case URL_ENCODED_POST:
				queryMethod = " (Method: URL ENCODED POST)";
				break;
			}

			String updateMethod = "";
			switch (endpointProperties.getUpdateMethod()) {
			case POST:
				updateMethod = " (Method: POST)";
				break;
			case URL_ENCODED_POST:
				updateMethod = " (Method: URL ENCODED POST)";
				break;
			}

			System.out.println("SPARQL 1.1 endpoint");
			System.out.println("----------------------");
			System.out.println("SPARQL 1.1 Query     | "+endpointProperties.getProtocolScheme()+"://" + endpointProperties.getHost() + portS
					+ endpointProperties.getQueryPath() + queryMethod);
			System.out.println("SPARQL 1.1 Update    | "+endpointProperties.getProtocolScheme()+"://" + endpointProperties.getHost() + portS
					+ endpointProperties.getUpdatePath() + updateMethod);
			System.out.println("----------------------");
			System.out.println("");

			// SPARQL 1.1 protocol gates
			System.out.println("SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			System.out.println("----------------------");

			if (!properties.isSecure())
				httpGate = new HttpGate(properties, scheduler);
			else
				httpsGate = new HttpsGate(properties, scheduler);

			// SPARQL 1.1 SE protocol gates
			System.out.println("----------------------");
			System.out.println("");
			System.out.println("SPARQL 1.1 SE Protocol (http://mml.arces.unibo.it/TR/sparql11-se-protocol.html)");
			System.out.println("----------------------");

			if (!properties.isSecure()) {
				wsServer = new WebsocketServer(properties.getWsPort(), properties.getSubscribePath(), scheduler);
			} else {
				wsServer = new SecureWebsocketServer(properties.getWssPort(),
						properties.getSecurePath() + properties.getSubscribePath(), scheduler);
			}

			// Start all
			scheduler.start();
			processor.start();
			wsServer.start();

			synchronized (wsServer) {
				wsServer.wait(5000);
			}

			System.out.println("----------------------");

			// Welcome message
			System.out.println("");
			System.out.println("*****************************************************************************************");
			System.out.println("*                        SEPA Broker Ver is up and running                              *");
			System.out.println("*                        Let Things Talk and Data Be Free!                              *");
			System.out.println("*****************************************************************************************");
			System.out.print("Version "+version);
			
			Logging.init();

		} catch (SEPAPropertiesException | SEPASecurityException | IllegalArgumentException | SEPAProtocolException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws SEPASecurityException, SEPAProtocolException {
		// Attach CTRL+C hook
		Runtime.getRuntime().addShutdownHook(new EngineShutdownHook(new Engine(args)));
	}

	public void shutdown() throws InterruptedException {
		System.out.println("Stopping...");

		if (httpGate != null) {
			System.out.println("Stopping HTTP gate...");
			httpGate.shutdown();
		}

		if (httpsGate != null) {
			System.out.println("Stopping HTTPS gate...");
			httpsGate.shutdown();
		}

		if (wsServer != null) {
			System.out.println("Stopping WebSocket gate...");
			wsServer.stop(wsShutdownTimeout);
		}

		System.out.println("Stopping Processor...");
		processor.interrupt();

		System.out.println("Stopped...bye bye :-)");
	}

	@Override
	public String getUpTime() {
		return EngineBeans.getUpTime();
	}

	@Override
	public void resetAll() {
		EngineBeans.resetAll();
	}

	@Override
	public String getVersion() {
		return EngineBeans.getVersion();
	}

	@Override
	public String getQueryPath() {
		return EngineBeans.getQueryPath();
	}

	@Override
	public String getUpdatePath() {
		return EngineBeans.getUpdatePath();
	}

	@Override
	public String getSubscribePath() {
		return EngineBeans.getSubscribePath();
	}

	@Override
	public String getSecurePath() {
		return EngineBeans.getSecurePath();
	}

	@Override
	public String getRegisterPath() {
		return EngineBeans.getRegisterPath();
	}

	@Override
	public String getTokenRequestPath() {
		return EngineBeans.getTokenRequestPath();
	}

	@Override
	public int getHttpPort() {
		return EngineBeans.getHttpPort();
	}

	@Override
	public int getHttpsPort() {
		return EngineBeans.getHttpsPort();
	}

	@Override
	public int getWsPort() {
		return EngineBeans.getWsPort();
	}

	@Override
	public int getWssPort() {
		return EngineBeans.getWssPort();
	}

	@Override
	public boolean getSecure() {
		return EngineBeans.getSecure();
	}

	@Override
	public String getSSLCertificate() {
		if (!properties.isSecure())
			return "Security off";
		return jwt.getParsedX509CertChain().get(0).getIssuerDN().getName() + " "+ jwt.getParsedX509CertChain().get(0).getNotAfter().toString();
	}

	@Override
	public void refreshSSLCertificate() {
		try {
			setSecurity();
		} catch (SEPASecurityException e) {
			Logging.logger.error(e.getMessage());
		}
	}
}
