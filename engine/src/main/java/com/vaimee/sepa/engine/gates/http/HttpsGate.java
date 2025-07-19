/* The main HTTP gate supporting the SPARQL 1.1 protocol over SSL
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.engine.gates.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.core.EngineProperties;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.protocol.oauth.JWTRequestHandler;
import com.vaimee.sepa.engine.protocol.oauth.RegisterHandler;
import com.vaimee.sepa.engine.protocol.sparql11.SecureSPARQL11Handler;
import com.vaimee.sepa.engine.scheduling.Scheduler;
import com.vaimee.sepa.logging.Logging;

public class HttpsGate {
	protected EngineProperties properties;
	protected Scheduler scheduler;

	protected String serverInfo = "SEPA Gate-HTTPS/1.1";
	protected HttpServer server = null;

	protected IOReactorConfig config = IOReactorConfig.custom().setTcpNoDelay(true).setSoReuseAddress(true).build();

	public HttpsGate(EngineProperties properties, Scheduler scheduler) throws SEPASecurityException, SEPAProtocolException {

		try {
//			SecureSPARQL11Handler handler = new SecureSPARQL11Handler(scheduler,properties.getSecurePath() + properties.getQueryPath(),properties.getSecurePath() + properties.getUpdatePath());
			SecureSPARQL11Handler handler = new SecureSPARQL11Handler(scheduler,properties.getQueryPath(),properties.getUpdatePath());
			
			server = ServerBootstrap.bootstrap().setListenerPort(443).setServerInfo(serverInfo)
					.setIOReactorConfig(config).setSslContext(Dependability.getSSLContext())
					.setExceptionLogger(ExceptionLogger.STD_ERR)
					.registerHandler(properties.getRegisterPath(), new RegisterHandler())
					.registerHandler(properties.getQueryPath(),handler)
					.registerHandler(properties.getUpdatePath(),handler)
					.registerHandler(properties.getTokenRequestPath(), new JWTRequestHandler())
					.registerHandler("/echo", new EchoHandler())
					.registerHandler("", new EchoHandler()).create();
		} catch (IllegalArgumentException e) {
			throw new SEPASecurityException(e);
		}
		
		try {
			server.start();
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}

		System.out.println("SPARQL 1.1 SE Query  | " + EngineBeans.getSecureQueryURL());
		System.out.println("SPARQL 1.1 SE Update | " + EngineBeans.getSecureUpdateURL());
		System.out.println("Client registration  | " + EngineBeans.getRegistrationURL());
		System.out.println("Token request        | " + EngineBeans.getTokenRequestURL());
	}
	
	public void shutdown() {
		server.shutdown(5, TimeUnit.SECONDS);
		
		try {
			server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Logging.getLogger().debug(serverInfo+" interrupted: " + e.getMessage());
		}
	}
}
