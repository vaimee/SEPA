package it.unibo.arces.wot.sepa.engine.protocol.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.protocol.handler.RegisterHandler;
import it.unibo.arces.wot.sepa.engine.protocol.handler.SecureQueryHandler;
import it.unibo.arces.wot.sepa.engine.protocol.handler.SecureUpdateHandler;
import it.unibo.arces.wot.sepa.engine.protocol.handler.TokenRequestHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class HttpsRequestHandler extends HttpRequestHandler implements HttpsRequestHandlerMBean {
	protected String registerPath;
	protected String tokenRequestPath;
	private long registrationRequests;
	
	protected AuthorizationManager am;
	private long accessTokenRequests;

	private String securePath;
	
	protected String getMBeanName() {
		return "SEPA:type=HTTPS";
	}

	public HttpsRequestHandler(EngineProperties properties, SchedulerInterface scheduler, AuthorizationManager am)
			throws IllegalArgumentException {
		super(properties, scheduler);

		registerPath = properties.getRegisterPath();
		tokenRequestPath = properties.getTokenRequestPath();

		this.am = am;
		
		securePath = properties.getSecurePath();
	}

	@Override
	protected void processRequestUri(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context) {
		logger.debug(request.getRequestLine().getUri());

		String requestUri = request.getRequestLine().getUri();

		if (requestUri.equals(securePath+updatePath)){
			updateRequests++;
			new SecureUpdateHandler(request, httpExchange, context, scheduler, am, timeout).start();
		}
		else if(requestUri.equals(securePath+queryPath)){
			queryRequests++;
			new SecureQueryHandler(request, httpExchange, context, scheduler, am, timeout).start();
		}
		else if (requestUri.equals(tokenRequestPath)){
			accessTokenRequests++;
			new TokenRequestHandler(request, httpExchange, context, scheduler, am, timeout).start();
		}
		else if (requestUri.equals(registerPath)){
			registrationRequests++;
			new RegisterHandler(request, httpExchange, context, scheduler, am, timeout).start();
		}
		else if (requestUri.equals(securePath+"/echo"))
			Utilities.sendResponse(httpExchange, HttpStatus.SC_OK, Utilities.buildEchoResponse(request).toString());
		else
			Utilities.failureResponse(httpExchange, HttpStatus.SC_NOT_FOUND, request.getRequestLine().getUri());
	}

	@Override
	public String getRegisterPath() {
		return registerPath;
	}

	@Override
	public String getTokenRequestPath() {
		return tokenRequestPath;
	}

	@Override
	public long getRegitrationRequests() {
		return registrationRequests;
	}

	@Override
	public long getAccessTokenRequests() {
		return accessTokenRequests;
	}
	
	public String getSecurePath(){
		return securePath;
	}
}
