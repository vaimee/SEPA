# SEPA - SPARQL Event Processing Architecture
[![Build Status](https://travis-ci.org/arces-wot/SEPA.svg?branch=master)](https://travis-ci.org/arces-wot/SEPA)
[ ![pac-pattern API](https://img.shields.io/badge/client--pattern--api-latest-blue.svg) ](https://bintray.com/arces-wot/sepa-java-libs/client-pac-pattern)
[ ![client api](https://img.shields.io/badge/client%20api-latest-blue.svg) ](https://bintray.com/arces-wot/sepa-java-libs/client-api)


SEPA is a publish-subscribe architecture designed to support information level interoperability. The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard **SPARQL** Updates and Queries. Notifications about events (i.e., changes in the **RDF** knowledge base) are expressed in terms of added and removed SPARQL binding results since the previous notification.

## Installation

- Download the [SEPA Engine](https://github.com/arces-wot/SEPA/releases/latest) and run it: `java -jar engine-x.y.z.jar`

- Download [Blazegraph](https://sourceforge.net/projects/bigdata/files/latest/download) (or use any other SPARQL 1.1 Protocol compliant service) and run it as shown [here](https://wiki.blazegraph.com/wiki/index.php/Quick_Start) 

## Configuration

The SEPA engine uses two JSON configuration files: `engine.jpar` and `endpoint.jpar` (included in the [SEPA Engine release](https://github.com/arces-wot/SEPA/releases/download/0.7.0/engine-0.7.0.rar) distribution). 
In the repository you will find some versions of `endpoint-{something}.jpar` file. According to your underlying endpoint, you have to rename the correct file to `endpoint.jpar`.
The default version of `endpoint.jpar` (will be removed soon!) configures the engine to use use a local running instance of Blazegraph as [SPARQL 1.1 Protocol Service](https://www.w3.org/TR/sparql11-protocol/).

```json
{
	"parameters": {
		"host": "localhost",
		"ports": {
			"http": 9999
		},
		"paths": {
			"update": "/blazegraph/namespace/kb/sparql",
			"query": "/blazegraph/namespace/kb/sparql"
		},
		"methods": {
			"query": "POST",
			"update": "URL_ENCODED_POST"
		},
		"formats": {
			"update": "HTML",
			"query": "JSON"
		}
	}
}
```
The default version of  `engine.jpar` configures the engine to listen for incoming [SPARQL 1.1 SE Protocol](http://wot.arces.unibo.it/TR/sparql11-se-protocol/) requests at the following URLs:

1. Query: http://localhost:8000/query
2. Update: http://localhost:8000/update
3. Subscribe/Unsubscribe: ws://localhost:9000/subscribe
4. SECURE Query: https://localhost:8443/secure/query
5. SECURE Update: https://localhost:8443/secure/update
6. SECURE Subscribe/Unsubscribe: wss://localhost:9443/secure/subscribe 
7. Regitration: https://localhost:8443/oauth/register
8. Token request: https://localhost:8443/oauth/token
```json
{
	"parameters": {
		"timeouts": {
			"scheduling": 0,
			"queueSize": 1000,
			"keepalive": 5000,
			"http": 5000
		},
		"ports": {
			"http": 8000,
			"ws": 9000,
			"https": 8443,
			"wss": 9443
		},
		"paths": {
			"update": "/update",
			"query": "/query",
			"subscribe": "/subscribe",
			"register": "/oauth/register",
			"tokenRequest": "/oauth/token",
			"securePath" : "/secure"
		}
	}
}
```
The engine uses a JKS for storing the keys and certificates for [SSL](http://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6ek/index.html) and [JWT](https://tools.ietf.org/html/rfc7519) signing/verification. A default `sepa.jks` is provided including a single X.509 certificate (the password for both the store and the key is: `sepa2017`). If you face problems using the provided JKS, please delete the `sepa.jks` file and create a new one as follows: `keytool -genkey -keyalg RSA -alias sepakey -keystore sepa.jks -storepass sepa2017 -validity 360 -keysize 2048`

## Usage

The SEPA engine allows to use a user generated JKS. Run `java -jar engine-x.y.z.jar -help` for a list of options. The Java [Keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html) can be used to create, access and modify a JKS. 

The SEPA engine is also distributed with a default [JMX](http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html) configuration `jmx.properties` (including the `jmxremote.password` and `jmxremote.access` files for password and user grants). Remember to change password file permissions using: `chmod 600 jmxremote.password`. To enable remote JMX, the engine must be run as follows: `java -Dcom.sun.management.config.file=jmx.properties -jar engine-x.y.z.jar`. Using [`jconsole`](http://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html) is possible to monitor and control the most important engine parameters. By default, the port is `5555` and the `root:root` credentials grant full control (read/write).

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Check some IDE specific instruction below
4. Commit your changes: `git commit -am 'Add some feature'`
5. Push to the branch: `git push origin my-new-feature`
6. Submit a pull request :D

### Clone in Eclipse

1. Open Eclipse
2. File > Import > Maven
3. Choose "Check out Maven Projects from SCM"
4. In the field SCM URL choose 'git' and add the clone address from Github. If 'git' is not found, tap into "Find more SCM connectors in the m2e Marketplace"
5. go on...
The project is cloned. Enjoy!

### Build with maven
After the project is cloned use:
`mvn install -DskipTests`


## History

SEPA has been inspired and influenced by [Smart-M3](https://sourceforge.net/projects/smart-m3/). SEPA authors have been involved in the development of Smart-M3 since its [origin](https://artemis-ia.eu/project/4-sofia.html). 

The main differences beetween SEPA and Smart-M3 are the protocol (now compliant with the [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/)) and the introduction of a security layer (based on TLS and JSON Web Token for client authentication). 

All the SEPA software components have been implemented from scratch.

## Credits

SEPA stands for *SPARQL Event Processing Architecture*. SEPA is promoted and maintained by the [**Dynamic linked data and Web of Things Research Group**](https://site.unibo.it/wot/en) @ [**ARCES**](http://www.arces.unibo.it), the *Advanced Research Center on Electronic Systems "Ercole De Castro"* of the [**University of Bologna**](http://www.unibo.it).

## License

SEPA Engine is released under the [GNU GPL](https://github.com/arces-wot/SEPA/blob/master/engine/LICENSE), SEPA APIs are released under the  [GNU LGPL](https://github.com/arces-wot/SEPA/blob/master/client-api/LICENSE)
