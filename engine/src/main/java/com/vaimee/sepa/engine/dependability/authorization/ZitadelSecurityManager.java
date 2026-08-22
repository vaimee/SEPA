package com.vaimee.sepa.engine.dependability.authorization;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.jena.acl.DatasetACL;
import org.apache.http.HttpStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.api.commons.security.Credentials;
import com.vaimee.sepa.engine.dependability.authorization.identities.DigitalIdentity;
import com.vaimee.sepa.logging.Logging;

public class ZitadelSecurityManager extends SecurityManager {
	private final ConfigurableJWTProcessor<SecurityContext> zitadelJwtProcessor;
	private final String issuer;
	private final String expectedAudience;
	private final String introspectionClientId;
	private final String introspectionClientSecret;
	private final Credentials endpointCredentials;
	private final HttpClient httpClient;

	public ZitadelSecurityManager(SSLContext ssl, RSAKey key, ZitadelProperties properties) throws SEPASecurityException {
		super(ssl, key, false);

		if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
			throw new SEPASecurityException("Zitadel issuer is not configured");
		}
		if (properties.getEndpointUser() == null || properties.getEndpointPassword() == null) {
			throw new SEPASecurityException("Zitadel endpoint credentials are not configured");
		}

		issuer = properties.getIssuer();
		expectedAudience = properties.getAudience();
		introspectionClientId = properties.getIntrospectionClientId();
		introspectionClientSecret = properties.getIntrospectionClientSecret();
		endpointCredentials = new Credentials(properties.getEndpointUser(), properties.getEndpointPassword());
		httpClient = HttpClient.newHttpClient();

		try {
			String jwksUri = issuer + "/oauth/v2/keys";
			JWKSource<SecurityContext> keySource = JWKSourceBuilder.<SecurityContext>create(new URI(jwksUri).toURL()).build();
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
			zitadelJwtProcessor = new DefaultJWTProcessor<>();
			zitadelJwtProcessor.setJWSKeySelector(keySelector);

			Set<String> required = new HashSet<>(Arrays.asList("exp", "iss", "sub"));
			JWTClaimsSet expected = new JWTClaimsSet.Builder().issuer(issuer).build();
			zitadelJwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(expected, required));
		} catch (Exception e) {
			throw new SEPASecurityException("Failed to initialize Zitadel security manager: " + e.getMessage());
		}
	}

	@Override
	public synchronized Response register(String uid) {
		return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not supported", "Implemented by Zitadel");
	}

	@Override
	public synchronized Response getToken(String encodedCredentials) {
		return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not supported", "Implemented by Zitadel");
	}

	@Override
	public synchronized ClientAuthorization validateToken(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return new ClientAuthorization("invalid_token", "Missing Bearer token");
		}

		if (!accessToken.contains(".")) {
			return introspect(accessToken);
		}

		try {
			JWTClaimsSet claims = zitadelJwtProcessor.process(accessToken, null);
			if (!audienceMatches(claims.getAudience())) {
				return new ClientAuthorization("invalid_token", "Invalid audience");
			}
			return authorize(resolveAclIdentity(claims, expectedAudience));
		} catch (Exception e) {
			Logging.log("oauth", "Zitadel token validation failed: " + e.getMessage());
			return new ClientAuthorization("invalid_token", e.getMessage());
		}
	}

	private ClientAuthorization introspect(String token) {
		if (introspectionClientId == null || introspectionClientId.isBlank()
				|| introspectionClientSecret == null || introspectionClientSecret.isBlank()) {
			return new ClientAuthorization("invalid_token", "Opaque token introspection is not configured");
		}

		try {
			String body = "token=" + urlEncode(token);
			HttpRequest request = HttpRequest.newBuilder(new URI(issuer + "/oauth/v2/introspect"))
					.header("Authorization", basicAuthHeader())
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) {
				Logging.log("oauth", "Zitadel token introspection failed with HTTP " + response.statusCode());
				return new ClientAuthorization("invalid_token", "Token introspection failed");
			}

			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			if (!json.has("active") || !json.get("active").getAsBoolean()) {
				return new ClientAuthorization("invalid_token", "Inactive token");
			}
			if (!json.has("aud") || !audienceMatches(json.get("aud"))) {
				return new ClientAuthorization("invalid_token", "Invalid audience");
			}
			if (!json.has("sub") || json.get("sub").getAsString().isBlank()) {
				return new ClientAuthorization("invalid_token", "Subject claim not found");
			}

			return authorize(resolveAclIdentity(json, expectedAudience));
		} catch (IOException e) {
			Logging.log("oauth", "Zitadel token introspection failed: " + e.getMessage());
			return new ClientAuthorization("invalid_token", "Token introspection failed");
		} catch (Exception e) {
			Logging.log("oauth", "Zitadel token introspection failed: " + e.getMessage());
			return new ClientAuthorization("invalid_token", e.getMessage());
		}
	}

	private ClientAuthorization authorize(String subject) {
		if (subject == null || subject.isBlank()) {
			return new ClientAuthorization("invalid_token", "Subject claim not found");
		}
		return new ClientAuthorization(endpointCredentials, subject);
	}

	static String resolveAclIdentity(JWTClaimsSet claims, String projectId) {
		return hasAdminRole(claims, projectId) ? DatasetACL.ADMIN_USER : claims.getSubject();
	}

	static String resolveAclIdentity(JsonObject token, String projectId) {
		return hasAdminRole(token, projectId) ? DatasetACL.ADMIN_USER : token.get("sub").getAsString();
	}

	private static boolean hasAdminRole(JWTClaimsSet claims, String projectId) {
		if (projectId == null || projectId.isBlank()) return false;
		Object roles = claims.getClaim(roleClaimName(projectId));
		return roles instanceof Map<?, ?> map && map.containsKey("admin");
	}

	private static boolean hasAdminRole(JsonObject token, String projectId) {
		if (projectId == null || projectId.isBlank()) return false;
		String claim = roleClaimName(projectId);
		return token.has(claim) && token.get(claim).isJsonObject() && token.getAsJsonObject(claim).has("admin");
	}

	private static String roleClaimName(String projectId) {
		return "urn:zitadel:iam:org:project:" + projectId + ":roles";
	}

	private boolean audienceMatches(List<String> audiences) {
		return expectedAudience == null || expectedAudience.isBlank()
				|| (audiences != null && audiences.stream().anyMatch(expectedAudience::equals));
	}

	private boolean audienceMatches(JsonElement audience) {
		if (expectedAudience == null || expectedAudience.isBlank()) {
			return true;
		}
		if (audience.isJsonArray()) {
			for (JsonElement element : audience.getAsJsonArray()) {
				if (expectedAudience.equals(element.getAsString())) {
					return true;
				}
			}
			return false;
		}
		return audience.isJsonPrimitive() && expectedAudience.equals(audience.getAsString());
	}

	private String basicAuthHeader() {
		String credentials = urlEncode(introspectionClientId) + ":" + urlEncode(introspectionClientSecret);
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		return endpointCredentials;
	}

	@Override public void addAuthorizedIdentity(DigitalIdentity identity) {}
	@Override public void removeAuthorizedIdentity(String uid) {}
	@Override public DigitalIdentity getIdentity(String uid) { return null; }
	@Override public boolean isAuthorized(String identity) { return false; }
	@Override public boolean isForTesting(String identity) { return false; }
	@Override public boolean storeCredentials(DigitalIdentity identity, String secret) { return false; }
	@Override public void removeCredentials(DigitalIdentity identity) {}
	@Override public boolean containsCredentials(String uid) { return false; }
	@Override public boolean checkCredentials(String uid, String secret) { return false; }
	@Override public void addJwt(String id, SignedJWT claims) {}
	@Override public boolean containsJwt(String id) { return false; }
	@Override public SignedJWT getJwt(String uid) { return null; }
	@Override public void removeJwt(String id) {}
	@Override public Date getTokenExpiringDate(String id) { return null; }
	@Override public long getTokenExpiringPeriod(String id) { return 0; }
	@Override public void setTokenExpiringPeriod(String id, long period) {}
	@Override public void setDeviceExpiringPeriod(long period) {}
	@Override public long getDeviceExpiringPeriod() { return 0; }
	@Override public void setApplicationExpiringPeriod(long period) {}
	@Override public long getApplicationExpiringPeriod() { return 0; }
	@Override public void setUserExpiringPeriod(long period) {}
	@Override public long getUserExpiringPeriod() { return 0; }
	@Override public void setDefaultExpiringPeriod(long period) {}
	@Override public long getDefaultExpiringPeriod() { return 0; }
	@Override public String getIssuer() { return issuer; }
	@Override public void setIssuer(String is) {}
}
