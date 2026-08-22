# Agora Studio ACL Policy Test

`AgoraStudioAclPolicyTest` verifies the ACL policy used by Agora Studio on top of Jena `DatasetACL`.

The test does not call Zitadel. Zitadel role extraction is covered by `ZitadelSecurityManagerTest`; this test starts from the identity that SEPA receives after token validation. For normal users that identity is the Zitadel `sub`; for admins it is `DatasetACL.ADMIN_USER`.

## Scenario

The admin identity creates the ACL configuration in memory through `SEPAAcl` before the dataset is used. Then each Studio role is exercised through an `RDFConnection` opened with that user's identity.

Configured users:

- `studio-user-farmer`: query on Agora app graph.
- `studio-user-developer`: query/update/insertData on Agora API and Dashboard SEPA graphs.
- `studio-user-agronomist`: query on Agora app, Scenarios, and CRITERIA Web graphs.
- `studio-user-student`: query on AgriTwix graph.
- `studio-user-cooperative`: query on Agora app, Scenarios, and WDA graphs.
- `studio-user-agent`: query/update/insertData on the agents graph.
- `studio-user-no-role`: no graph access.
- `DatasetACL.ADMIN_USER`: wildcard access to every graph.

## What Is Verified

- Users can query only the graphs assigned to their Studio role.
- Users cannot see graphs outside their ACL configuration.
- Only `developer` and `agent` can write to their configured writable graphs.
- Read-only roles cannot write even to graphs they can query.
- Admin can read and write the private admin graph without explicit ACL entries.

The write checks use `INSERT DATA`. Jena ACL checks that operation with the specific insert-data ACL in addition to the generic update permission, so writable users are configured with both rights. Denied writes are accepted as safe if they either produce no change or are rejected with `ACLException`.

The test appends a scenario suffix to users and graph IRIs (`read` or `write`) because the current ACL storage implementation keeps process-local state across test methods.

Run only this policy test with:

```sh
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home" mvn -pl engine -am -Dtest=AgoraStudioAclPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test
```
