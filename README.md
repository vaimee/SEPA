# SEPA - SPARQL Event Processing Architecture

SEPA is a publish-subscribe architecture designed to support information level interoperability. The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard **SPARQL** Updates and Queries. Notifications about events (i.e., changes in the **RDF** knowledge base) are expressed in terms of added and removed SPARQL binding results since the previous notification.

## Installation

- Download the [SEPA Engine](https://github.com/arces-wot/SEPA/releases/download/0.7.0/engine-0.7.0.rar) and run it: `java -Dcom.sun.management.config.file=jmx.properties -jar engine-x.y.z.jar`

- Download [Blazegraph](https://sourceforge.net/projects/bigdata/files/latest/download) (or use any other SPARQL 1.1 Protocol compliant service) and run it as shown [here](https://wiki.blazegraph.com/wiki/index.php/Quick_Start) 

## Configuration

The SEPA engine uses two JSON configuration files: `engine.jpar` and `endpoint.jpar`. The default versions of these files (included in the   [SEPA Engine release](https://github.com/arces-wot/SEPA/releases/download/0.7.0/engine-0.7.0.rar) distribution) configure the engine as follows:
- to use a local running instance of Blazegraph as SPARQL 1.1 Protocol Service
- to use the following URLs for incoming SPARQL 1.1 SE Protocol requests:
---------- SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)  ---------------
1. Query URL: http://192.168.1.12:8000/query
2. Update URL: http://192.168.1.12:8000/update

------ SPARQL SE 1.1 Protocol (https://wot.arces.unibo.it/TR/sparql11-se-protocol/)  -----
3. Subscribe on: ws://192.168.1.12:9000/subscribe
4. SECURE Query URL: https://192.168.1.12:8443/secure/query
5. SECURE Update URL: https://192.168.1.12:8443/secure/update
6. Regitration URL: https://192.168.1.12:8443/oauth/register
7. Token request URL: https://192.168.1.12:8443/oauth/token
8. SECURE Subscribe on: wss://192.168.1.12:9443/secure/subscribe

## Usage

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## History

SEPA has been inspired and influenced by [Smart-M3](https://sourceforge.net/projects/smart-m3/). SEPA authors have been involved in the development of Smart-M3 since its [origin](https://artemis-ia.eu/project/4-sofia.html). 

The main differences beetween SEPA and Smart-M3 are the protocol (now compliant with the [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/)) and the introduction of a security layer (based on TLS and JSON Web Token for client authentication). 

All the SEPA software components have been implemented from scratch.

## Credits

SEPA stands for *SPARQL Event Processing Architecture*. SEPA is promoted and maintained by the [**Web of Things Research Group**](http://wot.arces.unibo.it) @ [**ARCES**](http://www.arces.unibo.it), the *Advanced Research Center on Electronic Systems "Ercole De Castro"* of the [**University of Bologna**](http://www.unibo.it).

## License

SEPA Engine is released under the [GNU GPL](https://github.com/arces-wot/SEPA/blob/master/engine/LICENSE), SEPA APIs are released under the  [GNU LGPL](https://github.com/arces-wot/SEPA/blob/master/client-api/LICENSE)
