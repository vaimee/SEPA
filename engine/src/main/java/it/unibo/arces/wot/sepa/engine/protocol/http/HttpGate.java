package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ExceptionLogger;

import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

import it.unibo.arces.wot.sepa.engine.protocol.http.handler.QueryHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.UpdateHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.EchoHandler;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class HttpGate extends Thread {
	protected static final Logger logger = LogManager.getLogger("HttpGate");

	protected EngineProperties properties;
	protected Scheduler scheduler;

	protected String serverInfo = "SEPA Gate-HTTP/1.1";
	protected HttpServer server = null;
	
	public HttpGate(EngineProperties properties, Scheduler scheduler) {
		this.properties = properties;
		this.scheduler = scheduler;
	}

	protected static class StdErrorExceptionLogger implements ExceptionLogger {

		public StdErrorExceptionLogger() {
		}

		@Override
		public void log(final Exception ex) {
			logger.error(ex.getMessage());
//			if (ex.getClass().equals(ProtocolException.class)) {
//				logger.fatal("ProtocolException : " + ex.getMessage());
//			} else if (ex.getClass().equals(SocketTimeoutException.class)) {
//				logger.warn("SocketTimeoutException : " + ex.getMessage());
//			} else if (ex.getClass().equals(ConnectionClosedException.class)) {
//				logger.warn("ConnectionClosedException : " + ex.getMessage());
//			} else if (ex.getClass().equals(IOException.class)) {
//				logger.warn("IOException : " + ex.getMessage());
//			}
//
//			ex.printStackTrace();
		}

	}

	public void init() throws IOException {
		setName(serverInfo);

		IOReactorConfig config = IOReactorConfig.custom().setSoTimeout(properties.getTimeout()).setTcpNoDelay(true)
				.build();

		server = ServerBootstrap.bootstrap().setListenerPort(properties.getHttpPort())
				.setServerInfo(serverInfo).setIOReactorConfig(config).setExceptionLogger(ExceptionLogger.STD_ERR)
				.registerHandler(properties.getQueryPath(), new QueryHandler(scheduler, properties.getTimeout()))
				.registerHandler(properties.getUpdatePath(), new UpdateHandler(scheduler, properties.getTimeout()))
				.registerHandler("/echo", new EchoHandler()).create();

		server.start();	
	}
	
	public void run() {
		EngineBeans.setQueryURL("http://" + server.getEndpoint().getAddress() + properties.getQueryPath());
		EngineBeans.setUpdateURL("http://" + server.getEndpoint().getAddress() + properties.getUpdatePath());

		System.out.println("SPARQL 1.1 Query     | " + EngineBeans.getQueryURL());
		System.out.println("SPARQL 1.1 Update    | " + EngineBeans.getUpdateURL());

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
}
