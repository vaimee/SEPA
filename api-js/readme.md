<div align="center">
<br>
  <a href="">
    <img width="300px" src="./doc/logoJS.png">
  </a>
</div>


[![Build Status](https://travis-ci.org/arces-wot/SEPA-js.svg?branch=master)](https://travis-ci.org/arces-wot/SEPA-js)
[![SEPA 0.9.7](https://img.shields.io/badge/SEPA-0.9.7-blue.svg)](https://github.com/arces-wot/SEPA/releases/download/0.9.7/engine-0.9.7.jar)
[![npm version](https://badge.fury.io/js/%40arces-wot%2Fsepa-js.svg)](https://badge.fury.io/js/%40arces-wot%2Fsepa-js)
[![Web Version](https://data.jsdelivr.com/v1/package/npm/@arces-wot/sepa-js/badge)](https://www.jsdelivr.com/package/npm/@arces-wot/sepa-js)


A minimal SEPA client for browser and nodejs environments.
**Note** : this library is in an early development stage, use at your own risk.

## Installation

`npm i @arces-wot/sepa-js`

or in your html document

`<script src="https://cdn.jsdelivr.net/npm/@arces-wot/sepa-js/web/sepa.js"/>`

## Usage
SEPA-js comes with basic api to interact with the engine (Core API). But it also provides a rich interface to create Dynamic Linked Data applications with the support of **J**SON **S**emantic **A**pplication **P**rofile (JSAP).

### Core API

##### Nodejs:
```javascript
const sepa = require('@arces-wot/sepa-js').client
```
##### Browser:
```javascript
const sepa = Sepajs.client
```
The variable presented above returns a pre-configured sepa client instace. The following constructor can be used to have more control about defaults and protocol paramenter.

```javascript
const SEPA =  require('@arces-wot/sepa-js').SEPA
let client = new SEPA({/*...config...*/})
```
the following is the list of the parameters that can be set in a SEPA client instance:
```json
{
	"host": "localhost",
	"oauth": {
		"register": "https://localhost:8443/oauth/register",
		"tokenRequest": "https://localhost:8443/oauth/token"
	},
	"sparql11protocol": {
		"protocol": "http",
		"port": 8000,
		"query": {
			"path": "/query",
			"method": "POST",
			"format": "JSON"
		},
		"update": {
			"path": "/update",
			"method": "POST",
			"format": "JSON"
		}
	},
	"sparql11seprotocol": {
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/secure/subscribe"
			}
		}
	},
	"options" : {
		"httpsAgent" : httpsAgent,
		"headers" : {
			"fancy-header" : "content"
		}
	}
}
```
Refer to [official documentation](http://mml.arces.unibo.it/TR/jsap.html#protocol-parameters) for details of protocol parameters, while options field can be configured with the same properties defined in [axios](https://github.com/axios/axios) configuration schema.
#### Subscribe
The subscribe primitive allows a client to receive notifications about a query result. More information about notification data format can be found [here](http://mml.arces.unibo.it/TR/sparql11-subscribe.html#SubscribeResponse). This function as all the others can accept a configuration object as second argument. The object specifies particular parameters that are
valid only for this function call. The returned object is a subscription which can be revoked using the unsubscribe function.
```javascript
const sub = sepa.subscribe("select * where{?sub ?obj ?pred}LIMIT 1",{host:"www.vaimee.com"})

sub.on("subscribed",console.log)

sub.on("notification", not => {
  console.log("Notifcation:");
	console.log(JSON.stringify(not, null, 2));
	sub.unsubscribe()
})

sub.on("error",console.error)
```

#### Publish
The update function sends the SPARQL 1.1 update given as the first argument. For further information about response format see [here](https://www.w3.org/TR/sparql11-protocol/). Notice that
also this function accepts a configuration object as second argument.
```javascript
sepa.update("insert {<hello> <from> 'js'}where{}", {host:"www.vaimee.com"})
    .then(()=>{console.log("Updated");})
```

#### Query
For polling functionalities the query primitive issues a SPARQL 1.1 query to the endpoint. To know more about response data format refer to the [official documentation](https://www.w3.org/TR/sparql11-results-json/).
The last argument of this function is the configuartion object used to specific protocol parameters that are valid only for this function call.
```javascript
sepa.query("select * where {?s ?p 'js'}", {host:"www.vaimee.com"})
    .then((data)=>{console.log("SPARQL bindings: " + data);})
```

### Security
Core api supports secure connection with the endpoint. After obtaining `clientID` and `clientSecret` pair, a secure client can be instatieted with:
```javascript
const SecSEPA = require('@arces-wot/sepa-js').client.secure

const secClient = new SecSEPA(clientID,clientSecret)
```
If your SEPA instance supports the `register` primitive you can use the corrisponding function in sepa-js.
```javascript
const register = require('@arces-wot/sepa-js').client.secure.register

register("SEPATest").then(sClient =>{
	return sclient.query("select * where {?s ?p 'js'}")
})
```  

Secure client has the same interface of the unsecure one please refer to [Core API](#Core-API) for details.

#### Certificate
SEPA-js client ships with the default certificate published on SEPA repository, if you have a custom instance of SEPA engine you should
configure the SEPA certificate in your secure client as following:

```javascript
const https = require('https')
// Your certificate
const ca = `
Bag Attributes
    friendlyName: sepakey
    localKeyID: 54 69 6D 65 20 31 35 35 33 32 34 39 38 34 33 33 34 36
subject=/C=IT/ST=Bologna/L=Bologna/CN=localhost
issuer=/C=IT/ST=Bologna/L=Bologna/CN=localhost
-----BEGIN CERTIFICATE-----
MIIDKTCCAhGgAwIBAgIEbMARRzANBgkqhkiG9w0BAQsFADBFMQswCQYDVQQGEwJJ
VDEQMA4GA1UECBMHQm9sb2duYTEQMA4GA1UEBxMHQm9sb2duYTESMBAGA1UEAxMJ
bG9jYWxob3N0MB4XDTE5MDMyMjEwMDg1M1oXDTIwMDMxNjEwMDg1M1owRTELMAkG
A1UEBhMCSVQxEDAOBgNVBAgTB0JvbG9nbmExEDAOBgNVBAcTB0JvbG9nbmExEjAQ
BgNVBAMTCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
AJZ9ajuqEqXGU5QAWyMG3w4hScec9BXjOwDeqseSDDxOx/KuHCG5JDTiQzmPBT96
LLUGTn+c2l2c+Ezm4Dk11qjpJ+aiiv+gGvyNJmpw/+UwW7wp13O9sMr21GZxexmZ
/xV/nsXFXXoUurCwZecTzQ6UcNvrvlUy7NVr2TU+ZpwWR/DyKhxe452VJlEaP9Yk
Zu/g9x9rYMs7iG4qErQnGhGS6ds8fU6VrzPCAW9EFxC1SN7r2xnPREU/igv0SilX
e+tPk167l1wgGyBl3K4Ep7O8RoSE/EhyuooZ9oVenqD/MRlKLso1O2Kv7DslrBpv
8UufAWxwwEOhDA3XXfblsDkCAwEAAaMhMB8wHQYDVR0OBBYEFCxDyoFe03ccASA9
JfyvYFEAStceMA0GCSqGSIb3DQEBCwUAA4IBAQBvWzC0qLNhp8L9GkoaNtNKJGEu
WQkqaMDBtrD4Jy+I75/k73ivvwVgbgg0kq9+jYC48tWwcBsDzqNau+Zay4rWZlf9
qbnP3+j4hgLIBPrAAvxWQBzLrVOkZK1hXdrS1fNCFmYdIwlEU7M06C3mv69CD/yJ
vJF2FczexVR2I2L15JdpVlqZ35KwQ8QRTKTtwvQxZeZG56g+Db0vGMMwJqSpPRZc
WdUXV+2aTVZWdO3avHXkS/qZ0A+8HX8bVvm8O/5b21bIo9BfCf3za3/CAVSNFfNp
VfDUVhC465CzJcei94rxKyjWTuVl7CZA+6e2x5Ua/4tASi0sFFAlqGJIpiXr
-----END CERTIFICATE-----
`
const httpsAgent = new https.Agent({ ca: [ca] })
const SecSEPA = require('@arces-wot/sepa-js').secure

const secClient = new SecSEPA(clientID,clientSecret,{ options : {
	httpsAgent : httpsAgent
}})

// Or using  register function

const register = require('@arces-wot/sepa-js').client.secure.register

register("SEPATest",{ options : { httpsAgent : httpsAgent}}).then(sClient =>{
	return sclient.query("select * where {?s ?p 'js'}")
})

```

### Query bench api
From v0.10.0 SEPAjs provides apis to store query templates and substitutes variables. See the following example:
```javascript
const Bench = require('sepajs').bench

bench = new Bench()
query = bench.sparql("select * where{?a ?b ?c.}",{
	a:{
	   type: "uri",
       value :"urn:epc:id:gid:0.1.0102030405060708090A0B0C"
	}
})
// query : select * where{<urn:epc:id:gid:0.1.0102030405060708090A0B0C> ?b ?c}
```
for futher details check query bench unit tests [here](./tests/querybench.js).

**Note**: Inside broswers the bench api can be required with `const bench = new Sepajs.bench();` as for other Sepajs functionalities.

### JSAP api
**J**ons **S**parql **A**pplication **P**rofile. JASAP api leverage on Query Bench API to provide an application development model.

#### Nodejs:
```javascript
const App = require('sepa-js').Jsap
```
#### Browser:
```javascript
const JsapApi = Sepajs.Jsap
```

```javascript
app = new JsapApi({
	host: "mml.arces.unibo.it",
	queries : {
		simpleQuery : { sparql : "select * where {?a ?b ?c}"}
	}
})

let subscription = app.simpleQuery({})
subscription.on("notification",console.log)
```

#### JSAP object example:
```javascript
jsap_example = {
	host: "mml.arces.unibo.it",
	oauth: {
		enable : false,
		register: "https://localhost:8443/oauth/register",
		tokenRequest: "https://localhost:8443/oauth/token"
	},
	sparql11protocol: {
		protocol: "http",
		port: 8000,
		query: {
			path: "/query",
			method: "POST",
			format: "JSON"
		},
		update: {
			path: "/update",
			method: "POST",
			format: "JSON"
		}
	},
	sparql11seprotocol: {
		protocol: "ws",
		availableProtocols: {
			ws: {
				port: 9000,
				path: "/subscribe"
			},
			wss: {
				port: 9443,
				path: "/secure/subscribe"
			}
		}
	},
	namespaces: {
		exp: "http://www.w3.org/example#",
	},
	updates: {
		simpleUpdate: {
			sparql: "INSERT DATA { exp:hello exp:from 'js' }"
		},
		updateArgs: {
			sparql: "INSERT DATA {?sub ?pred ?obj}",
			forcedBindings: {
				sub: {
					type: "uri",
					value: "exp:hello"
				},
				pred: {
					type: "uri",
					value: "exp:from"
				},
				obj: {
					type: "literal",
					value: "js"
				}
			}
		}
	},
	 queries : {
		 simpleQuery : {
			 sparql : "select * where{?a ?b ?c}"
		},
		 queryArgs : {
			 sparql : "select * where{?a ?b ?c}",
			 forcedBindings : {
				 a : {
					 type : "uri",
					 value : "exp:subj"
				}
			}
		}
	}
}
```
#### Subscriber
```javascript
subscriber = new JsapApi(jsap_example)
let sub = subscriber.simpleQuery({})
sub.on("notification",console.log)

```
#### Publisher
```javascript
publisher = new JsapApi(jsap_example)
publisher.simpleUpdate().then(res=>{console.log("Update response: " + res)})
```
#### Forced bindings
The JSAP api support query bindings to easly inject data in query templates. Here is an example to use a producer with code specifed bindings:
```javascript
app = new JsapApi(jsap_example)
data = {
  sub : "exp:person1",
  pred: "exp:hasName",
  obj : "Max"
}
app.updateArgs(data).then(res=>{console.log("Update response: " + res)})
```
The SPARQL update issued to the broker will be:
```sparql
PREFIX exp:<http://www.w3.org/example#>
INSERT DATA {exp:person1 exp:hasName 'Max'}
```
**Note** with JSAP you can specify default arguments and their types

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
Please make sure to update tests as appropriate.

**Note**: run tests with `npm run tests` and if succefull `npm run integration-test`. Additionally, integration tests needs a default SEPA instance 
running on your local machine. Refer to [SEPA github page](https://github.com/arces-wot/SEPA) for information about the installation and configuration. 

## License
[LGPL](https://choosealicense.com/licenses/lgpl-3.0/)
