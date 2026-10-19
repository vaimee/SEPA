# Engine and dashboard changes

## Command line and parameters

| Option / parameter | Default | Meaning |
| --- | --- | --- |
| `-jsap <file>[,<file>...]` | none | JSAPs whose `graphPolicy` sections say which graphs accept subscriptions ([graph-policy.md](graph-policy.md)); only that section is read |
| `parameters.gates.wsPing` (`-parameters.gates.wsping`) | `60` | WebSocket ping interval in seconds, `0` turns it off |
| `-engine <file>` | `engine.jpar` | engine parameters; Agorà passes `/jsap/engine-secure.jpar` to turn on Zitadel security and the ACL ([security-and-acl.md](security-and-acl.md)) |
| `-zitadel*` | | Zitadel settings, see [security-and-acl.md](security-and-acl.md#configuration) |

Agorà runs the engine as:

```sh
java [-javaagent:/jolokia.jar=host=0.0.0.0,port=8778,user=sepa,password=...] -jar /engine.jar \
     -endpoint /endpoints/jena-in-memory.jpar -jsap /jsap/mdw.jsap,/jsap/agora.jsap \
     [-engine /jsap/engine-secure.jpar]
```

The dataset is Jena TDB1 at `./data/jena` (relative to the working
directory, `/data/jena` in the image) despite the endpoint file's name, and
the ACL is TDB2 at `acl.path`: mount `/data` on a volume.

## WebSocket ping (`2a2503ee`)

Connection-lost detection was off, so an idle subscription sent no traffic.
Docker swarm VIPs (IPVS) drop TCP connections idle for 15 minutes, and
proxies after their idle timeout, without telling either side: the client
stayed connected to nothing and missed every notification. The server now
pings every WebSocket each `wsPing` seconds, which keeps the connections
alive and closes those of clients that are gone.

Clients with long-lived HTTP connections have the same problem with their
keep-alive pools: the MDW agents close connections idle for more than 30 s
(`IdleSafeHttp` in the mdw repository), since the Java client does not retry a
`Connection reset`.

## Stopping SPUs (`b520116a`, `a2183bf2`)

When the last subscriber of an SPU leaves, the SPU is stopped. Two bugs made
this dangerous:

- It was stopped with `Thread.interrupt()`. If the SPU was querying the Jena
  TDB dataset at that moment, the interrupt closed the dataset's file channels
  (`ClosedByInterruptException`) and every later query failed with
  `ClosedChannelException` until restart; stopping clients while SEPA was
  processing their updates was enough.
- Waking the SPU took its monitors under the `Subscriptions` lock, while an SPU
  notifying its subscribers holds its own monitor and waits for that lock: the
  engine deadlocked when a subscriber dropped during an update.

`SPU.interrupt()` now only clears the running flag; the SPU's waits poll it
every 500 ms, a query in progress completes, and the SPU exits sending its
end of processing as before.

## Denied and failed updates are answered (`83610c0c`)

An exception in the update processing thread (an ACL refusal among them) was
printed on stderr and the client was never answered: it waited on the socket
until its own timeout. The client now gets `403 access_denied` for an ACL
refusal and `500 update_failed` otherwise.

## Docker build

`.dockerignore` excludes `**/target/`, so building the image does not send
the local Maven outputs (hundreds of MB) to the builder.

Large updates can overflow the parser's stack: run the engine with
`-Xss8m` when clients send big `INSERT DATA` blocks.

## Web dashboard

`dashboard-web` can run inside Agorà Studio as an iframe app:

- **Studio token**: it accepts `studio:auth` from the Studio shell and sends
  `Authorization: Bearer <token>` on queries, updates and subscriptions, so
  with SEPA security on each user sees the graphs their ACL allows (admins see
  everything).
- **Connection from the container env** (`2d21aa42`): `HOST`, `HTTP_PORT`,
  `HTTP_PROTOCOL`, `WS_PORT`, `WS_PROTOCOL`, `QUERY_PATH`, `UPDATE_PATH`,
  `SUBSCRIBE_PATH` are written to `js/config.jsc` at startup and override the
  connection of every JSAP loaded (behind a proxy SEPA is `https`/`wss` on
  443).
- **JSAP catalogue**: `jsap/catalog.json` lists the JSAPs a user can pick;
  `JSAP_PATH` is the default. Agorà's build replaces the default with one JSAP
  merged from `mdw.jsap` and `agora.jsap` (`deploy/portainer/build-and-push.sh`,
  `sepa-dashboard`). The `studio:instance` message only describes the
  connection: a JSAP without queries or updates no longer replaces the one
  loaded (`667fabc0`).
- **ACL admin JSAP** (`acl-admin.jsap`), for admins: queries (`ACL_USERS`,
  `ACL_USER_RIGHTS`, `ACL_GROUPS`, ...) and updates (`ACL_ADD_USER`,
  `ACL_ADD_USER_RIGHT`, `ACL_SET_USER_GROUP`, ...) on the ACL graph
  `http://acl.sepa.com/acl`. They work on the ACL graph when it is reachable
  through SPARQL; the engine caches the ACL, so after writing it call
  `reloadUsers()` / `reloadGroups()` on the MBean. With the ACL in its own
  TDB2 store (`acl.mode: tdb2`, as in Agorà) manage it through the MBean
  ([security-and-acl.md](security-and-acl.md#3-managing-the-acl-over-http-jmx--jolokia)).
- nginx serves the files with `Cache-Control: no-cache` (`a8037923`), so
  browsers pick up a new image.
