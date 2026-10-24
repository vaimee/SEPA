package com.vaimee.sepa.apps.chat;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.security.OAuthProperties;
import com.vaimee.sepa.api.pattern.JSAP;

/**
 * A JSAP whose requests carry a bearer token obtained elsewhere, e.g. a Zitadel
 * access token for an engine running with Zitadel security and the ACL. The
 * engine maps the token's subject to its ACL user.
 *
 * The client library's own OAuth flows (client registration, token refresh,
 * storing the credentials in the JSAP file) do not apply and are turned off.
 */
public class BearerTokenJSAP extends JSAP {
	private static final long serialVersionUID = 1L;

	private transient OAuthProperties bearer;

	public BearerTokenJSAP(String uri, String token) throws SEPAPropertiesException {
		super(uri);
		// Built while isSecure() is still false: the OAuthProperties constructor would read the oauth section
		bearer = new BearerProperties(this, token);
	}

	@Override
	public boolean isSecure() {
		return bearer != null;
	}

	@Override
	public OAuthProperties getAuthenticationProperties() {
		return bearer;
	}

	private static final class BearerProperties extends OAuthProperties {
		private final String token;

		BearerProperties(JSAP jsap, String token) throws SEPAPropertiesException {
			super(jsap);
			this.token = token;
		}

		@Override
		public String getBearerAuthorizationHeader() {
			return "Bearer " + token;
		}

		@Override
		public String getToken() {
			return token;
		}

		@Override
		public String getTokenType() {
			return "Bearer";
		}

		@Override
		public boolean isTokenExpired() {
			return false;
		}

		@Override
		public boolean isClientRegistered() {
			return true;
		}

		@Override
		public synchronized void storeProperties() {
			// Nothing to store: the token lives in memory only
		}
	}
}
