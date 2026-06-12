const assert = require('assert');
const Jsap = require('../../lib/jsap')
const test_jsap = require("../jsapconfig")

describe('Integration tests for jsap api', function() {
    let jsap
    beforeEach(function() {
      jsap = new Jsap(test_jsap)
    });

    it('test producer', function() {
       return jsap.Producers.simple().then((res)=>{
         assert.equal(200,res.status)
       }).catch((err)=> {
         console.log(err);
         assert.ok(false,"Update not confirmed: "+err)
       })
    });

    it('test producer with bindings dafaults', function() {
       return jsap.Producers.defaultArgs().then((res)=>{
         assert.equal(200,res.status)
       }).catch((err)=> {
         console.log(err);
         assert.ok(false,"Update not confirmed: "+err)
       })
    });

    it('test producer with bindings override', function() {
       return jsap.Producers.defaultArgs({obj : 'jsap'}).then((res)=>{
         assert.equal(200,res.status)
       }).catch((err)=> {
         console.log(err);
         assert.ok(false,"Update not confirmed: "+err)
       })
    });

    it('test consumer', function(done) {
       let consumer = jsap.Consumers.simple({})
       consumer.on("notification", 
       data => {
         assert.equal(data.sequence,0)
         consumer.unsubscribe()
         done()
       })
    });

    it('test consumer with bindings default', function(done) {
       let consumer = jsap.Consumers.defaultArgs({})
       consumer.on("notification" ,data => {
         assert.equal(data.sequence,0)
         consumer.unsubscribe()
         done()
       })
    });

    it('test consumer with bindings override', function(done) {
       let consumer = jsap.Consumers.defaultArgs({a:"jsap"})
       consumer.on("notification",data => {
         assert.equal(data.sequence,0)
         consumer.unsubscribe()
         done()
       })
    });

    it('test consumer - producer notifcation', function(done) {
       jsap.deleteIntgration().then(res =>{
         assert.equal(200,res.status)
         let sub = jsap.integration({})
         sub.on("notification",data =>{
           if(data.sequence == 0){
             jsap.notification().then(res =>{
               assert.equal(200,res.status)
             })
           }
           // skip the confirmation message
           if(data.sequence > 0){
             assert.equal(data.addedResults.results.bindings[0].c.value,"Hello World")
             sub.unsubscribe()
             done()
           }
         })
       })
    });
})
