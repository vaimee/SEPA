module.exports = `
{
	"host": "localhost",
	"oauth": {
		"enable": false,
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
	"namespaces": {
		"rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
		"rdfs": "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	},
	"extended": {
     "thirdparty" : "hey!"
	},
	"updates": {
		"simple": {
			"sparql": "INSERT { <hello> <from> <js> }WHERE{}"
		},
		"deleteIntgration" : {
			"sparql" : "DELETE {<integration> <jsap> ?a }WHERE{<integration> <jsap> ?a}"
		},
		"notification" : {
			"sparql" : "INSERT {<integration> <jsap> 'Hello World' }WHERE{}"
		},
		"costumProtocol" : {
			"sparql" : "INSERT {<integration> <jsap> 'Hello World' }WHERE{}",
			"sparql11protocol" : {
				"port": 7000,
				"update": {
					"format": "XML"
				}
			}
		},
		"defaultArgs": {
			"sparql": "INSERT {?sub ?pred ?obj}WHERE{}",
			"forcedBindings": {
				"sub": {
					"type": "uri",
					"value": "hello"
				},
				"pred": {
					"type": "uri",
					"value": "from"
				},
				"obj": {
					"type": "literal",
					"value": "js"
				}
			}
		}
	},
	"queries": {
		"simple": {
			"sparql": "select * where{?a ?b ?c}"
		},
		"integration": {
			"sparql": "select * where{<integration> <jsap> ?c}"
		},
		"defaultArgs": {
			"sparql": "select * where{?a ?b ?c}",
			"forcedBindings": {
				"a": {
					"type": "uri",
					"value": "subj"
				}
			}
		},
		"costumProtocol" : {
			"host": "other.host.it",
			"sparql" : "INSERT {<integration> <jsap> 'Hello World' }WHERE{}",
			"sparql11seprotocol" : {
				"protocol" : "wss"
			}
		}
	}
}
`
