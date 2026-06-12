const assert = require('assert');
const register = require('../../lib/secure').register
const https = require('https')
//Default self-signed sepa certificate
const ca = `
Bag Attributes
    friendlyName: localhost
    localKeyID: 54 69 6D 65 20 31 35 37 35 34 36 39 38 30 35 35 35 36
subject=/C=IT/ST=Bologna/L=Bologna/O=UNIBO/OU=ARCES/CN=localhost
issuer=/C=IT/ST=Bologna/L=Bologna/O=UNIBO/OU=ARCES/CN=localhost
-----BEGIN CERTIFICATE-----
MIIDaTCCAlGgAwIBAgIEM921wTANBgkqhkiG9w0BAQsFADBlMQswCQYDVQQGEwJJ
VDEQMA4GA1UECBMHQm9sb2duYTEQMA4GA1UEBxMHQm9sb2duYTEOMAwGA1UEChMF
VU5JQk8xDjAMBgNVBAsTBUFSQ0VTMRIwEAYDVQQDEwlsb2NhbGhvc3QwHhcNMTkw
MzIxMTczODE2WhcNMjAwMzE1MTczODE2WjBlMQswCQYDVQQGEwJJVDEQMA4GA1UE
CBMHQm9sb2duYTEQMA4GA1UEBxMHQm9sb2duYTEOMAwGA1UEChMFVU5JQk8xDjAM
BgNVBAsTBUFSQ0VTMRIwEAYDVQQDEwlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQCbnEEfLvR3pfXmIWMLrNrZ9Oc9IsDW2qL8CWgiaFLH
b/LSkT+F9uGZmrn3i6ZfI01+K8rP2OApL57FtDbocuqbueGTVP7L4cye+RXYfI2g
X9Ig4lSIl2n0RDIz6ps2q81C/s+3oxcGdgmrKQpL09+YwnMQfQkMC6vU5Ivk6pCr
IgXNclLF0bl0qrT5hZhpSA2cYMuqKyRWcGE0lgz/bzfV8suM04TS1IiR6enwdY4n
szjz9/040cTkZ4wGLoeDG2NSG+lOlEXX/CW1CvNorsMzohghJYn1+FANNlaHMsFE
22UAdoAXXwSbfkO2JJVSvjcnImy+fyAJqzqR4ToAoATFAgMBAAGjITAfMB0GA1Ud
DgQWBBQLT1A7bw/d6xsJB2LW2+J4PorIcjANBgkqhkiG9w0BAQsFAAOCAQEAE8vG
i8oVoovgf0y4Cq/cgEsOB+lLC4b6KERQh6imazdcopAuQMuDLdmRU+GQ6uOWKvIY
IGR3mH5xcJp2K1Xc5PxBA2aqTGP5237j309PlNE3Fxf5/UDeQIalxQZWfcSsXFXa
5icUUBBwvoPTXPnY0ahYvyJcT/8KWhQZsFkRBHcyCxkDhqrVBaxaSDr+UahJm3ZO
eys4rPxm1YtAtOO+apLqvomX5Ls+T5ol37DeCWrp4AcbGP9X+rcV02AqHKMavCxX
XvFCoewehkyOtRppTysKZrNjzs0XF2Nz1kzLXKXAT+W0jDZ5LaTbUl1Ijg+xlqJe
Ua+3Vi8p0X8MqK7dcQ==
-----END CERTIFICATE-----
`

describe('Core secure APIs integration tests', () => {
   let client
   
    before(() => {
        return register("SEPATest",{
            options : {
                httpsAgent: new https.Agent({ca:[ca]})
            }
        }).then((secureCli) => {
            client = secureCli
        })
    });
    
    it('Should login',async () => {
        await client.login()
        assert.ok(client.webToken,"Web Token undefined")
    })

    it('Should query', () => {
        return client.query("select * where{?a ?b ?c}")
    });

    it('Should update', () => {
        return client.update("INSERT DATA {<hello> <test> <test>}")
    });

    it('Should subscribe', (done) => {
        let sub = client.subscribe("select * where{?a ?b ?c}")

        sub.on("subscribed", () => { sub.kill(); done() })
        sub.on("error", done)
        sub.on("connection-error", done)
    });
    
    it('Should subscribe and unsubscribe', (done) => {
        let sub = client.subscribe("select * where{?a ?b ?c}")
        sub.on("subscribed", () => { sub.unsubscribe() })
        sub.on("error", done)
        sub.on("connection-error", done)
        sub.on("unsubscribed", () => done())
    });

    it('Subscribe should renew the webtoken', (done) => {
        let sub = client.subscribe("select * where{?a ?b ?c}")
        
        sub.on("subscribed", () => {
            sub.unsubscribe()
        })
        sub.on("unsubscribed", () => {
            new Promise((resolve, reject) => {
                setTimeout(resolve, 5100)
            }).then(async () => {
                let sub2 = await client.subscribe("select * where{?a ?b ?c}")
                sub2.on("subscribed", () => { done(); sub2.kill() })
                sub2.on("error", done)
                sub2.on("connection-error", done)
            })
        })
       
    });

    
    it('Unsubscribe should renew the webtoken', (done) => {
        let sub = client.subscribe("select * where{?a ?b ?c}")
        sub.on("subscribed", () => {
            new Promise((resolve) => {
                setTimeout(resolve, 5100)
            }).then(sub.unsubscribe.bind(sub))
        })

        sub.on("unsubscribed", () => { done() })
        sub.on("error", done)
        sub.on("connection-error", done)    
    })

    it('Query should renew token', () => {
            return client.query("select * where{?a ?b ?c}").then(() => {
                return new Promise((resolve, reject) => {
                    setTimeout(resolve, 5100)
                }).then(() => {
                    return client.query("select * where{?a ?b ?c}").then((resp) => {
                        assert.ok(resp,"Token seems renewed but the response is undefined")
                    }).catch((erro) => {
                        assert.fail("Token is not renewed")
                    })
                })
        })
    });

    it('Update should renew token', () => {
        return client.update("INSERT DATA {<hello> <test> <test>}").then(() => {
            return new Promise((resolve, reject) => {
                setTimeout(resolve, 5100)
            }).then(() => {
                return client.update("INSERT DATA {<hello> <test> <test>}").then((resp) => {
                    assert.ok(resp, "Token seems renewed but the response is undefined")
                }).catch((erro) => {
                    assert.fail("Token is not renewed")
                })
            })
        })
    });

});