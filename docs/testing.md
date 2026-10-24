# Running the tests

Java 25 (the build's `release`). The commands run from the repository root;
`-Dgpg.skip` turns off the artifact signing that `install` and `deploy` need
for publishing.

| Suite | What | Needs |
| --- | --- | --- |
| Unit (`mvn test`) | api-java, engine (ACL, graph policy, Zitadel tokens, request identity, denied updates) | nothing |
| Integration (`mvn verify`) | api-java `IT*` classes: SPARQL 1.1 and SE protocols, WebSocket, PAC pattern | an engine on `localhost:8000` / `9000` without security |
| Chat example | `example-chat`: `SEPAChatTest` (10 bots exchanging messages through subscriptions) and `UpdateQueryTest` | an engine on `localhost:8000` / `9000`, with or without security |

The chat tests are plain JUnit tests, so `mvn test` runs them too: start the
engine first, or leave `example-chat` out (`-pl api-java,engine`).

## Engine without security

```sh
mvn -B package -DskipTests -Dgpg.skip
mkdir -p /tmp/sepa-it && cp engine/target/sepa-engine-1.0.0-SNAPSHOT.jar /tmp/sepa-it/engine.jar
(cd /tmp/sepa-it && java -Xss8m -jar engine.jar)    # in-memory Jena, /query /update /subscribe
mvn -B verify -Dgpg.skip
```

Start it from an empty directory: the engine reads `engine.jpar` and
`endpoint.jpar` from the working directory, and without them it uses the
defaults the tests expect. No `-jsap`: with a graph policy the chat
subscriptions on `http://wot.arces.unibo.it/chat` are refused.

## Chat example with security and ACL

The engine runs as in Agorà: Zitadel tokens and the dataset ACL
([security-and-acl.md](security-and-acl.md)). The chat clients send a bearer
token taken from `SEPA_TOKEN` (or `-DsepaToken=...`); without it they send none.

1. Engine: `engine-secure.jpar` with a local ACL path, `sepa.jks` in the working
   directory, the Jolokia agent for the ACL MBean.

   ```sh
   cat > engine-secure.jpar <<'EOF'
   {"gates": {"security": {"tls": false, "enabled": true, "type": "zitadel",
      "zitadel": {"issuer": "https://auth.vaimee.com", "audience": "387103488611450924"}}},
    "acl": {"enabled": true, "type": "dataset", "mode": "tdb2", "path": "./data/acl"}}
   EOF
   cp <repo>/engine/src/main/resources/config/sepa.jks .
   # jolokia.jar: org.jolokia:jolokia-agent-jvm:2.3.0, classifier javaagent
   java -javaagent:jolokia.jar=host=127.0.0.1,port=8778,user=sepa,password=$JOLOKIA_PASSWORD \
        -Xss8m -jar engine.jar -engine engine-secure.jpar
   ```

2. A token whose `sub` is the test user, e.g. a Zitadel service user's token
   (JWT profile grant, audience the Studio project).

3. The authorization: the chat writes `http://wot.arces.unibo.it/chat` and
   `http://wot.arces.unibo.it/chat/log`. Without permissions every update is
   refused (`403 access_denied`).

   ```sh
   J=http://127.0.0.1:8778/jolokia; U=<sub of the token>
   jx() { curl -s -u sepa:$JOLOKIA_PASSWORD -H 'Content-Type: application/json' $J -d "$1"; echo; }
   jx "{\"type\":\"exec\",\"mbean\":\"SEPA:type=SEPAAclProcessor\",\"operation\":\"addUser(java.lang.String)\",\"arguments\":[\"$U\"]}"
   for g in http://wot.arces.unibo.it/chat http://wot.arces.unibo.it/chat/log; do
     for p in query update insertData deleteData; do
       jx "{\"type\":\"exec\",\"mbean\":\"SEPA:type=SEPAAclProcessor\",\"operation\":\"addUserPermission(java.lang.String,java.lang.String,java.lang.String)\",\"arguments\":[\"$U\",\"$g\",\"$p\"]}"
     done
   done
   ```

4. The tests:

   ```sh
   SEPA_TOKEN=<token> mvn -B test -Dgpg.skip -pl api-java,example-chat
   ```

Without `SEPA_TOKEN` the same tests get `401` from the secure engine; with the
token and no permissions, `403`.

## Fixes made while enabling the tests

- **Denied updates blocked the engine** (#118): `DeniedUpdateTest` subscribes
  to a graph, makes the endpoint refuse an update the way the ACL does, and
  checks that the next update is answered (it hung before the fix).
- **ACL tests sharing one storage**: `ACLStorageFactory.newInstance` keeps one
  storage per JVM, so the tests that filled it (Studio roles, variable graphs,
  identity) left their users to the others. The tests that do not test the
  factory now build their own `ACLStorageDataset`. `SEPAAclTest` uses its own
  ACL again (the one in its comment table): the shared one grants `monger`
  update on graph1 and nothing on graph5, contrary to what the test asserts.
  It failed on `master` too.
- **ACL permission names**: `decodeACLId` checked the lower-cased name but looked
  up the original one, so `insertData` / `deleteData` became `null` and the
  MBean failed with a `NullPointerException`. Names are case-insensitive, and
  an unknown one answers `ERROR`.
- **WebSocket client with a secure profile**: `WebsocketClientEndpoint` set a
  TLS engine whenever the profile was secure, also on `ws://`, where the
  connection then failed. TLS is now set on `wss://` only.
- **Chat example**: the old secure mode (`-Dsecure`, `chat-secure.jsap`) could
  not work, since the library's `JSAP.isSecure()` is always `false` and the file
  does not exist; it is replaced by the bearer token (`BearerTokenJSAP`).
  `UpdateQueryTest` bound `?user` in `REGISTER_USER`, which generates it
  (`BIND(UUID() AS ?user)`), turning the update into invalid SPARQL.
