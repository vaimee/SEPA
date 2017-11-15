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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SPARQL11SEPrimitive;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
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

public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProtocol");

	private SPARQL11SEWebsocket wsClient;
	// private Websocket wssClient;

	protected SPARQL11SEProperties properties = null;

	public SPARQL11SEProtocol(SPARQL11SEProperties properties)
			throws SEPAProtocolException {
		super(properties);
	}
	
	public SPARQL11SEProtocol(SPARQL11SEProperties properties, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(properties);

		if (handler == null) {
			logger.fatal("Handler is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Handler is null"));
		}

		try {
			wsClient = new SPARQL11SEWebsocket(
					"ws://" + properties.getHost() + ":" + properties.getWsPort() + properties.getSubscribePath(),
					handler);
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}

	// public SPARQL11SEProtocol(SPARQL11SEProperties properties,
	// ISubscriptionHandler handler) throws SEPAProtocolException,
	// SEPASecurityException {
	// this(properties, handler, "sepa.jks", "sepa2017");
	// }
	//
	// public SPARQL11SEProtocol(SPARQL11SEProperties properties,
	// ISubscriptionHandler handler, String jksName,
	// String jksPassword) throws SEPASecurityException, SEPAProtocolException {
	// super(properties);
	//
	// if (properties == null) {
	// logger.fatal("Properties are null");
	// throw new IllegalArgumentException("Properties are null");
	// }
	//
	// this.properties = properties;
	//
	// // Create WebSocket clients (secure and not)
	// try {
	// wsClient = new SPARQL11SEWebsocket(
	// "ws://" + properties.getHost() + ":" + properties.getWsPort() +
	// properties.getSubscribePath(), handler);
	// } catch (URISyntaxException e) {
	// throw new SEPAProtocolException(e);
	// }
	//// wssClient = new Websocket("wss://" + properties.getHost() + ":" +
	// properties.getWssPort()
	//// + properties.getSecurePath() + properties.getSubscribePath(), true,
	// handler);
	// }

	public String toString() {
		return properties.toString();
	}

	// SPARQL 1.1 Update Primitive
	public Response update(UpdateRequest request) {
		return super.update(request, 0);
	}

	// SPARQL 1.1 Query Primitive
	public Response query(QueryRequest request) {
		logger.debug(request.toString());
		return super.query(request, 0);
	}

	// SPARQL 1.1 SE Subscribe Primitive
	public Response subscribe(SubscribeRequest request) {
		logger.debug(request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SUBSCRIBE, request);
	}

	// SPARQL 1.1 SE Unsubscribe Primitive
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

    @Override
    public void close() throws IOException {
        super.close();
        wsClient.close();
    }

    protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op) {
		return executeSPARQL11SEPrimitive(op, null);
	}

	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op, Object request) {
		// Create the HTTPS request
		URI uri;
		String path = null;
		int port = 0;

		// Headers and body
		String contentType = null;
		ByteArrayEntity body = null;
		String accept = null;
		String authorization = null;

		switch (op) {
		case SUBSCRIBE:
			SubscribeRequest subscribe = (SubscribeRequest) request;
			return wsClient.subscribe(subscribe.getSPARQL());
		case UNSUBSCRIBE:
			UnsubscribeRequest unsubscribe = (UnsubscribeRequest) request;
			return wsClient.unsubscribe(unsubscribe.getSubscribeUUID());
		// case SECURESUBSCRIBE:
		// SubscribeRequest securesubscribe = (SubscribeRequest) request;
		// try {
		// return wssClient.subscribe(securesubscribe.getSPARQL(),
		// securesubscribe.getAlias(),
		// properties.getAccessToken());
		// } catch (SEPASecurityException e2) {
		// return new ErrorResponse(500,e2.getMessage());
		// }
		// case SECUREUNSUBSCRIBE:
		// UnsubscribeRequest secureunsubscribe = (UnsubscribeRequest) request;
		// try {
		// return wssClient.unsubscribe(secureunsubscribe.getSubscribeUUID(),
		// properties.getAccessToken());
		// } catch (SEPASecurityException e2) {
		// return new ErrorResponse(500,e2.getMessage());
		// }
		default:
			break;
		}

		switch (op) {
		case REGISTER:
			path = properties.getRegisterPath();
			port = properties.getHttpsPort();

			accept = "application/json";
			contentType = "application/json";
			String identity = (String) request;

			try {
				body = new ByteArrayEntity(new RegistrationRequest(identity).toString().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(500, e.getMessage());
			}
			break;
		case REQUESTTOKEN:
			String basic;
			try {
				basic = properties.getBasicAuthorization();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(500, e2.getMessage());
			}
			if (basic == null)
				return new ErrorResponse(0, 401, "Basic authorization in null. Register first");

			path = properties.getTokenRequestPath();
			port = properties.getHttpsPort();

			authorization = "Basic " + basic;
			contentType = "application/json";
			accept = "application/json";
			break;
		case SECUREUPDATE:
			path = properties.getSecurePath() + properties.getUpdatePath();
			port = properties.getHttpsPort();

			accept = "text/plain";
			contentType = "application/x-www-form-urlencoded";
			try {
				authorization = "Bearer " + properties.getAccessToken();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(500, e2.getMessage());
			}

			String encodedContent;
			try {
				encodedContent = URLEncoder.encode(((UpdateRequest) request).getSPARQL(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(500, e.getMessage());
			}
			body = new ByteArrayEntity(("update=" + encodedContent).getBytes());
			body.setContentType(contentType);
			break;
		case SECUREQUERY:
			path = properties.getSecurePath() + properties.getQueryPath();
			port = properties.getHttpsPort();

			accept = "application/sparql-results+json";
			contentType = "application/sparql-query";
			try {
				authorization = "Bearer " + properties.getAccessToken();
			} catch (SEPASecurityException e2) {
				return new ErrorResponse(500, e2.getMessage());
			}

			try {
				body = new ByteArrayEntity(((QueryRequest) request).getSPARQL().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return new ErrorResponse(500, e.getMessage());
			}
			break;
		default:
			break;
		}

		// POST request
		try {
			uri = new URI("https", null, properties.getHost(), port, path, null, null);
		} catch (URISyntaxException e1) {
			return new ErrorResponse(500, e1.getMessage());
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
			return new ErrorResponse(500, e.getMessage());
		}
		CloseableHttpResponse response = null;
		String jsonResponse = null;

		try {
			long timing = System.nanoTime();

			try {
				response = httpclient.execute(httpRequest);
			} catch (IOException e) {
				return new ErrorResponse(500, e.getMessage());
			}

			timing = System.nanoTime() - timing;

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
				return new ErrorResponse(500, e.getMessage());
			}
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				return new ErrorResponse(500, e.getMessage());
			}
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				return new ErrorResponse(500, e.getMessage());
			}
		}

		// Parsing the response
		try {
			return parseSPARQL11SEResponse(jsonResponse, op);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException e) {
			return new ErrorResponse(500, e.getMessage());
		}
	}

	protected Response parseSPARQL11SEResponse(String response, SPARQL11SEPrimitive op) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (response == null)
			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response is null");

		JsonObject json = null;
		try {
			json = new JsonParser().parse(response).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown response: " + response);
		}

		// Error response
		if (json.get("code") != null)
			if (json.get("code").getAsInt() >= 400)
				return new ErrorResponse(0, json.get("code").getAsInt(), json.get("body").getAsString());

		if (op == SPARQL11SEPrimitive.SECUREQUERY)
			return new QueryResponse(json);
		if (op == SPARQL11SEPrimitive.SECUREUPDATE)
			return new UpdateResponse(response);

		if (op == SPARQL11SEPrimitive.REGISTER) {
			if (json.get("client_id") != null && json.get("client_secret") != null) {
				try {
					properties.setCredentials(json.get("client_id").getAsString(),
							json.get("client_secret").getAsString());
				} catch (SEPASecurityException | SEPAPropertiesException e) {
					return new ErrorResponse(-1, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to save credentials");
				}

				return new RegistrationResponse(json.get("client_id").getAsString(),
						json.get("client_secret").getAsString(), json.get("signature"));
			}
			return new ErrorResponse(-1, HttpStatus.SC_INTERNAL_SERVER_ERROR,
					"Credentials not found in registration response");
		}

		if (op == SPARQL11SEPrimitive.REQUESTTOKEN) {
			if (json.get("access_token") != null && json.get("expires_in") != null && json.get("token_type") != null) {
				int seconds = json.get("expires_in").getAsInt();
				Date expires = new Date();
				expires.setTime(expires.getTime() + (1000 * seconds));
				try {
					properties.setJWT(json.get("access_token").getAsString(), expires,
							json.get("token_type").getAsString());
				} catch (SEPASecurityException | SEPAPropertiesException e) {
					return new ErrorResponse(-1, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to save JWT");
				}
				return new JWTResponse(json.get("access_token").getAsString(), json.get("token_type").getAsString(),
						json.get("expires_in").getAsLong());
			} else if (json.get("code") != null && json.get("body") != null)
				return new ErrorResponse(0, json.get("code").getAsInt(), json.get("body").getAsString());
			else if (json.get("code") != null)
				return new ErrorResponse(0, json.get("code").getAsInt(), "");

			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR,
					"Response not recognized: " + json.toString());
		}

		return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response unknown: " + response);
	}
}
