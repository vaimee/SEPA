/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;

class WebsocketWatchdog extends Thread {
	
	private long pingPeriod = 0;
	private long firstPing = 0;
	private long DEFAULT_PING_PERIOD = 5000;
	private long DEFAULT_SUBSCRIPTION_DELAY = 5000;
	
	private boolean pingReceived = false;		
	private SubscriptionState state = SubscriptionState.UNSUBSCRIBED;
	
	private static final Logger logger = LogManager.getLogger("WebsocketWatchdog");
	
	private INotificationHandler handler = null;
	private WebsocketClientEndpoint wsClient;
	private String sparql;
	private String token;
	private String alias;
	
	public WebsocketWatchdog(INotificationHandler handler, WebsocketClientEndpoint wsClient,String sparql,String alias,String token) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = token;
		this.alias = alias;
	}
	
	public WebsocketWatchdog(INotificationHandler handler, WebsocketClientEndpoint wsClient,String sparql,String alias) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = null;
		this.alias = alias;
	}
	
	public WebsocketWatchdog(INotificationHandler handler, WebsocketClientEndpoint wsClient,String sparql) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = null;
		this.alias = null;
	}
	
	public synchronized void ping() {
		logger.debug("Ping!");
		pingReceived = true;
		if (firstPing == 0) firstPing = System.currentTimeMillis();
		else {
			pingPeriod = System.currentTimeMillis() - firstPing;	
			firstPing = 0;
			logger.debug("Ping period: "+pingPeriod);
		}
		notifyAll();
	}
	
	public void subscribed() {
		state = SubscriptionState.SUBSCRIBED;
		logger.debug("Subscribed");
	}
	
	public void unsubscribed() {
		state = SubscriptionState.UNSUBSCRIBED;
		logger.debug("Unsubscribed");
	}
	
	private synchronized boolean waitPing() {
		logger.debug("Wait ping...");
		pingReceived = false;
		try {
			if (pingPeriod != 0) wait(pingPeriod*3/2);
			else wait(DEFAULT_PING_PERIOD*3/2);
		} catch (InterruptedException e) {

		}	
		return pingReceived;
	}
	
	private synchronized boolean subscribing() throws DeploymentException, IOException, URISyntaxException {
		logger.debug("Subscribing...");
		if (wsClient == null) {
			logger.warn("Websocket client is null");
			return false;
		}
		while(state == SubscriptionState.BROKEN_SOCKET) {
			if (wsClient.isConnected()) wsClient.close();
			
			wsClient.subscribe(sparql,alias,token,handler);
			
			try {
				wait(DEFAULT_SUBSCRIPTION_DELAY);
			} catch (InterruptedException e) {

			}
		}
		return (state == SubscriptionState.SUBSCRIBED);
	}
	
	public void run() {
		try {
			Thread.sleep(DEFAULT_PING_PERIOD*5/2);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
			return;
		}
		
		while(true){
			while (waitPing()) {}
			
			if (state == SubscriptionState.SUBSCRIBED) {
				if (handler != null) handler.onBrokenSubscription();
				state = SubscriptionState.BROKEN_SOCKET;
			}
			
			try {
				if(!subscribing()) return;
			} catch (DeploymentException | IOException | URISyntaxException e) {
				logger.error(e.getMessage());
				return;
			}
		}
	}
}
