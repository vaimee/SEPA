import { EventEmitter } from "events"
const partial = require("util").partial

// EVENTS
const NOTIFICATION = "notification"
const ADDED        = "added"
const REMOVED      = "removed"
const SUBSCRIBED   = "subscribed"
const ERROR        = "error"
const UNSUBSCRIBED = "unsubscribed"
const CONNECTERROR = "connection-error"

export default class Subscription extends EventEmitter {
    private _connection: any
    private _unsubscribed: boolean
    private _query: string
    private _alias: string
    private _stream: any
    options: any
    
    constructor(query: string,connection,alias?: string,options?: any){
        super()
        this._connection = connection
        this._unsubscribed = false;
        this._query = query
        //Math random ensure that the alias is unique but is not secure (cripto)
        this._alias = alias ? alias : Math.random().toString(26).slice(2)
        
        let handler = ((notification) => {
            if (notification.unsubscribed) {
                this._stream.close()
                this.emit(UNSUBSCRIBED, notification)
                this.removeAllListeners()
                this.setMaxListeners(0)
                this._unsubscribed = true;
            } else if (notification.error) {
                this.emit(ERROR, notification)
            } else {
                notification = notification.notification
                if(notification.sequence === 0){
                    this.emit(SUBSCRIBED,notification)
                }
                this.emit(NOTIFICATION, notification)
                if (Object.keys(notification.addedResults).length)    this.emit(ADDED, notification.addedResults)
                if (Object.keys(notification.removedResults).length)  this.emit(REMOVED, notification.removedResults)
            }
        })
        this.options = options ? options : {}
        let data = {
            subscribe: {
                sparql: query,
                alias: this._alias
            }
        }
        
        data.subscribe = Object.assign(data.subscribe,this.options)
        this._stream = connection.notificationStream(data)

        this._stream.on("notification", handler.bind(this))

        this._stream.on("error", ((err) => {
            this._stream.close()
            this.emit(CONNECTERROR,err)
            this.removeAllListeners()
            this.setMaxListeners(0)
            this._unsubscribed = true;
        }))
    }

    get alias() {
        return this._alias
    }

    get query(){
        return this._query
    }
    
    unsubscribe(){
        if(this._unsubscribed){
            throw new Error("Subscription already unsubscribed")
        }
        let unsubMessage = { unsubscribe: { spuid: this._stream.spuid, alias: this.alias } }
        Object.assign(unsubMessage.unsubscribe,this.options)
        this._stream.send(unsubMessage)
    }

    kill(){
        this._stream.close()
    }
}