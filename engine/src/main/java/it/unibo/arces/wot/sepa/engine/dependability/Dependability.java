/* Main entry point for dependability management
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

package it.unibo.arces.wot.sepa.engine.dependability;

import javax.net.ssl.SSLContext;

import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.gates.Gate;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

public class Dependability {
	protected static Logger logger = LogManager.getLogger();
	
	private static boolean isSecure = false;
	private static SecurityManager authManager = null;
		
	private static String keystore = null;
	private static String keypass = null;
	
	public static boolean isSecure() {
		return isSecure;
	}
		
	public static void enableLDAP(String host, int port, String base, String uid, String pwd) throws SEPASecurityException {
		if (authManager == null) throw new SEPASecurityException("Authorization manager is null");
		
		authManager.enableLDAP(host, port, base, uid, pwd);
	}
	
	public static void enableSecurity(String keystoreFileName,String keystorePwd,String keyAlias) throws SEPASecurityException {
		authManager = new SecurityManager(keystoreFileName,keystorePwd,keyAlias);
		
		isSecure = true;
		
		keystore = keystoreFileName;
		keypass = keystorePwd;
		
		authManager.setSSLContextFromJKS(keystore, keypass);
	}
	
	public static SSLContext getSSLContext() throws SEPASecurityException {
		if (authManager == null) throw new SEPASecurityException("Authorization manager is null. First call enableSecurity()");
		
		return authManager.getSSLContext();
	}
	
	public static Response getToken(String encodedCredentials) throws SEPASecurityException {
		if (authManager == null) throw new SEPASecurityException("Authorization manager is null. First call enableSecurity()");
		
		return authManager.getToken(encodedCredentials);
	}

	public static Response register(String identity) throws SEPASecurityException {
		if (authManager == null) throw new SEPASecurityException("Authorization manager is null. First call enableSecurity()");
		
		return authManager.register(identity);
	}

	public static ClientAuthorization validateToken(String jwt) throws SEPASecurityException {
		if (authManager == null) throw new SEPASecurityException("Authorization manager is null. First call enableSecurity()");
		
		return authManager.validateToken(jwt);
	}
	
	public static void setProcessor(Processor p) {
		GatesMonitor.setProcessor(p);
	}
	
	public static void onCloseGate(String gid) throws InterruptedException {
		GatesMonitor.onClose(gid);
	}

	public static void addGate(Gate g)  {
		GatesMonitor.addGate(g);
	}
	
	public static void removeGate(Gate g)  {
		GatesMonitor.removeGate(g);
	}
	
	public static void onGateError(String gid, Exception e) {
		GatesMonitor.onError(gid, e);
	}

	public static void onSubscribe(String gid, String sid) {
		GatesMonitor.onSubscribe(gid, sid);
	}

	public static void onUnsubscribe(String gid, String sid) {
		GatesMonitor.onUnsubscribe(gid, sid);
	}

	public static boolean processCORSRequest(HttpAsyncExchange exchange) {
		return CORSManager.processCORSRequest(exchange);
	}

	public static boolean isPreFlightRequest(HttpAsyncExchange exchange) {
		return CORSManager.isPreFlightRequest(exchange);
	}

	

}
