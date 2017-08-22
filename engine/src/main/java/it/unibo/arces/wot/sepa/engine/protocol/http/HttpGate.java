package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.ProtocolException;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.bean.HTTPGateBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

import it.unibo.arces.wot.sepa.engine.protocol.http.handler.QueryHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.UpdateHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.EchoHandler;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class HttpGate extends Thread implements HttpGateMBean{
	protected static final Logger logger = LogManager.getLogger("HttpGate");

	protected EngineProperties properties;
	protected Scheduler scheduler;
	
	protected String serverInfo = "SEPA Gate-HTTP/1.1";
	
	// JMX
	protected HTTPGateBeans jmx = new HTTPGateBeans();
	
	public HttpGate(EngineProperties properties, Scheduler scheduler) {
		this.properties = properties;
		this.scheduler = scheduler;
	
		// JMX
		SEPABeans.registerMBean("SEPA:type="+this.getClass().getSimpleName(), this);
	}

	protected static class StdErrorExceptionLogger implements ExceptionLogger {

		public StdErrorExceptionLogger() {
		}

		@Override
		public void log(final Exception ex) {

			if (ex.getClass().equals(ProtocolException.class)) {
				logger.fatal("ProtocolException : " + ex.getMessage());
			} else if (ex.getClass().equals(SocketTimeoutException.class)) {
				logger.warn("SocketTimeoutException : " + ex.getMessage());
			} else if (ex.getClass().equals(ConnectionClosedException.class)) {
				logger.warn("ConnectionClosedException : " + ex.getMessage());
			} else if (ex.getClass().equals(IOException.class)) {
				logger.warn("IOException : " + ex.getMessage());
			} 
			
			ex.printStackTrace();
		}

	}

	public void run() {		
		setName(serverInfo);

		 IOReactorConfig config = IOReactorConfig.custom()
	                .setSoTimeout(properties.getHttpTimeout())
	                .setTcpNoDelay(true)
	                .build();
		 
		 final HttpServer server = ServerBootstrap.bootstrap()
	                .setListenerPort(properties.getHttpPort())
	                .setServerInfo(serverInfo)
	                .setIOReactorConfig(config)
	                .setExceptionLogger(ExceptionLogger.STD_ERR)
	                .registerHandler(properties.getQueryPath(), new QueryHandler(scheduler, properties.getHttpTimeout()))
	                .registerHandler(properties.getUpdatePath(), new UpdateHandler(scheduler, properties.getHttpTimeout()))
	                .registerHandler("/echo", new EchoHandler())
	                .create();

		try {
			server.start();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		
		jmx.setQueryURL("http://" + server.getEndpoint().getAddress() + properties.getQueryPath());
		jmx.setUpdateURL("http://" + server.getEndpoint().getAddress() +properties.getUpdatePath());
		System.out.println("SPARQL 1.1 Query     | " + jmx.getQueryURL());
		System.out.println("SPARQL 1.1 Update    | " + jmx.getUpdateURL());

		synchronized (this) {
			notify();
		}

		try {
			server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.shutdown(5, TimeUnit.SECONDS);
			}
		});
	}

	@Override
	public String getQueryURL() {
		return jmx.getQueryURL();
	}

	@Override
	public String getUpdateURL() {
		return jmx.getUpdateURL();
	}
}
