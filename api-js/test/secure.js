const secure = require("../build/secure")
const assert = require("assert")

describe('Secure initizalition unit tests', () => {
    it('should merge configurations', () => {
        const client =  secure.defaultClient("a","b",{host :"another"})
        assert.equal(client.config.host,"another")
    });

    it('should forward execption', (done) => {
        const client =  secure.defaultClient("a","b",{host :"another"})
        client._autoRenewToken(()=>{
            return Promise.reject(new Error("Not ok"))
        }).then(() =>{
            console.log("hallo");
            done(new Error("Error not forwarded"))
        }).catch(()=>{
            done()
        })
    });
    
});