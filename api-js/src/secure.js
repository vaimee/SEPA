const SEPA = require("./sepa").default;
const EventEmitter = require('events').EventEmitter
const axios = require('axios')
const WebSocket = require('isomorphic-ws');
const defaults = require('./defaults').default
const merge = require("./utils").mergeWithDefaults

class SecureSEPA extends SEPA {

    constructor(clientID,clientSecret,parameters){
        super(parameters)
        this.clientID = clientID
        this.clientSecret = clientSecret
        super._wsFactory = (uri) => {
            if (this.config && this.config.options && this.config.options.httpsAgent){
                return new WebSocket(uri, undefined,{ agent: this.config.options.httpsAgent })
            }
            // use default wss agent
            return new WebSocket(uri)
        }
        this.webToken = undefined
        this.maxTokenRetries = 2
    }
    
    async query(query, config){
        
        if(!this.webToken){
            this.webToken = await this._retriveToken()
        }

        config = this._setHeaders(config)

        return this._autoRenewToken(super.query.bind(this),query,config,0)
    }

    async update(update, config){
        if (!this.webToken) {
            this.webToken = await this._retriveToken()
        }

        config = this._setHeaders(config)
        return this._autoRenewToken(super.update.bind(this), update, config, 0)
    }

    subscribe(query, config, alias){
        
        let auth = this.webToken.token_type.charAt(0).toUpperCase() + this.webToken.token_type.slice(1) + " " + this.webToken.access_token

        if(config){
            if(config.options){
                config.options.authorization = auth
            }else{
                config.options = {authorization : auth}
            }            
        }else{
            config = {options: {authorization: auth}}
        }

        
        
        
        return new SecureSubscriptionWrapper(super.subscribe(query,config,alias),config,this)
    }

    async login(){
        this.webToken = await this._retriveToken()
    }

    _retriveToken(){
        let credentials = Buffer.from(this.clientID+":"+this.clientSecret).toString("base64")
        return axios.post(this.config.oauth.tokenRequest, "", {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
                Authorization : "Basic "+credentials
            },
           httpsAgent: this.config.options.httpsAgent
        }).then((response) => {
            return response.data.token
        })
    }

    _autoRenewToken(oper,arg,config,callTimes){
        if(callTimes >= this.maxTokenRetries){
            return Promise.reject("Max token regeneration requests")
        }

        return oper(arg,config).catch((e) => {
            // Supports also old versions of SEPA that returns 400 instead of 401
            // for unauthorized requests 
            if (e.response && (e.response.status === 400 || e.response.status === 401)) {
                return this._retriveToken().then((token) => {
                    this.webToken = token
                    config = this._setHeaders(config)
                }).then(() => this._autoRenewToken(oper, arg, config,++callTimes))
            }
            return Promise.reject(e)
        })
    }

    _setHeaders(clientConfig){ 
        let config = Object.assign(this.config,clientConfig)
        let auth = this.webToken.token_type.charAt(0).toUpperCase() + this.webToken.token_type.slice(1) + " " + this.webToken.access_token
        if (!config) {
            config = {
                options: {
                    headers: {
                        Authorization: auth
                    }
                }
            }
        } else if (config && !config.options) {
            config.options = {
                headers: {
                    Authorization: auth
                },
                
            }
        } else if (config && config.options && !config.options.headers) {
            config.options.headers = {
                Authorization: auth
            }
        } else {
            config.options.headers.Authorization = auth
            
        }
        
        return config
    }

    static defaultClient(clientID, clientSecret,config = {}){
        config = merge(defaults, config)
        return new SecureSEPA(clientID,clientSecret,config)
    }

    static register(clientID, config = {}) {
        config = merge(defaults, config)
        let body = {
            register: {
                client_identity: clientID,
                grant_types: ["client_credentials"]
            },
            httpsAgent: config.options.httpsAgent
        }
        return axios.post(config.oauth.register, JSON.stringify(body), {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            httpsAgent: config.options.httpsAgent
        }).then((res) => {
            config = Object.assign({}, config)

            let clientID = res.data.credentials.client_id
            let clientSecret = res.data.credentials.client_secret

            config.sparql11protocol.protocol = "https"
            config.sparql11protocol.query.path = "/secure/query"
            config.sparql11protocol.update.path = "/secure/update"
            config.sparql11protocol.port = 8443
            config.sparql11seprotocol.protocol = "wss"

            return new SecureSEPA(clientID, clientSecret, config)
        })
    }

    
}

class SecureSubscriptionWrapper extends EventEmitter{

    constructor(sub,config, secureClient){
        super()
        this._config = config
        this._secureClient = secureClient
        this._forwardSubEvents(sub)
        this._unsubcribing = false
    }

    emit(name,object){
        if (name === "error" && object.error && (object.status_code === 400 || object.status_code === 401) && !this._unsubcribing){
            this._secureClient._retriveToken().then(token => {
                this._secureClient.webToken = token
                this._sub.kill()
                return this._secureClient.subscribe(this.query, this._config,this.alias)
            }).then(sub => {
                this._forwardSubEvents(sub)
            })
        } else if (name === "error" && object.error && (object.status_code === 400 || object.status_code === 401) && this._unsubcribing){
            this._secureClient._retriveToken().then(token => {
                let auth = token.token_type.charAt(0).toUpperCase() + token.token_type.slice(1) + " " + token.access_token
                this._sub.options.authorization = auth
                this._secureClient.webToken = token
                this.unsubscribe()
            })
        }else{
            super.emit(name,object)
        }
    }

    get alias (){
        return this._sub.alias
    }

    get query() {
        return this._sub.query
    }

    unsubscribe(){
        this._unsubcribing = true
        this._sub.unsubscribe()
    }

    kill(){
        this._sub.kill()
    }

    _forwardSubEvents(sub){
        this._sub = sub

        let forward = name => {
            return (event => {
                this.emit(name,event)
            }).bind(this)
        }

        sub.on("subscribed", forward("subscribed"))
        sub.on("unsubscribed", forward("unsubscribed"))
        sub.on("notification", forward("notification"))
        sub.on("added", forward("added"))
        sub.on("removed",forward("removed"))
        sub.on("error", forward("error"))
        sub.on("connection-error", forward("connection-error"))
    }

    

}
module.exports = SecureSEPA