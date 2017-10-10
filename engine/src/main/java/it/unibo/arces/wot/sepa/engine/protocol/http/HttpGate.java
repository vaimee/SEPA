package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
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

public class HttpGate {//extends Thread {
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
		}

	}

	public void init() throws IOException {
		//setName(serverInfo);

		IOReactorConfig config = IOReactorConfig.custom().setTcpNoDelay(true)
				.setSoReuseAddress(true).build();

		server = ServerBootstrap.bootstrap().setListenerPort(properties.getHttpPort())
				.setServerInfo(serverInfo).setIOReactorConfig(config).setExceptionLogger(ExceptionLogger.STD_ERR)
				.registerHandler(properties.getQueryPath(), new QueryHandler(scheduler))
				.registerHandler(properties.getUpdatePath(), new UpdateHandler(scheduler))
				.registerHandler("/echo", new EchoHandler()).create();
	}
	
	public void shutdown() {
		server.shutdown(5, TimeUnit.SECONDS);
		
		try {
			server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.info("HTTP gate interrupted: " + e.getMessage());
		}
	}
	
	//@Override
	public void start() throws IOException {
		server.start();	
		
		if(server.getEndpoint().getException()!=null) {
			throw new IOException(server.getEndpoint().getException().getMessage());	
		}
		
		String address = server.getEndpoint().getAddress().toString();
		
		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			throw new IOException(e1.getMessage());
		}
		EngineBeans.setQueryURL("http://" + address + ":" + properties.getHttpPort()+properties.getQueryPath());
		EngineBeans.setUpdateURL("http://" + address + ":" + properties.getHttpPort()+properties.getUpdatePath());

		System.out.println("SPARQL 1.1 Query     | " + EngineBeans.getQueryURL());
		System.out.println("SPARQL 1.1 Update    | " + EngineBeans.getUpdateURL());

		synchronized (this) {
			notify();
		}
	}
}
