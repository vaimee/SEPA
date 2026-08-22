package com.vaimee.sepa.engine.dependability.authorization;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.jena.acl.DatasetACL;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZitadelSecurityManagerTest {
    private static final String PROJECT_ID = "387103488611450924";
    private static final String ROLE_CLAIM = "urn:zitadel:iam:org:project:" + PROJECT_ID + ":roles";

    @Test
    void adminJwtRoleMapsToAclAdminUser() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("zitadel-admin-sub")
                .claim(ROLE_CLAIM, Map.of("admin", Map.of("org", "self.auth.vaimee.com")))
                .build();

        assertEquals(DatasetACL.ADMIN_USER, ZitadelSecurityManager.resolveAclIdentity(claims, PROJECT_ID));
    }

    @Test
    void nonAdminJwtRoleKeepsZitadelSubject() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("zitadel-user-sub")
                .claim(ROLE_CLAIM, Map.of("farmer", Map.of("org", "self.auth.vaimee.com")))
                .build();

        assertEquals("zitadel-user-sub", ZitadelSecurityManager.resolveAclIdentity(claims, PROJECT_ID));
    }

    @Test
    void adminIntrospectionRoleMapsToAclAdminUser() {
        JsonObject token = new JsonObject();
        token.addProperty("sub", "zitadel-admin-sub");
        JsonObject roles = new JsonObject();
        roles.add("admin", new JsonObject());
        token.add(ROLE_CLAIM, roles);

        assertEquals(DatasetACL.ADMIN_USER, ZitadelSecurityManager.resolveAclIdentity(token, PROJECT_ID));
    }

    @Test
    void missingRolesKeepZitadelSubject() {
        JsonObject token = new JsonObject();
        token.addProperty("sub", "zitadel-user-sub");

        assertEquals("zitadel-user-sub", ZitadelSecurityManager.resolveAclIdentity(token, PROJECT_ID));
    }
}
