import defaults, { SEPAConfig } from './defaults';
import SEPA from './sepa';
import Bench, { ForcedBindings } from './querybench';

type QueryOrUpdateDefinition = { sparql: string, forcedBindings?: ForcedBindings }

export type JSAPConfig = SEPAConfig & { namespaces?: Record<string, string>, extended?: Record<string, any>, updates?: Record<string, QueryOrUpdateDefinition>, queries?: Record<string, QueryOrUpdateDefinition> };

export type Bindings = Record<string, string | number | boolean | Array<string | number | boolean>>

function trasformBindings(bindings: Bindings = {},forcedBindings: ForcedBindings={}) {
  let result = {}
  Object.keys(bindings).forEach(k => {
    result[k] = {
      value : bindings[k],
      type : forcedBindings[k] ? forcedBindings[k].type : "literal"
    }
  })
  result = Object.assign(forcedBindings,result)
  return result
}



class Jsap {
  namespaces: Record<string, string>;
  extended: Record<string, any>;
  updates: Record<string, any>;
  queries: Record<string, any>;
  
  #api: SEPA;
  #bench: Bench;

  /**
   * Create a JSAP interface to interact with other Agents in 
   * your space. 
   * @param config 
   * @param client Override the default SEPA client.
   */
  constructor(config: JSAPConfig | string  = {}, client?: SEPA ) {
    if (typeof config === 'string') {
      // TODO: validate the parsed JSON
      config = JSON.parse(config) as JSAPConfig
    }

    let parameters = (({ host, sparql11protocol, sparql11seprotocol }) => (prune({ host, sparql11protocol, sparql11seprotocol })))(config)
    parameters = Object.assign({},defaults,parameters)
    
    Object.assign(this, parameters)
    
    this.namespaces = config.namespaces ? config.namespaces : {}
    this.extended   = config.extended   ? config.extended : {}
    this.updates    = config.updates    ? config.updates : {}
    this.queries    = config.queries    ? config.queries : {}
    
    this.#api   = client == null ? new SEPA(parameters) : client;
    this.#bench = new Bench(this.namespaces)

    Object.keys(this.updates).forEach(k =>{
      this[k] = binds => {
        return this.update(k,binds)
      }
    })
    Object.keys(this.queries).forEach(k =>{
      if(this[k]){
        delete(this[k])
      }else{
      this[k] = (binds) => {
         return this.subscribe(k,binds)
        }
      this[k].query = binds => {
          return this.query(k, binds)
        }
      }
    })
  }

  public query(key: string, bindings: Bindings = {}){
    let query = this.queries[key].sparql
    let binds = trasformBindings(bindings, this.queries[key].forcedBindings)
    let config = (({ host, sparql11seprotocol }) => (prune({ host, sparql11seprotocol })))(this.queries[key])

    query = this.#bench.sparql(query, binds)

    return this.#api.query(query, config)
  }

  public subscribe(key: string,bindings: Bindings={}){
    let query = this.queries[key].sparql
    let binds = trasformBindings(bindings,this.queries[key].forcedBindings)
    let config = (({ host, sparql11seprotocol }) => (prune({ host, sparql11seprotocol })))(this.queries[key])
    
    query = this.#bench.sparql(query,binds)
    
    return this.#api.subscribe(query,config)
  }

  public update(key: string,bindings: Bindings = {}){
    let update = this.updates[key].sparql
    let binds  = trasformBindings(bindings,this.updates[key].forcedBindings)   
    let config = (({ host, sparql11protocol }) => (prune({ host, sparql11protocol })))(this.updates[key])

    update = this.#bench.sparql(update,binds)

    return this.#api.update(update,config);
  }

  public producer(key: string){
    return binds => { this.update(key,binds)}
  }

  public consumer(key: string){
    return binds => { this.subscribe(key,binds)}
  }

  public get Producers(){
    let result = {}
    Object.keys(this.updates).forEach(k =>{
      result[k] = binds => {
        return this.update(k,binds)
      }
    })
    return result;
  }

  public get Consumers(){
    let result = {}
    Object.keys(this.queries).forEach(k =>{
      result[k] = (binds) => { return this.subscribe(k,binds)}
    })
    return result;
  }
}
/**
 * Removes undedfined properties
 * @param {Object} obj 
 */
function prune(obj) {
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const element = obj[key];
      
      if(!element){
        delete(obj[key])
      }
    }
  }
  return obj;
}

type Tail<T> = T extends (ignored: infer _, ...args: infer P) => infer ReturnType ? (...args:P) => ReturnType : never
type DynamicJsap = new <T extends JSAPConfig>(attr: T) => Jsap & Record<keyof T["updates"], Tail<Jsap["update"]>> & Record<keyof T["queries"], (Tail<Jsap["subscribe"]> & { query: Tail<Jsap["query"]> })>;

export default Jsap as DynamicJsap;

