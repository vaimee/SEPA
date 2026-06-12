const assert = require('assert');
const sepa = require('../../lib/core').client
var config_host = "localhost"

describe('Integration tests for api', function() {
    
    it('it should fail with wrong uri', function() {
      return sepa.query("select ?a where {<integration> <tests> ?a}",{host:"invalidURI"})
      .then(()=>{assert(false,"it should fail!")}).catch(() => {
        assert(true,"Ok")
      })
    });
    it('test query',function () {
      return sepa.query("select ?a where {<integration> <tests> ?a}LIMIT 1")
    })
    it('test subscription', function(done) {
      let sub = sepa.subscribe("select ?a where {<integration> <tests> ?a}", { host: config_host })
      sub.on("subscribed", notification => {
        sub.unsubscribe()
        assert.ok(notification)
        assert.equal(notification.sequence,0)
        done()
      })
      sub.on("error", (e) => {
        done(e)
      })
      sub.on("connection-error",(e) => {
        done(e)
      })
    });
    it('test subscription should give error', function(done) {
      let sub = sepa.subscribe("selectfa ?a where {<integration> <tests> ?a}", { host: config_host })
      
      sub.on("error",() => {
        sub.kill()
        done()
      })
      sub.on("notification",assert.ok.bind(false,"This subscription should fail"))
      
    });
    it('should fire connection error', function(done) {
      let sub = sepa.subscribe("selectfa ?a where {<integration> <tests> ?a}", { host: "invalidURI" })
      
      sub.on("connection-error",() => {
        done()
      })
      sub.on("notification",assert.ok.bind(false,"This subscription should fail"))
      
    });
    it('test notification with update', function(done) {
      sepa.update("delete{<integration> <tests> ?a}where{<integration> <tests> ?a}",
        {host:config_host}).then(
        (res)=>{
          assert.equal(200,res.status)
            let sub = sepa.subscribe("select ?a where {<integration> <tests> ?a}", { host: config_host })
            sub.on("notification", notification => {
             if(notification.sequence == 0){
              sepa.update("insert data{<integration> <tests> '--hello--'}",
              {host:config_host}).then(
                  (res)=>{
                    assert.equal(200,res.status)
                  }
                )
             }else{
              assert.equal(notification.addedResults.results.bindings[0].a.value,"--hello--")
                sub.unsubscribe()
                done()
             }
          })
          
        }
      )
    });

})
