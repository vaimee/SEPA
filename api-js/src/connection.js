const EventEmitter = require("events").EventEmitter
const WebSocket = require("isomorphic-ws")

class Connection extends EventEmitter {

    constructor(uri, wsFactory) {
        super()
        this.__wsFactory = wsFactory
        this.ws = wsFactory(uri)
        this.__connectedClients = 0
    }

    notificationStream(request) {
        let json = JSON.stringify(request)
        
        const connect = (async () => {
            return new Promise(( resolve, reject )=> {
                if (this.ws.readyState === WebSocket.CONNECTING) {
                    let callback = (() => {
                        this.ws.removeEventListener("open", callback)
                        this.ws.send(json)
                        resolve();
                    }).bind(this)

                    this.ws.addEventListener("open", callback)
                    this.ws.addEventListener("error", (err) => {
                        reject(err)
                    })
                } else {
                    this.ws.send(json)
                    resolve();
                }
            })
        }).bind(this)

        let reconnect = (() => {
            let timeout = setTimeout(() => {
                this.ws = this.__wsFactory(this.ws.url)
                connect().then(() => {
                        clearTimeout(timeout)
                        this.ws.addEventListener("close", this.__closeCallback)
                        this.emit("reconnected", this);
                    }).catch((err) => {
                        reconnect();
                    })
            }, 1000);
        }).bind(this)

        this.__closeCallback = () => {
            this.emit("connection-lost")
            this._connectedClients = 0
            reconnect()
        }

        this.ws.addEventListener("close", this.__closeCallback)

        //Wait next tick to send subscriptions and let the client
        //subscribe to the notifications
        connect().catch((err) => {
            this.emit("error", err)
        })

        return new NotificationStream(this,request.subscribe.alias)
    }
    
    get connectedClients() {
        return this._connectedClients
    }
    
    set _connectedClients(value){
        this.__connectedClients = value
        if(value <= 0){
            // do not handle server close event because we are willing to close the connection
            this.ws.removeEventListener("close",this.__closeCallback)
            // Just wait gracefully for the server to close the connection
            this.ws.addEventListener("close", ()=> { this.emit("close")})
            this.ws.close()
        }
    }

    get _connectedClients ()  {
        return this.__connectedClients
    }

}

class NotificationStream extends EventEmitter{

    constructor(connection,alias) {
        super()
        this.alias = alias
        this._reconnectListener = this._setUpConnection.bind(this)
        this._dataHandler = data => {
            try {
                data = JSON.parse(data.data)
                data["toString"] = () => { return JSON.stringify(data) }

                if (data.notification &&
                    data.notification.alias &&
                    data.notification.alias === this.alias) {
                    this.spuid = data.notification.spuid
                }

                if ((data.error && data.alias === this.alias) ||
                    (data.notification && data.notification.spuid === this.spuid) ||
                    (data.unsubscribed && data.unsubscribed.spuid === this.spuid)) {
                    this.emit("notification", data)
                }
            } catch (error) {
                this.emit("error", error)
            }
        }
        this._setUpConnection(connection);
    }

    _setUpConnection(connection){
        connection.removeListener("reconnected", this._reconnectListener)
        connection.on("reconnected", this._reconnectListener);

        this.ws = connection.ws
        this._connection = connection
        this._connection._connectedClients++
        
        this.ws.removeEventListener("message", this._dataHandler)
        this.ws.addEventListener("message", this._dataHandler)        
    }

    send(data){
        //TODO: test send
        this.ws.send(JSON.stringify(data))
    }
    
    close(){
        this.ws.removeEventListener("message", this._dataHandler)
        this._connection._connectedClients--
    }

}

module.exports = Connection