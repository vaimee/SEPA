package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.EchoHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.RegisterHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.SecureQueryHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.SecureUpdateHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.TokenRequestHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class HttpsGate extends HttpGate implements HttpsGateMBean {
	private static final Logger logger = LogManager.getLogger("HttpsGate");
	
	protected String serverInfo = "SEPA Secure Gate-HTTP/1.1";
	protected String welcomeMessage;
	
	protected AuthorizationManager oauth;

	public HttpsGate(EngineProperties properties, Scheduler scheduler, AuthorizationManager oauth) {
		super(properties, scheduler);

		this.oauth = oauth;
	}

	public void run() {
		setName(serverInfo);
		
		IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(properties.getHttpTimeout())
                .setTcpNoDelay(true)
                .build();
		
		final HttpServer server;
        try {
			server = ServerBootstrap.bootstrap()
			        .setListenerPort(properties.getHttpsPort())
			        .setServerInfo(serverInfo)
			        .setIOReactorConfig(config)
			        .setSslContext(oauth.getSSLContext())
			        .setExceptionLogger(ExceptionLogger.STD_ERR)
			        .registerHandler(properties.getRegisterPath(), new RegisterHandler(oauth))
				.registerHandler(properties.getSecurePath()+properties.getQueryPath(), new SecureQueryHandler(scheduler,oauth,properties.getHttpTimeout()))
				.registerHandler(properties.getSecurePath()+properties.getUpdatePath(), new SecureUpdateHandler(scheduler,oauth,properties.getHttpTimeout()))
				.registerHandler(properties.getTokenRequestPath(), new TokenRequestHandler(oauth))
				.registerHandler("/echo", new EchoHandler())
			        .create();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return;
		}

		try {
			server.start();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return;
		}
		
		jmx.setQueryURL("https://"+server.getEndpoint().getAddress()+properties.getSecurePath()+properties.getQueryPath());
		jmx.setUpdateURL("https://"+server.getEndpoint().getAddress()+properties.getSecurePath()+properties.getUpdatePath());
		jmx.setRegistrationURL("https://"+server.getEndpoint().getAddress()+properties.getRegisterPath());
		jmx.setTokenRequestURL("https://"+server.getEndpoint().getAddress()+properties.getTokenRequestPath());
		
		System.out.println("SPARQL 1.1 SE Query  | "+jmx.getQueryURL());
		System.out.println("SPARQL 1.1 SE Update | "+jmx.getUpdateURL());
		System.out.println("Client registration  | "+jmx.getRegistrationURL());
		System.out.println("Token request        | "+jmx.getTokenRequestURL());
		
		synchronized(this) {
			notify();
		}
		
		try {
			server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			return;
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.shutdown(5, TimeUnit.SECONDS);
			}
		});
	}

	@Override
	public String getRegistrationURL() {
		return jmx.getRegistrationURL();
	}

	@Override
	public String getTokenRequestURL() {
		return jmx.getTokenRequestURL();
	}
}
