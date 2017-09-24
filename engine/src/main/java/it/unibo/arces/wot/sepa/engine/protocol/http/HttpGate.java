package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ExceptionLogger;

import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;
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

		try{
			server.start();	
		}
		catch(BindException | IOReactorException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	public void run() {
		String address = server.getEndpoint().getAddress().toString();
		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		EngineBeans.setQueryURL("http://" + address + ":" + properties.getHttpPort()+properties.getQueryPath());
		EngineBeans.setUpdateURL("http://" + address + ":" + properties.getHttpPort()+properties.getUpdatePath());

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
