package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;

import org.apache.http.impl.nio.SSLNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class HttpsServer implements Runnable {
	protected Logger logger = LogManager.getLogger("HttpServer");

	// Create server-side I/O reactor
	private IOEventDispatch ioEventDispatch;
	private ListeningIOReactor ioReactor;

	public HttpsServer(EngineProperties properties, Scheduler scheduler, AuthorizationManager oauth) throws IllegalArgumentException,KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

		if (properties == null || oauth == null || scheduler == null) throw new IllegalArgumentException("One or more argument are null");
		
		// HTTP parameters for the server
		ConnectionConfig config = ConnectionConfig.DEFAULT;
		InetSocketAddress address = new InetSocketAddress(properties.getHttpsPort());

		// Create HTTP protocol processing chain
		HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
				// Use standard server-side protocol interceptors
				new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl() });

		// Create request handler registry
		UriHttpAsyncRequestHandlerMapper mapper = new UriHttpAsyncRequestHandlerMapper();

		// Register the default handler for all URIs
		HttpsRequestHandler handler = new HttpsRequestHandler(properties,scheduler,oauth);
		mapper.register("*", handler);

		// Create server-side HTTP protocol handler
		HttpAsyncService protocolHandler = new HttpAsyncService(httpproc, mapper);

		// Create HTTP connection factory
		NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;

		connFactory = new SSLNHttpServerConnectionFactory(oauth.getSSLContext(), null, config);

		// Create server-side I/O event dispatch
		ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);

		// Listen of the given port
		ioReactor = new DefaultListeningIOReactor();
		ioReactor.listen(address);

		// Welcome message
		try {
			System.out.println("SECURE Query URL: https://" + InetAddress.getLocalHost().getHostAddress() + ":"
					+ address.getPort() + handler.getSecurePath() + handler.getQueryPath());
		} catch (UnknownHostException e1) {
			logger.error(e1.getLocalizedMessage());
		}
		try {
			System.out.println("SECURE Update URL: https://" + InetAddress.getLocalHost().getHostAddress() + ":"
					+ address.getPort() + handler.getSecurePath() + handler.getUpdatePath());
		} catch (UnknownHostException e1) {
			logger.error(e1.getLocalizedMessage());
		}
		try {
			System.out.println("Regitration URL: https://" + InetAddress.getLocalHost().getHostAddress() + ":"
					+ address.getPort() + handler.getRegisterPath());
		} catch (UnknownHostException e1) {
			logger.error(e1.getLocalizedMessage());
		}
		try {
			System.out.println("Token request URL: https://" + InetAddress.getLocalHost().getHostAddress() + ":"
					+ address.getPort() + handler.getTokenRequestPath());
		} catch (UnknownHostException e1) {
			logger.error(e1.getLocalizedMessage());
		}

	}

	@Override
	public void run() {
		// Ready to go!
		try {
			ioReactor.execute(ioEventDispatch);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage());
		}
	}
}
