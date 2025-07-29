package com.vaimee.sepa.api.commons.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.request.RegistrationRequest;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.JWTResponse;
import com.vaimee.sepa.api.commons.response.RegistrationResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.logging.Logging;

public class DefaultAuthenticationService extends AuthenticationService {
	
	public DefaultAuthenticationService(OAuthProperties oauthProperties)
			throws SEPASecurityException {
		super(oauthProperties);
	}
	
	public Response registerClient(String client_id, String username, String initialAccessToken, int timeout) throws SEPASecurityException {
		if (client_id == null) throw new SEPASecurityException("Identity is null");
			
		Logging.log("oauth","REGISTER " + client_id);

		CloseableHttpResponse response = null;
		long start = Logging.getTime();

		try {
			URI uri = new URI(oauthProperties.getRegisterUrl());
			ByteArrayEntity body = new ByteArrayEntity(new RegistrationRequest(client_id).toString().getBytes("UTF-8"));

			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setHeader("Accept", "application/json");
			httpRequest.setEntity(body);

			// Set timeout
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
					.build();
			httpRequest.setConfig(requestConfig);

			Logging.log("oauth","Request: "+httpRequest);

			try {
				response = httpClient.execute(httpRequest);
			} catch (IOException e) {
				Logging.error("HTTP EXECUTE: " + e.getMessage());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "HttpExecute", e.getMessage());
			}

			Logging.log("oauth","Response: " + response);
			
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity, Charset.forName("UTF-8"));

			EntityUtils.consume(entity);

			JsonObject json = new Gson().fromJson(jsonResponse,JsonObject.class);

			if (json.has("error")) {
				int code = json.get("status_code").getAsInt();
				String error = json.get("error").getAsString();
				String description = json.get("error_description").getAsString();

				ErrorResponse ret = new ErrorResponse(code, error, description);
				Logging.error(ret.toString());

				return ret;
			}

			String id = json.get("credentials").getAsJsonObject().get("client_id").getAsString();
			String secret = json.get("credentials").getAsJsonObject().get("client_secret").getAsString();
			JsonElement signature = json.get("credentials").getAsJsonObject().get("signature");

			Logging.logTiming("REGISTER", start, Logging.getTime());

			return new RegistrationResponse(id, secret, signature);

		} catch (URISyntaxException e) {
			Logging.error(e.getMessage());
			Logging.logTiming("REGISTER_ERROR", start, Logging.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "URISyntaxException", e.getMessage());
		} catch (UnsupportedEncodingException e) {
			Logging.error(e.getMessage());
			Logging.logTiming("REGISTER_ERROR", start, Logging.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e.getMessage());
		} catch (ParseException e) {
			Logging.error(e.getMessage());
			Logging.logTiming("REGISTER_ERROR", start, Logging.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "ParseException", e.getMessage());
		} catch (IOException e) {
			Logging.error(e.getMessage());
			Logging.logTiming("REGISTER_ERROR", start, Logging.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				Logging.error(e.getMessage());
				Logging.logTiming("REGISTER_ERROR", start, Logging.getTime());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
			}
		}
	}

	public Response requestToken(String authorization,int timeout) {
		Logging.log("oauth","TOKEN_REQUEST: " + authorization);

		CloseableHttpResponse response = null;
		long start = Logging.getTime();

		try {
			URI uri = new URI(oauthProperties.getTokenRequestUrl());

			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setHeader("Accept", "application/json");
			httpRequest.setHeader("Authorization", authorization);

			// Set timeout
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
					.build();
			httpRequest.setConfig(requestConfig);

			try {
				response = httpClient.execute(httpRequest);
				// break;
			} catch (IOException e) {
				Logging.error("HTTP EXECUTE: " + e.getMessage());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "HttpExecute", e.getMessage());
			}

			Logging.log("oauth","Response: " + response);
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			EntityUtils.consume(entity);

			// Parse response
			JsonObject json = new Gson().fromJson(jsonResponse,JsonObject.class);

			if (json.has("error")) {
				Logging.logTiming("TOKEN_REQUEST", start, Logging.getTime());
				ErrorResponse error = new ErrorResponse(json.get("status_code").getAsInt(),
						json.get("error").getAsString(), json.get("error_description").getAsString());
				return error;
			}

			return new JWTResponse(json);
		} catch (Exception e) {
			Logging.error(e.getMessage());
			Logging.logTiming("TOKEN_REQUEST", start, Logging.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Exception", e.getMessage());
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				Logging.error(e.getMessage());
				Logging.logTiming("TOKEN_REQUEST", start, Logging.getTime());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
			}
		}
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
}
