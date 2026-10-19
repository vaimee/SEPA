# JSAP forced bindings

A JSAP entry is a SPARQL template plus the variables a client may set
(`forcedBindings`). The client replaces each bound variable with its value
before sending the request; the engine never sees the bindings.

```json
"https://agora.vaimee.com/jsap/query/getDailyWeatherPlan": {
  "sparql": "SELECT * WHERE { GRAPH ?graph { ?plan a agora:DailyWeatherPlan ; agora:bulkInit ?performInitialUpdate . OPTIONAL { ?plan agora:forecastDays ?forecastDays } } }",
  "forcedBindings": {
    "graph": { "type": "uri" },
    "plan":  { "type": "uri" }
  }
}
```

## Declaring a binding

| Key | Meaning |
| --- | --- |
| `type` | `uri`, `literal` or `bnode` |
| `datatype` | for literals: an XSD datatype, as prefixed name (`xsd:dateTime`) or IRI |
| `language` | for literals: a language tag |
| `value` | **default value**, used when the client does not set the variable |

A binding with a `value` is never free: the default applies whenever the
client sets nothing. A binding **without `value` is optional**: if the client
does not set it, the variable stays a variable.

## Optional bindings

When an optional binding is not set, nothing is replaced and the variable
keeps matching anything. One entry then serves several requests:

| Bindings set | `getDailyWeatherPlan` returns |
| --- | --- |
| `graph`, `plan` | that plan |
| `graph` | every plan of that graph |
| none | every plan of every graph the caller may read (the ACL filters `GRAPH ?graph`) |

Rules for writing optional bindings:

- **Queries**: just leave the variable in the template. Combine with
  `OPTIONAL { ... }` for properties that may be missing in the data (an unset
  binding and a missing triple are different things: the first widens the
  match, the second needs `OPTIONAL`).
- **Subscriptions**: the graph variable must be bound, with one value or
  several (`VALUES`): an unbound `GRAPH ?graph` is refused by the graph policy
  ([graph-policy.md](graph-policy.md)). Other variables may stay free.
- **Updates**: `INSERT DATA` / `DELETE DATA` must be fully bound (a variable
  left there is a syntax error). For an optional value use
  `INSERT { ... } WHERE { ... }`: SPARQL drops the template triples whose
  variables are unbound, so the triple is written only when the value is set:
  ```sparql
  INSERT { GRAPH ?graph { ?plan criteria:saveDate ?saveDate } } WHERE { }
  ```
  With `DELETE { ... } INSERT { ... } WHERE { OPTIONAL { ... } }` the same
  entry can replace a value that may or may not exist (`setParameter` in
  `agora.jsap`).
- **Typed values from strings**: when a client cannot attach the datatype
  (sepa-js, below), bind the lexical form and type it in the template:
  `BIND(STRDT(STR(?date), xsd:dateTime) AS ?d)`.

How each client leaves a binding free:

| | Java (`api-java`) | JavaScript (`@vaimee/sepa-js`, `api-js`) |
| --- | --- | --- |
| Free when | the binding has no value (`null`): `Namespaces.replaceBindings` skips it | the value is `null` or `undefined`: `querybench` does not replace it |
| JSAP default `value` | applied when the client sets nothing (`Client.addDefaultDatatype` starts from the JSAP template) | applied: the JSAP definition is the starting object |
| **Value given in an earlier call** | kept by the `Producer` / `Consumer` instance that set it (`setUpdateBindingValue`, `setSubscribeBindingValue`) until it is changed; set it to `null` to free it again. Other instances start from the JSAP template | **kept in the JSAP**: `trasformBindings` writes the values into the entry's `forcedBindings` object (`Object.assign(forcedBindings, result)`), so the next call inherits them |

The last row matters for optional bindings: once a variable was set,
omitting it later does **not** free it. Set it explicitly to `null`, or set
every binding of the entry on every call (what the Agorà API does). With
sepa-js this is not limited to one object: the JSAP config is shared by all
`Jsap` instances built from it, so a value set by one request is inherited by
the next request on any instance.

## Substitution rules

| Binding | Java | JavaScript |
| --- | --- | --- |
| `uri` | `<value>` if the IRI is hierarchical (`http://...`) or `urn:`; otherwise left as is, i.e. read as a prefixed name (`agora:Field`) | prefixed name if it looks like one, else `<value>` |
| `literal`, no datatype | `'''value'''`, escaped (`StringEscapeUtils.escapeJava`) | `'value'`, **not escaped**: the caller must escape `\ ' " \n \r` |
| `literal`, numeric or boolean datatype (`xsd:integer`, `xsd:decimal`, `xsd:double`, `xsd:boolean`) | the bare value, not checked | — |
| `literal`, other datatype | `'''value'''^^datatype` | datatype ignored: `'value'` (use `STRDT` in the template) |
| `literal`, number or boolean JS value | — | the bare value |
| `bnode` | `_:label`, or `<value>` | — |

Variables are matched as whole names (`?plan` does not touch `?planned`),
both `?var` and `$var`.

Caveats:

- Values are inserted as text: never bind unchecked input to `uri` (an IRI
  with `>` or spaces breaks the query) and escape literals in sepa-js.
- A non-hierarchical IRI such as `uid:...` or `mailto:...` is left unwrapped
  by the Java client and parsed as a prefixed name: declare the prefix or
  avoid such IRIs.

## Multiple bindings (VALUES)

Several values for the same variables become a `VALUES` block at the start
of the query, so one query or subscription covers several channels:

```sparql
SELECT * WHERE { VALUES (?graph ?plan) { (<g1> <p1>) (<g2> <p2>) } GRAPH ?graph { ... } }
```

- sepa-js: give an array as the value (`{ graph: [g1, g2] }`); queries only.
- Java: `MultipleForcedBindings` (`Producer.setUpdateMultipleBindings`, and
  in the MDW agents' JSAP extension `getQueryMultipleBindings`), as the MDW
  agents bind every deployment of an agent in one subscription.
- The graph policy reads the graphs bound by `VALUES`, so such subscriptions
  are accepted when every value is a live graph.
