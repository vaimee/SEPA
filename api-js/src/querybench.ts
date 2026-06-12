
export type URIBinding = {type: "uri", value: string | string[] | null}
export type LiteralBinding = {type: "literal", value: string | number | boolean | (string | number | boolean)[] | null, lang?: string, datatype?: string}
export type Binding = URIBinding | LiteralBinding
type NoArrayBinding = (Omit<URIBinding, "value"> & { value: string }) | (Omit<LiteralBinding, "value"> & { value: string | number | boolean })
export type ForcedBindings = Record<string, Binding> | Array<Record<string, Binding>>

export default class SPARQLbench {
  private namespaces: Record<string, string>;

  constructor(namespaces: Record<string,string> = {}){
    this.namespaces = namespaces
  }

  public addNameSpace(prefix: string,ns: string){
    this.namespaces[prefix] = ns
  }

  public removeNameSpace(prefix: string){
    delete this.namespaces[prefix]
  }

  public sparql(template: string, bindings: ForcedBindings){
    const prefixes = Object.keys(this.namespaces).map(k => {
      let pref = `PREFIX ${k}:<${this.namespaces[k]}>`
      return pref;
    }).join(" ")

    if(bindings instanceof Array){
      if (!this._isquery(template)){
        throw new Error("SPARQL Update not supported for multiple bindings")
      }

      const base = this._createValueTemplate(bindings)
      return template.replace("{","{"+this._createValues(base))
    }
    const valuesTemplate: {body: string[][], vars: string[]} = {
      body: [],
      vars: []
    }

    Object.keys(bindings).forEach(k =>{
      let search = new RegExp("(\\?|\\$)" + k +"(?![a-z]|[A-Z]|_|[0-9]|[\\u00D6-\\u06fa]|[\\u00D8-\\u00F6]|[\\u00F8-\\u02FF]|[\\u0370-\\u037D]|[\\u037F-\\u1FFF]|[\\u200C-\\u200D]|[\\u2070-\\u218F]|[\\u2C00-\\u2FEF]|[\\u3001-\\uD7FF]|[\\uF900-\\uFDCF]|[\\uFDF0-\\uFFFD]|[\\u1000-\\uEFFF])",'g')
      const value = bindings[k].value
      
      if(value instanceof Array){
        if(!this._isquery(template)) throw new Error("SPARQL Update not supported for multiple bindings");

        valuesTemplate.vars.push("?"+k)
        let values = value.map((val)=>{
          return this._transformValue({ value: val, type: bindings[k].type})
        })
        const index = valuesTemplate.vars.length -1
        for (let i = 0; i < values.length; i++) {
          if (!valuesTemplate.body[i]) valuesTemplate.body[i] = [];

          valuesTemplate.body[i][index] = values[i]
        }
       
      }else if(value !== undefined && value !== null){
        let replaceValue = this._transformValue(bindings[k] as NoArrayBinding)
        template = template.replace(search, replaceValue)
      }
    })

    template = template.replace("{", "{" + this._createValues(valuesTemplate))

    return (prefixes+" "+template).trim()
  }

  private _isquery(template: string){
    return template.toLowerCase().includes("select")
  }
 
  private _transformValue({ value, type }: NoArrayBinding){
    switch (type) {
      case "uri":
        const usingPrefix = /^([A-Z]|[a-z])(([A-Z]|[a-z]|_|-|[0-9]|\.)*([A-Z]|[a-z]|_|-|[0-9]))?:([A-Z]|[a-z]|_|[0-9])(([A-Z]|[a-z]|_|-|[0-9]|\.)*([A-Z]|[a-z]|_|-|[0-9]))?$/gm;
        return usingPrefix.test(value) ? value : "<" + value + ">"
      case "literal":
        switch (typeof value) {
          case "string":
            return "'" + value + "'"
          case "number":
          case "boolean":
            return value.toString();
          default:
            throw new Error("Invalid value type for literal binding");
        }
      default:
        return value
    }
  }

  private _createValueTemplate(bindings: Exclude<ForcedBindings, Record<string, Binding>>){
    let vars = []
    let body = []
    vars = Object.keys(bindings[0]).map(key => "?"+key)
    
    body = bindings.map(innerBindings =>{
      return Object.keys(innerBindings).map(key => {
        const binding = innerBindings[key];
        if(binding.value instanceof Array){
          throw new Error("Multiple bindings not supported inside array bindings")
        }
        return this._transformValue(binding as NoArrayBinding);
      })
    })

    return {body,vars}
  }

  private _createValues({ body, vars }: { body: string[][], vars: string[] }){
    if(body.length === 0 && vars.length ===0 ){
      return ""
    }

    const bodyString = body.map(values =>{
      return `(${values.join(" ")})` 
    }).join(" ")
    return `VALUES(${vars.join(" ")}){${bodyString}}`
  }
}
