/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SPARQL11SEPrimitive;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.RegistrationRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;

import it.unibo.arces.wot.sepa.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

/**
 * This class implements the SPARQL 1.1 Secure event protocol with SPARQL 1.1
 * subscribe language.
 *
 * @see <a href="http://wot.arces.unibo.it/TR/sparql11-se-protocol.html">SPARQL
 *      1.1 Secure Event Protocol</a>
 * @see <a href="http://wot.arces.unibo.it/TR/sparql11-subscribe.html">SPARQL
 *      1.1 Subscribe Language</a>
 */
public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger();

	private SPARQL11SEProperties properties = null;	
	private ISubscriptionProtocol subscriptionProtocol;
	
	public SPARQL11SEProtocol(SPARQL11SEProperties properties,ISubscriptionProtocol protocol,ISubscriptionHandler handler) throws IllegalArgumentException, SEPAProtocolException {
		if (protocol == null  || handler == null || properties == null) {
			logger.error("One or more arguments are null");
			throw new IllegalArgumentException("One or more arguments are null");
		}
		
		this.subscriptionProtocol = protocol;
		this.subscriptionProtocol.setHandler(handler);
		this.properties = properties;
	}

	/**
	 * {@inheritDoc}
	 */
	public Response update(UpdateRequest request) {
		return super.update(request);
	}

	/**
	 * {@inheritDoc}
	 */
	public Response query(QueryRequest request) {
		return super.query(request);
	}

	/**
	 * Subscribe with a SPARQL 1.1 Subscription language. All the notification will
	 * be forwarded to the {@link ISubscriptionHandler} of this instance.
	 *
	 * @param request
	 * @return A valid {@link Response} if the subscription is successful <br>
	 *         an {@link ErrorResponse} otherwise
	 */
	public Response subscribe(SubscribeRequest request) {
		logger.debug(request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SUBSCRIBE, request);
	}

	/**
	 * Unsubscribe with a SPARQL 1.1 Subscription language. Note that you must
	 * supply a SPUID that identify the subscription that you want to delete. This
	 * primitive does not free any resources, you must call the {@link #close()}
	 * method.
	 *
	 * @param request
	 * @return A valid {@link Response} if the unsubscription is successful <br>
	 *         an {@link ErrorResponse} otherwise
	 */
	public Response unsubscribe(UnsubscribeRequest request) {
		logger.debug(request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.UNSUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Subscribe Primitive
	public Response secureSubscribe(SubscribeRequest request) {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECURESUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Unsubscribe Primitive
	public Response secureUnsubscribe(UnsubscribeRequest request) {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUNSUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Update Primitive
	public Response secureUpdate(UpdateRequest request) {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUPDATE, request);
	}

	// SPARQL 1.1 SE SECURE Query Primitive
	public Response secureQuery(QueryRequest request) {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREQUERY, request);
	}

	// Registration to the Authorization Server (AS)
	public Response register(String identity) {
		logger.debug("REGISTER " + identity);
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REGISTER, identity);
	}

	// Token request to the Authorization Server (AS)
	public Response requestToken() {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REQUESTTOKEN);
	}

	// Get expiring time
	public long getTokenExpiringSeconds() throws SEPASecurityException {
		return properties.getExpiringSeconds();
	}

	/**
	 * Free the http connection manager and the WebSocket client.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		super.close();
		subscriptionProtocol.close();
	}

	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op) {
		return executeSPARQL11SEPrimitive(op, null);
	}

	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op, Object request) {
		// Create the HTTPS request
		URI uri = null;
		String path = null;
		int port = 0;

		// Headers and body
		String contentType = null;
		ByteArrayEntity body = null;
		String accept = null;
		String authorization = null;

		switch (op) {
		case SUBSCRIBE:
		case SECURESUBSCRIBE:
			SubscribeRequest subscribe = (SubscribeRequest) request;
			if (op == SPARQL11SEPrimitive.SUBSCRIBE) {
				return subscriptionProtocol.subscribe(subscribe.getSPARQL());
			}

			try {
				authorization = properties.getAccessToken();
			} catch (SEPASecurityException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			
			return subscriptionProtocol.secureSubscribe(subscribe.getSPARQL(), "Bearer " + authorization);
		case UNSUBSCRIBE:
		case SECUREUNSUBSCRIBE:
			UnsubscribeRequest unsubscribe = (UnsubscribeRequest) request;
			if (op == SPARQL11SEPrimitive.UNSUBSCRIBE) {
				return subscriptionProtocol.unsubscribe(unsubscribe.getSubscribeUUID());
			}
			
			try {
				return subscriptionProtocol.secureUnsubscribe(unsubscribe.getSubscribeUUID(),"Bearer " + properties.getAccessToken());
			} catch (SEPASecurityException e3) {
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, e3.getMessage());
			}
		default:
			break;
		}

		switch (op) {
		case REGISTER:
			try {
				uri = new URI(properties.getRegisterUrl());
			} catch (URISyntaxException e1) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			}

			accept = "application/json";
			contentType = "application/json";
			String identity = (String) request;

			try {
				body = new ByteArrayEntity(new RegistrationRequest(identity).toString().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			break;
		case REQUESTTOKEN:
			try {
				uri = new URI(properties.getTokenRequestUrl());
			} catch (URISyntaxException e1) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			}

			String basic;
			try {
				basic = properties.getBasicAuthorization();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e2.getMessage());
			}
			if (basic == null)
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Basic authorization in null. Register first");

			authorization = "Basic " + basic;
			contentType = "application/json";
			accept = "application/json";
			break;
		case SECUREUPDATE:
			path = properties.getUpdatePath();
			port = properties.getHttpsPort();

			accept = "text/plain";
			contentType = "application/x-www-form-urlencoded";
			try {
				authorization = "Bearer " + properties.getAccessToken();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e2.getMessage());
			}

			String encodedContent;
			try {
				encodedContent = URLEncoder.encode(((UpdateRequest) request).getSPARQL(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			body = new ByteArrayEntity(("update=" + encodedContent).getBytes());
			body.setContentType(contentType);
			break;
		case SECUREQUERY:
			path = properties.getDefaultQueryPath();
			port = properties.getHttpsPort();

			accept = "application/sparql-results+json";
			contentType = "application/sparql-query";
			try {
				authorization = "Bearer " + properties.getAccessToken();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e2.getMessage());
			}

			try {
				body = new ByteArrayEntity(((QueryRequest) request).getSPARQL().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			break;
		default:
			break;
		}

		// POST request
		try {
			if (uri == null)
				uri = new URI("https", null, properties.getDefaultHost(), port, path, null, null);
		} catch (URISyntaxException e1) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
		}

		HttpUriRequest httpRequest = new HttpPost(uri);

		if (contentType != null)
			httpRequest.setHeader("Content-Type", contentType);
		if (accept != null)
			httpRequest.setHeader("Accept", accept);
		if (authorization != null)
			httpRequest.setHeader("Authorization", authorization);
		if (body != null)
			((HttpPost) httpRequest).setEntity(body);

		logger.debug("Request: " + httpRequest);

		// HTTP request execution
		CloseableHttpClient httpclient;
		try {
			httpclient = new SSLSecurityManager("TLSv1", "sepa.jks", "sepa2017", "sepa2017").getSSLHttpClient();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
				| CertificateException | IOException e) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
		CloseableHttpResponse response = null;
		String jsonResponse = null;

		try {
			long timing = System.nanoTime();

			try {
				response = httpclient.execute(httpRequest);
			} catch (IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}

			timing = System.nanoTime() - timing;

			if (response == null) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response is null");
			}

			logger.debug("Response: " + response);
			if (op.equals(SPARQL11SEPrimitive.REGISTER))
				logger.debug("REGISTER " + timing / 1000000 + " ms");
			else if (op.equals(SPARQL11SEPrimitive.REQUESTTOKEN))
				logger.debug("TOKEN " + timing / 1000000 + " ms");
			else if (op.equals(SPARQL11SEPrimitive.SECUREQUERY))
				logger.debug("SECURE_QUERY " + timing / 1000000 + " ms");
			else if (op.equals(SPARQL11SEPrimitive.SECUREUPDATE))
				logger.debug("SECURE_UPDATE " + timing / 1000000 + " ms");

			HttpEntity entity = response.getEntity();
			try {
				jsonResponse = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			} catch (ParseException | IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}

		// Parsing the response
		try {
			return parseSPARQL11SEResponse(jsonResponse, op);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException e) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	protected Response parseSPARQL11SEResponse(String response, SPARQL11SEPrimitive op) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (response == null)
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response is null");

		// Parse JSON response
		JsonObject json = null;
		try {
			json = new JsonParser().parse(response).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Not JSON response: " + response);
		}

		// Error response
		try {
			if (json.get("error") != null)
				return new ErrorResponse(0, json.get("error").getAsJsonObject().get("code").getAsInt(),
						json.get("error").getAsJsonObject().get("body").getAsString());
		} catch (Exception e) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unrecognized error message");
		}

		if (op == SPARQL11SEPrimitive.SECUREQUERY)
			return new QueryResponse(json);
		if (op == SPARQL11SEPrimitive.SECUREUPDATE)
			return new UpdateResponse(response);

		if (op == SPARQL11SEPrimitive.REGISTER) {
			try {
				String id = json.get("credentials").getAsJsonObject().get("client_id").getAsString();
				String secret = json.get("credentials").getAsJsonObject().get("client_secret").getAsString();
				JsonElement signature = json.get("credentials").getAsJsonObject().get("signature");
				properties.setCredentials(id, secret);
				return new RegistrationResponse(id, secret, signature);
			} catch (Exception e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
			}
		}

		if (op == SPARQL11SEPrimitive.REQUESTTOKEN) {
			try {
				int seconds = json.get("token").getAsJsonObject().get("expires_in").getAsInt();
				String jwt = json.get("token").getAsJsonObject().get("access_token").getAsString();
				String type = json.get("token").getAsJsonObject().get("token_type").getAsString();
				Date expires = new Date();
				expires.setTime(expires.getTime() + (1000 * seconds));
				properties.setJWT(jwt, expires, type);
				return new JWTResponse(jwt, type, seconds);
			} catch (Exception e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
			}
		}

		return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response unknown: " + response);
	}
}
