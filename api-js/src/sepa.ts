import axios from 'axios';
import WebSocket from 'isomorphic-ws';
import { SEPAConfig } from './defaults';
import Subscription from './subscription';
const utils = require('./utils');
const Connection = require('./connection')


export default class SEPA {
  private _config: SEPAConfig;
  private queryURI: string;
  private updateURI: string;
  private connectionPool: Map<string, WebSocket>;
  private subscribeURI: string;
  private _wsFactory: (uri: string) => WebSocket;

  public get config() {
    return this._config
  }

  constructor(parameters: SEPAConfig) {
    this._config = parameters
    this.queryURI = utils.createURI(parameters.sparql11protocol.protocol, parameters.host, parameters.sparql11protocol.port, parameters.sparql11protocol.query.path)
    this.updateURI = utils.createURI(parameters.sparql11protocol.protocol, parameters.host, parameters.sparql11protocol.port, parameters.sparql11protocol.update.path)

    this.connectionPool = new Map()

    let subprotcol = parameters.sparql11seprotocol.protocol
    let selectSubProtocol = parameters.sparql11seprotocol.availableProtocols[subprotcol]
    this.subscribeURI = utils.createURI(subprotcol, parameters.host, selectSubProtocol.port, selectSubProtocol.path)

    this._wsFactory = (uri) => {
      return new WebSocket(uri)
    } 
  }

  public query(query: string, config: SEPAConfig) {
    let q_uri = this.queryURI
    // clone the configuration
    let axiosConfig = mergeConfigurations({},this._config)
    if ( config !== undefined){
      axiosConfig = mergeConfigurations(this._config,config)
      q_uri = utils.createURI(axiosConfig.sparql11protocol.protocol, axiosConfig.host, axiosConfig.sparql11protocol.port, axiosConfig.sparql11protocol.query.path)
    }
    
    axiosConfig = setHeadersIfUndefined(axiosConfig, { "Content-Type": "application/sparql-query" })

    return axios.post(q_uri,query, axiosConfig.options).then(function(response) {
       return response.data;
    })
  }

  public update(update: string, config: SEPAConfig) {
    let up_uri = this.updateURI
    // clone the configuration
    let axiosConfig = mergeConfigurations({}, this._config)
    if (config !== undefined) {
      axiosConfig = mergeConfigurations(this._config, config)
      up_uri = utils.createURI(axiosConfig.sparql11protocol.protocol, axiosConfig.host, axiosConfig.sparql11protocol.port, axiosConfig.sparql11protocol.update.path)
    }
    axiosConfig = setHeadersIfUndefined(axiosConfig, { "Content-Type": "application/sparql-update" })
    return axios.post(up_uri, update, axiosConfig.options).then(function(response) {
      return {"status" : response.status,
              "statusText" : response.statusText
      };
    })
  }

  public subscribe (query: string, config: SEPAConfig, alias?: string) {
    let sub_uri = this.subscribeURI
    let subConfig = this._config
    if ( config !== undefined){
      subConfig = mergeConfigurations(this._config,config)
      // delete https agent if present
      subConfig.options && subConfig.options.httpsAgent && delete subConfig.options.httpsAgent
      
      let subprotcol = subConfig.sparql11seprotocol.protocol
      let selectSubProtocol = subConfig.sparql11seprotocol.availableProtocols[subprotcol]
      sub_uri = utils.createURI(subprotcol, subConfig.host, selectSubProtocol.port, selectSubProtocol.path)
    }

    let connection = this.connectionPool.get(sub_uri)
    
    if(!connection){
      connection = new Connection(sub_uri, this._wsFactory)
      this.connectionPool.set(sub_uri,connection)
      connection.on("close",(()=>{this.connectionPool.delete(sub_uri)}).bind(this))
      connection.on("error", ()=> { 
        /* Ignore the error, connection will try to reconnect automatically. 
           TODO: this is a bad pattern, we should design connection error handling better
           See https://github.com/arces-wot/SEPA-js/issues/36
        */
      })
    }
    let options = subConfig ? subConfig.options : {}
    return new Subscription(query,connection,alias,options)
  }
  
}

function setHeadersIfUndefined(localConfig,headers) {
  let config = Object.assign({},localConfig)
  if(!config.options) config.options = {headers : {}}
  if(!config.options.headers) config.options.headers = {}

  Object.keys(headers).forEach((key) => {
    if (!config.options.headers.hasOwnProperty(key)){
      config.options.headers[key] = headers[key]
    }  
  })
  return config
}

function mergeConfigurations(defaults: SEPAConfig, user: SEPAConfig){
  return utils.mergeWithDefaults(defaults,user)
}