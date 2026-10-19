# Subscriptions on live graphs only (graph policy)

A subscription is an SPU that re-runs its query after every update touching
its graphs. That is right for graphs holding the **current state** (a field's
profile, the latest weather observation, today's forecast), small and
changing. It is wrong for **history** graphs, which only grow and never change
what they already hold: a subscription there re-runs a query over a growing
graph at each insert, and gains nothing, since past data does not change.
History is read with queries.

The engine enforces this split when the JSAPs it is given declare it.

## Declaring the graphs

Each application declares its graphs in a `graphPolicy` section of its JSAP,
as regular expressions matched against the whole graph IRI:

```json
"graphPolicy": {
  "description": "Agorà graphs: a field graph holds the current profile, soil, crop and plans of a unit; the catalogues are live; the irrigations a farmer entered are history (the latest is in <unit>_irrigations-live).",
  "live":    [ ".+_field", "https://vaimee\\.com/criteria/(soils|crops)#(public|user-[A-Za-z0-9_-]+)" ],
  "history": [ ".+_irrigations" ]
}
```

and the engine reads the sections of every JSAP it is started with
(comma separated, joined):

```sh
java -jar engine.jar -jsap /jsap/mdw.jsap,/jsap/agora.jsap
```

- JSAPs without `graphPolicy` add nothing.
- With no section at all the policy is off and every subscription is accepted
  (as on `master`); the engine logs it at startup.
- Only the `graphPolicy` section is read: the other sections, connection
  settings and credentials, are ignored. Agorà's image copies only that
  section of each JSAP into `/jsap`.

## The rule

A subscription is accepted only if **every graph its query reads** matches a
`live` pattern and none matches a `history` pattern. Otherwise the
subscribe request is answered:

```json
{"error": "subscription_not_allowed", "status_code": 403,
 "error_description": "Subscriptions are allowed on live graphs only: <reason>"}
```

| Graphs read | Result | Reason given |
| --- | --- | --- |
| `GRAPH <g>` with `g` live | accepted | |
| `GRAPH <g>` with `g` history | refused | `<g> is a historical graph: read it with a query` |
| `GRAPH <g>` with `g` in no list | refused | `<g> is not declared live in the graph policy` |
| patterns outside `GRAPH` (default graph) | refused | `the default graph is not a live graph: use GRAPH <live graph>` |
| `GRAPH ?g` | refused | `a variable graph (GRAPH ?g) can read historical graphs: name the live graphs` |
| `GRAPH ?g` with `VALUES ?g { <a> <b> }` | as if `GRAPH <a>`, `GRAPH <b>` | |

Queries and updates are never restricted by the policy.

## How the graphs of a query are found

`GraphPolicy.graphsOf` compiles the query to the algebra in quad form and
walks it, so every graph is found wherever it is: `OPTIONAL`, `UNION`,
`MINUS`, subqueries, `FILTER EXISTS / NOT EXISTS` (read as default graph when
inside `GRAPH`, which can only refuse more). Then:

- **Variable graphs bound by `VALUES`** (`df061c95`): a `GRAPH ?g` whose
  variable gets an IRI in *every* row of a `VALUES` table reads exactly those
  IRIs. This is how clients subscribe to several channels with one query
  (multiple forced bindings become a `VALUES` block, see
  [jsap-forced-bindings.md](jsap-forced-bindings.md#multiple-bindings-values)).
  A row with `UNDEF` or a literal leaves the variable free: refused.
- **`FROM`** replaces the default graph with the graphs it names;
  **`FROM NAMED`** limits `GRAPH ?g` to the graphs it names.
- Queries that do not parse are left to the SPU initialisation, which reports
  the parse error as before.

## Consequences for clients

- Name the live graph in the subscription (`GRAPH <...>`), or bind the graph
  variable: a JSAP subscription with `GRAPH ?graph` must get a value for
  `graph`, or several values (`VALUES`). An unbound graph variable, which a
  query accepts (see optional bindings), is refused in a subscription.
- Read history with queries, e.g. the CRITERIA agent reads past weather from
  `<unit>_mdw-weather-history` with a query and subscribes only to
  `<unit>_mdw-weather-live`.
- Keep a small *live* companion of a history graph when clients must react to
  new entries (`<unit>_irrigations-live` holds the latest irrigation).

## Code and tests

- `engine/.../processing/subscriptions/GraphPolicy.java`: `load(jsaps)`,
  `check(sparql)`, `graphsOf(sparql)`.
- `Engine` loads the policy at startup (`GraphPolicy.load(properties.getJsap())`);
  `SPUManager` calls `GraphPolicy.check` before creating the SPU.
- `EngineProperties`: `-jsap <file[,file...]>`.
- `GraphPolicyTest`: `findsGraphsWhereverTheyAre` (`OPTIONAL`, `UNION`,
  `NOT EXISTS`, `VALUES` with and without `UNDEF`, `FROM`, `FROM NAMED`),
  `allowsSubscriptionsOnLiveGraphsOnly`, `joinsThePoliciesOfSeveralJsaps`,
  `withoutAPolicyEverythingIsAllowed`.
