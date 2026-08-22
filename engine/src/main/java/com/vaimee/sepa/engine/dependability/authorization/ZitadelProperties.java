package com.vaimee.sepa.engine.dependability.authorization;

public class ZitadelProperties {
	private final String issuer;
	private final String audience;
	private final String introspectionClientId;
	private final String introspectionClientSecret;
	private final String endpointUser;
	private final String endpointPassword;

	public ZitadelProperties(String issuer, String audience, String introspectionClientId,
			String introspectionClientSecret, String endpointUser, String endpointPassword) {
		this.issuer = issuer == null ? null : issuer.replaceAll("/+$", "");
		this.audience = audience;
		this.introspectionClientId = introspectionClientId;
		this.introspectionClientSecret = introspectionClientSecret;
		this.endpointUser = endpointUser;
		this.endpointPassword = endpointPassword;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getAudience() {
		return audience;
	}

	public String getIntrospectionClientId() {
		return introspectionClientId;
	}

	public String getIntrospectionClientSecret() {
		return introspectionClientSecret;
	}

	public String getEndpointUser() {
		return endpointUser;
	}

	public String getEndpointPassword() {
		return endpointPassword;
	}
}
