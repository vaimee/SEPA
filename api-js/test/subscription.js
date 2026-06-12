const assert = require('assert');
const Subscription = require('../build/subscription').default
const EventEmitter = require('events').EventEmitter
const sinon = require('sinon')

describe('Subscription', () => {
    let connection
    let fakeNotificationStream
    
    beforeEach(() => {
        connection = new EventEmitter()
        fakeNotificationStream = new EventEmitter()
        connection.notificationStream = sinon.stub().returns(fakeNotificationStream)
        fakeNotificationStream.send = sinon.spy()
        fakeNotificationStream.close = sinon.spy()
    });
    
    it('should fire subscribe event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()

        subtest.on("subscribed",callback)
        fakeNotificationStream.emit("notification",{notification: {sequence: 0,addedResults:{},removedResults:{} }})

        assert(callback.called,"subscribe event not fired")
        

    });
    it('should fire subscribed and notification event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()

        subtest.on("subscribed",callback)
        subtest.on("notification",callback)
        fakeNotificationStream.emit("notification",{notification: {sequence: 0,addedResults:{},removedResults:{} }})

        assert(callback.calledTwice,"subscribe event not fired")

    });

    it('should fire just added event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()
        let removedCallback = sinon.spy()

        subtest.on("added",callback)
        subtest.on("removed", removedCallback)
        fakeNotificationStream.emit("notification",{notification :{sequence: 0,addedResults:{data:"something"},removedResults:{} }})

        assert(callback.called,"Added event not fired")
        assert(!removedCallback.called,"Removed event fired")

    });
    it('should fire just removed event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()
        let removedCallback = sinon.spy()

        subtest.on("added",callback)
        subtest.on("removed", removedCallback)
        fakeNotificationStream.emit("notification", {notification : { sequence: 0, addedResults: {}, removedResults: { data: "something"} }})

        assert(!callback.called,"Added event fired")
        assert(removedCallback.called,"Removed not event fired")

    });
    it('should fire both added and removed event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()
        let removedCallback = sinon.spy()

        subtest.on("added",callback)
        subtest.on("removed", removedCallback)
        fakeNotificationStream.emit("notification", {notification: { sequence: 0, addedResults: { data: "something"}, removedResults: { data: "something"} }})

        assert(callback.called,"Added event not fired")
        assert(removedCallback.called,"Removed not event fired")
    });
    it('should fire unsubscribed event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()
        let removedCallback = sinon.spy()

        subtest.on("unsubscribed",callback)
        subtest.unsubscribe()
        
        assert(fakeNotificationStream.send.called,"Unsubscribe not called")

        fakeNotificationStream.emit("notification", {unsubscribed: { }})

        assert(callback.called,"Unsubscribe not called")
        assert(fakeNotificationStream.close.called, "Uderlining streaming not closed after succes unsubscribe")
        assert.equal(subtest.listenerCount(),0, "Subscription listeners are not cleaned")
    });
    it('should fire error event', () => {
        let subtest = new Subscription("any",connection,"test")
        let callback = sinon.spy()
        let removedCallback = sinon.spy()

        subtest.on("error",callback)
        subtest.unsubscribe()
        
        
        fakeNotificationStream.emit("notification", {error: { }})
        
        assert(callback.called,"Unsubscribe not called")
        assert(!fakeNotificationStream.close.called, "Uderlining streaming closed after error")
        assert(fakeNotificationStream.send.called,"Unsubscribe not called after error event")
        assert.equal(subtest.listenerCount(), 0, "Subscription listeners are not cleaned")

    });
    it('should generate an alias', () => {
        let subtest = new Subscription("any",connection)

        assert(subtest.alias,"Alias undefined")
        //TODO: check this
        connection.notificationStream.calledWith({ sparql: "any", alias: subtest.alias })

    });
    it('should generate two aliases', () => {
        let subtest1 = new Subscription("any",connection)
        let subtest2 = new Subscription("any",connection)

        assert.notEqual(subtest1.alias,subtest2.alias,"Aliases are equal")

    });
});