const assert = require('assert');
const Connection = require('../build/connection');
const WebSocket = require("ws")
const Emitter = require('events').EventEmitter

const sinon = require('sinon')
const clock = sinon.useFakeTimers()


describe('Connection', function () {
    let fakeWs
    let connection  

    beforeEach(() => {
        fakeWs = new Emitter()
        fakeWs.readyState = WebSocket.OPEN
        fakeWs.send = sinon.fake()
        fakeWs.close = sinon.fake(() => {
            fakeWs.emit('close')
        })

        // broswer methods
        fakeWs.addEventListener = fakeWs.addListener
        fakeWs.removeEventListener = fakeWs.removeListener

        connection = new Connection("fakeuri",() => fakeWs)
        fakeWs.send.resetHistory()
        fakeWs.close.resetHistory()
    });
    describe('Notification stream', () => {
        it('should notify the first notification', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)  
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"test"}}' })
            
            assert(callback.calledOnce, "Callaback not called")
        });
        
        it('should polately accept bad json formatting', () => {
            let callback = sinon.fake()
            let errCallback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)
            notStrem.on("error", errCallback)  
            fakeWs.emit("message", { data: '{"notification" bad json here :{"spuid":"spuid://test","alias":"test"}}' })
            
            assert(!callback.calledOnce, "Data callback shouldn't be called")
            assert(errCallback.calledOnce, "Error callaback not called")

        });

        it('should notify the every notification', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)
            
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            
            assert(callback.calledThrice, "Notifications are not sent")
        });

        it('should discard other notifications on subscription', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)
            
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"not-test"}}' })
            
            assert(!callback.called, "Callaback called for wrong alias notification")
        });

        it('should discard other notifications interlived in streaming', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)
            
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://not-test","alias":"not-test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://not-test"}}' })
            
            assert(callback.calledThrice, "Callaback called for wrong alias notification")
        });

        it('should notify both clients', () => {
            let callback1 = sinon.fake()
            let callback2 = sinon.fake()

            let notStream1 = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })
            let notStream2 = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'not-test'
                }
            })

            notStream1.on("notification", callback1)
            notStream2.on("notification", callback2)
            
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://not-test","alias":"not-test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test"}}' })
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://not-test"}}' })
            
            assert.equal(callback1.callCount,3, "First callback not called")
            assert.equal(callback2.callCount,2, "Second callback not called")
        });

        it('should notify the error notification', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)

            fakeWs.emit("message", {data: '{"error": "unauthorized_client","error_description": "Client is not authorized","status_code": 401,"alias":"test"}' })
            assert(callback.calledOnce, "Callaback not called")
        });

        it('should notify the unsubscribe notification', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)

           
            fakeWs.emit("message", { data: '{"notification":{"spuid":"sepa://test","alias":"test"}}' })
            callback.resetHistory()
            fakeWs.emit("message", {data: '{"unsubscribed":{"spuid": "sepa://test"}}' })
            assert(callback.calledOnce, "Callaback not called after unsubscribe")
        });

        it('should close the notification stream', () => {
            let callback = sinon.fake()

            let notStrem = connection.notificationStream({
                subscribe:
                {
                    sparql: "fake",
                    alias: 'test'
                }
            })

            notStrem.on("notification", callback)

            notStrem.close()
            fakeWs.emit("message", { data: '{"notification":{"spuid":"spuid://test","alias":"test"}}' })
            assert(!callback.calledOnce, "Callaback called on notification")
            fakeWs.emit("message", { data: '{"error":{}}' })
            assert(!callback.calledOnce, "Callaback called on error")

            assert.equal(fakeWs.listenerCount("message"),0,"Web socket listeners are still connected")
        });
    });

    it('should close the underling websocket connection', () => {
        let callback = sinon.fake()
        let closeCallback = sinon.fake()

        connection.on("test", callback)
        connection.on("close",closeCallback)

        let notStream = connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })

        notStream.close()

        assert(!callback.called, "Callaback callled")
        assert(fakeWs.close.calledOnce, "Connection not closed")
        
        assert(closeCallback.called,"close callback not called")

    });

    it('should reconnect if server closes the connection', async () => {
        let reconnectCallback = sinon.fake()
        let connectionLostCallback = sinon.fake()

        connection.on("reconnect", reconnectCallback)
        connection.on("connection-lost", connectionLostCallback )

        let notStream = connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })


        // Pretend that server closed the connection
        fakeWs.emit("close")

        assert(connectionLostCallback.calledOnce, "Connection lost callback not called")
        await clock.tick(1000)

        assert(reconnectCallback, "recconect callback not called")

    });
    
    it('should have the correct number of connected clients', () => {
        let callback = sinon.fake()
        let closeCallback = sinon.fake()

        connection.on("test", callback)
        connection.on("close",closeCallback)

        let notStream1 = connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })
        let notStream2 = connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })

        let notStream3 = connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })

        assert.equal(connection.connectedClients,3,"Wrong connect client number")
        
        notStream1.close()

        assert.equal(connection.connectedClients, 2, "Wrong connect client number after closing one")

    });

    it('should send the subscription after socket opening', (done) => {
        fakeWs.readyState = WebSocket.CONNECTING
        
        connection.notificationStream({
            subscribe:
            {
                sparql: "fake",
                alias: 'test'
            }
        })
        
        assert(!fakeWs.send.called,"send called before opening")
        
        process.nextTick(() => {
            fakeWs.emit("open")
            assert(fakeWs.send.called, "send called before opening")
            done()
        })

    });

    it('should send the two subscription after socket opening', (done) => {
        fakeWs.readyState = WebSocket.CONNECTING
        

        connection.notificationStream({
            subscribe:
                {
                    sparql : "fake",
                    alias : 'test'
                }
        })
        connection.notificationStream({
            subscribe:
                {
                    sparql : "fake",
                    alias : 'test'
                }
        })

        assert(!fakeWs.send.called,"send called before opening")
        process.nextTick(() => {
            fakeWs.emit("open")
            assert(fakeWs.send.calledTwice, "send called before opening")
            done()
        })
    });

    afterEach(() => {
        fakeWs.readyState = WebSocket.OPEN
    });
    
});
