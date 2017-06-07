# SEPA - SPARQL Event Processing Architecture

SEPA is a publish-subscribe architecture designed to support information level interoperability. The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard **SPARQL** Updates and Queries. Notifications about events (i.e., changes in the **RDF** knowledge base) are expressed in terms of added and removed SPARQL binding results since the previous notification.

## Installation

TODO: Describe the installation process

## Usage

TODO: Write usage instructions

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## History

SEPA has been inspired and influenced by [Smart-M3](https://sourceforge.net/projects/smart-m3/). SEPA authors have been involved in the development of Smart-M3 since its [origin](https://artemis-ia.eu/project/4-sofia.html). The main differences beetween SEPA and Smart-M3 are the protocol (now compliant with the [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/)) and the introduction of a security layer (based on TLS and JSON Web Token for client authentication). All the SEPA software components have been implemented from scratch.

## Credits

SEPA stands for *SPARQL Event Processing Architecture* and it is promoted and maintained by the [**Web of Things Research Group**](http://wot.arces.unibo.it) @ [**ARCES**](http://www.arces.unibo.it), the *Advanced Research Center on Electronic Systems "Ercole De Castro"* of the [**University of Bologna**](http://www.unibo.it).

## License

TODO: Write license
