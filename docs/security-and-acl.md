# Zitadel security and dataset ACL

With security on, every request (query, update, subscribe) must carry an
`Authorization: Bearer <access token>` issued by Zitadel, and SEPA checks
what the caller may read and write graph by graph with the Jena dataset ACL
(SJENAR). Permissions are changed at runtime, without restarting the engine,
through a JMX MBean that the Jolokia agent publishes over HTTP.

## 1. Authentication: Zitadel

`ZitadelSecurityManager` (`gates.security.type: "zitadel"`) validates the
token:

- **JWT access tokens**: signature with the issuer's keys
  (`<issuer>/oauth/v2/keys`, cached), required claims `exp`, `iss`, `sub`,
  issuer equal to `issuer`, and `audience` among the token's `aud` (the
  Zitadel project ID: tokens of every application of the project are accepted).
- **Opaque tokens**: introspection at `<issuer>/oauth/v2/introspect`, only if
  `introspectionClientId` and `introspectionClientSecret` are set; same
  checks on `aud` and `sub`.

The **ACL user** of a request is:

- the token's `sub` (the Zitadel user ID, also for service users with a JWT
  profile key);
- `DatasetACL.ADMIN_USER` when the token has the `admin` role of the project
  (claim `urn:zitadel:iam:org:project:<audience>:roles`): the administrator
  passes every ACL check.

Invalid or missing tokens are answered `401`. The identity travels with the
request (`InternalRequest`) down to the query and update processors and to
the SPUs, so subscriptions are filtered with the subscriber's permissions.

### Configuration

`engine.jpar` (or a second file passed with `-engine`, as Agorà's
`/jsap/engine-secure.jpar`):

```json
{
  "gates": {
    "security": {
      "enabled": true,
      "tls": false,
      "type": "zitadel",
      "zitadel": {
        "issuer": "https://auth.vaimee.com",
        "audience": "<Zitadel project ID>",
        "introspectionClientId": "",
        "introspectionClientSecret": "",
        "endpointUser": "",
        "endpointPassword": ""
      }
    }
  },
  "acl": { "enabled": true, "type": "dataset", "mode": "tdb2", "path": "/data/acl" }
}
```

The same values can be given on the command line: `-zitadelissuer`,
`-zitadelaudience`, `-zitadelintrospectionclientid`,
`-zitadelintrospectionclientsecret`, `-zitadelendpointuser`,
`-zitadelendpointpassword` (or the long form
`-parameters.gates.security.zitadel.<key>`).

- `endpointUser` / `endpointPassword` are the credentials SEPA uses towards
  its SPARQL endpoint; leave them empty for the embedded Jena endpoint.
- The secure engine loads a keystore (`sepa.jks`) even though Zitadel
  validates the tokens: the image must contain one (Agorà generates it with
  `keytool` when building `vaimee/agora-sepa`).

## 2. Authorization: the dataset ACL

The ACL (`DatasetACL` of SJENAR, `acl.type: "dataset"`) is evaluated inside
Jena, for every graph a request touches, with the user of the request:

| Permission (`aclId`) | Checked for |
| --- | --- |
| `query` | reading the graph (queries, subscriptions, the WHERE of updates) |
| `update` | any update that writes the graph |
| `insertdata` / `deletedata` | `INSERT DATA` / `DELETE DATA` (in addition to `update`) |
| `create` / `clear` / `drop` | `CREATE`, `CLEAR`, `DROP` of the graph |

- **Reads are filtered, not refused**: a query sees only the graphs the user
  may query, so `GRAPH ?g { ... }` returns the readable graphs only and no
  query needs ACL patterns of its own.
- **Writes are refused**: an update that touches a graph without the
  permission fails with `403 access_denied` (before `83610c0c` the client was
  never answered and waited until its own timeout).
- **Groups**: a user's permission is the union of its own and its groups'.
- **`$$ALL$$`** as graph name means every graph (e.g. service users that run
  the agents); **`$$DEFAULT$$`** is the default graph.
- The administrator (`admin` role) needs no entries.

### Persistence

With `"mode": "tdb2"` users, groups and permissions are stored in a TDB2
dataset at `acl.path` (`/data/acl` in Agorà, on the same volume as the data)
and survive restarts. Two bugs prevented it before `e08cc11a`:

- the storage mode was compared in upper case with lower-case constants, so
  the ACL always stayed in memory;
- graph names were inserted in the storage queries with `replaceAll`, where
  `$$ALL$$` is a group reference: every `$$ALL$$` permission failed with
  `ERROR: null`. Values are now quoted (`Matcher.quoteReplacement`) and the
  two special names are stored as `http://acl.sepa.com/graph/ALL` and
  `http://acl.sepa.com/graph/DEFAULT`.

## 3. Managing the ACL over HTTP (JMX + Jolokia)

The ACL is managed by the MBean **`SEPA:type=SEPAAclProcessor`**, registered
by the engine when the ACL is enabled (`6081dce0`). Its operations:

| Operation | Effect |
| --- | --- |
| `addUser(user)` / `removeUser(user)` | create / delete a user (its Zitadel `sub`) |
| `addUserPermission(user, graph, id)` | give `id` (one of the permissions above) on `graph` (IRI, `$$ALL$$`, `$$DEFAULT$$`) |
| `removeUserPermission(user, graph, id)` / `removeUserPermissions(user, graph)` | remove one / all permissions on a graph |
| `addGroup(group)` / `removeGroup(group)` | create / delete a group |
| `addGroupPermission(group, graph, id)` / `removeGroupPermission(...)` / `removeGroupPermissions(group, graph)` | same, for a group |
| `addUserToGroup(user, group)` / `removeUserFromGroup(user, group)` | membership |
| `listUsers()`, `viewUser(user)`, `listGroups()`, `viewGroup(group)` | read the ACL |
| `reloadUsers()`, `reloadGroups()`, `reloadUser(u)`, `reloadGroup(g)` | reload from the storage |

Every operation answers `OK` or `ERROR: <message>`; `addUser` on an existing
user answers `ERROR: null`, which can be ignored.

### The HTTP endpoint

SEPA does not open an HTTP port for the ACL itself: the **Jolokia JVM agent**
exposes the JMX MBeans as JSON over HTTP. Agorà's image adds the agent
(`jolokia-agent-jvm` 2.3.0, checksum pinned) and starts the engine with it
when a password is given:

```sh
java -javaagent:/jolokia.jar=host=0.0.0.0,port=8778,user=sepa,password=$SEPA_JOLOKIA_PASSWORD \
     -jar /engine.jar -engine /jsap/engine-secure.jpar ...
```

Keep port 8778 on the internal network only (no proxy route): it changes
permissions. In Agorà only the API calls it (`API_SEPA_ACL_URL=http://sepa:8778/jolokia/`).

Requests are `POST /jolokia/` with basic authentication and a JSON body:

```sh
J=http://sepa:8778/jolokia/
# create a user and let it read and write every graph (e.g. an agents' service user)
curl -s -u sepa:$PASSWORD -H 'Content-Type: application/json' $J -d \
  '{"type":"exec","mbean":"SEPA:type=SEPAAclProcessor","operation":"addUser(java.lang.String)","arguments":["387105833512599596"]}'
for p in query update insertdata deletedata create clear drop; do
  curl -s -u sepa:$PASSWORD -H 'Content-Type: application/json' $J -d \
    "{\"type\":\"exec\",\"mbean\":\"SEPA:type=SEPAAclProcessor\",\"operation\":\"addUserPermission(java.lang.String,java.lang.String,java.lang.String)\",\"arguments\":[\"387105833512599596\",\"\$\$ALL\$\$\",\"$p\"]}"
done
# a group that reads one graph, and a member
curl ... -d '{"type":"exec","mbean":"SEPA:type=SEPAAclProcessor","operation":"addGroup(java.lang.String)","arguments":["agora-users"]}'
curl ... -d '{"type":"exec","mbean":"SEPA:type=SEPAAclProcessor","operation":"addGroupPermission(java.lang.String,java.lang.String,java.lang.String)","arguments":["agora-users","https://vaimee.com/criteria/soils#public","query"]}'
curl ... -d '{"type":"exec","mbean":"SEPA:type=SEPAAclProcessor","operation":"addUserToGroup(java.lang.String,java.lang.String)","arguments":["<sub>","agora-users"]}'
# read
curl ... -d '{"type":"exec","mbean":"SEPA:type=SEPAAclProcessor","operation":"listUsers()"}'
```

The answer is `{"value": "OK", "status": 200, ...}`; Jolokia errors have a
`status` other than 200 and an `error` field, MBean errors a `value` starting
with `ERROR`. The operation signature (`name(types)`) is needed because some
operations are overloaded.

### How Agorà uses it

- Users get permissions on the graphs of what they create (their fields, their
  catalogue soils and crops) when they create it; every user is in the group
  `agora-users`, which reads the public catalogue.
- Service users (the API, the MDW agents) are Zitadel users with a JWT
  profile key, not admins: the API grants their permissions when it starts
  (`$$ALL$$` read/write for the agents).
- Admins (Studio `admin` role) see and change everything.

## 4. Tests

| Test | What it checks |
| --- | --- |
| `ZitadelSecurityManagerTest` | role extraction: `admin` role → `ADMIN_USER`, otherwise the `sub` |
| `AgoraStudioAclPolicyTest` (+ `.md`) | Studio roles on Jena `DatasetACL`: who reads and writes which graphs |
| `VariableGraphAclTest` | `GRAPH ?g` queries return only readable graphs; default graph |
| `JenaAclIdentityTest`, `InternalRequestIdentityTest` | the identity reaches Jena with the request |

```sh
JAVA_HOME=<temurin 25> mvn -pl engine -am -Dtest='*Acl*Test,ZitadelSecurityManagerTest' -Dsurefire.failIfNoSpecifiedTests=false test
```
