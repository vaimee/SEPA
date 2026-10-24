# SEPA on the `studio` branch

This branch is the SEPA that Agorà Studio runs (images `vaimee/sepa` and
`vaimee/agora-sepa`, release 0.2.0 of Agorà). It adds to `master` the
features below, each documented in its own page.

| Topic | What changed | Page |
| --- | --- | --- |
| Security | Zitadel access tokens (`gates.security.type: zitadel`): the token's `sub` is the user, the `admin` project role is the ACL administrator | [security-and-acl.md](security-and-acl.md) |
| ACL | Jena dataset ACL (SJENAR) per graph and operation, persisted in TDB2; managed at runtime through the `SEPA:type=SEPAAclProcessor` MBean, reachable over HTTP with the Jolokia agent | [security-and-acl.md](security-and-acl.md) |
| Subscriptions | Graph policy: subscriptions are accepted on *live* graphs only, declared in the `graphPolicy` section of the JSAPs given to the engine (`-jsap`) | [graph-policy.md](graph-policy.md) |
| JSAP | Forced bindings: types, datatypes, defaults, multiple bindings (`VALUES`) and **optional bindings** (no value: the variable stays free), in the Java and JavaScript clients | [jsap-forced-bindings.md](jsap-forced-bindings.md) |
| Engine | `-jsap` option, WebSocket ping, SPU stop without deadlocks, denied updates answered, Docker build context | [engine.md](engine.md) |
| Dashboard | Web dashboard embedded in Studio: Studio token, JSAP catalogue, ACL admin JSAP | [engine.md](engine.md#web-dashboard) |
| Tests | Unit, integration and chat tests, also against a secure engine with the ACL; the fixes made to enable them | [testing.md](testing.md) |

Commits (oldest first):

| Commit | Change |
| --- | --- |
| `58257309` | feat(auth): add Zitadel ACL enforcement |
| `da7f46e6`, `abd64808`, `fa176616` | dashboard: Studio ACL admin support, JSAP selector, Studio local stack |
| `ea9847ec`, `4595b1cc` | ACL policy tests: Studio roles, variable-graph queries, default graph |
| `6081dce0` | fix(acl): register the ACL management MBean |
| `83610c0c` | fix(acl): answer denied updates, and persist user permissions |
| `2d21aa42`, `a8037923`, `667fabc0` | dashboard: Studio SEPA connection from the env, cache revalidation, catalogue JSAP kept |
| `b520116a`, `a2183bf2` | fix(engine): stop SPUs without interrupting their thread and without taking their monitors |
| `6449395a` | feat(engine): accept subscriptions on live graphs only |
| `df061c95` | fix(engine): read graphs bound by VALUES in the subscription policy |
| `2a2503ee` | fix(engine): ping WebSocket clients every 60 s |
| `e08cc11a` | fix(engine): persist the dataset ACL and its `$$ALL$$` permissions |

How Agorà uses all this (users, groups, service users, the bootstrap of the
permissions, the migration) is described in the agora repository:
`docs/AGORA-ONTOLOGY-ALIGNMENT.md` (Access control, Rollout) and
`deploy/portainer/README.md`.
