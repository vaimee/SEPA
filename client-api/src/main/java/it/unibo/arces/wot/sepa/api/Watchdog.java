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

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

import org.apache.logging.log4j.LogManager;

public class Watchdog extends Thread {

	private long pingPeriod = 0;
	private long firstPing = 0;
	private long DEFAULT_PING_PERIOD = 5000;

	private boolean pingReceived = false;
	private SubscriptionState state = SubscriptionState.INIT;
	private FireBrokenSubscription fire = null;
	
	private static final Logger logger = LogManager.getLogger("WebsocketWatchdog");

	private INotificationHandler handler = null;
	private Websocket wsClient;
	private String sparql;
	private String token;
	private String alias;

	public Watchdog(INotificationHandler handler, Websocket wsClient, String sparql, String alias, String token) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = token;
		this.alias = alias;
	}

	public Watchdog(INotificationHandler handler, Websocket wsClient, String sparql, String alias) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = null;
		this.alias = alias;
	}

	public Watchdog(INotificationHandler handler, Websocket wsClient, String sparql) {
		this.handler = handler;
		this.wsClient = wsClient;
		this.sparql = sparql;
		this.token = null;
		this.alias = null;
	}

	private class FireBrokenSubscription extends Thread {
		private long sleep;

		public FireBrokenSubscription(long timeout) {
			sleep = timeout;
		}

		public void run() {
			pingReceived = false;

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				logger.debug("Broken alert interrupted");
				return;
			}

			if (!pingReceived) {
				if (handler != null)
					handler.onBrokenSocket();

				synchronized (state) {
					state = SubscriptionState.BROKEN_SOCKET;
				}
			}
		}
	}

	public synchronized void ping() {
		logger.debug("Ping!");

		pingReceived = true;

		if (firstPing == 0)
			firstPing = System.currentTimeMillis();
		else {
			pingPeriod = System.currentTimeMillis() - firstPing;
			firstPing = 0;
			logger.debug("Ping period: " + pingPeriod);
		}
		
		if (fire!=null) fire.interrupt();
	}

	public void subscribed() {
		synchronized (state) {
			state = SubscriptionState.SUBSCRIBED;
		}
		logger.debug("Subscribed");
	}

	public void unsubscribed() {
		synchronized (state) {
			state = SubscriptionState.UNSUBSCRIBED;
		}
		logger.debug("Unsubscribed");
	}

	public void run() {
		logger.debug("Calculate ping period...");
		try {
			Thread.sleep(DEFAULT_PING_PERIOD * 5 / 2);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
			return;
		}
		
		while (true) {
			logger.debug("State: " + state);
			switch (state) {
			
			case SUBSCRIBED:
				logger.debug("Wait for ping...");

				fire = new FireBrokenSubscription(pingPeriod*3/2);
				fire.start();
				try {
					synchronized (fire) {
						fire.wait();
					}
				} catch (InterruptedException e1) {
					logger.debug(e1.getMessage());
				}
				break;
			
			case BROKEN_SOCKET:
				logger.warn("Broken socket: try to recover subscription...");

				Response response =	wsClient.subscribe(sparql, alias, token);
				
				if (!response.getClass().equals(ErrorResponse.class)) {
					subscribed();
					continue;
				}

				try {
					Thread.sleep(Websocket.SUBSCRIBE_TIMEOUT);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage());
					return;
				}

				break;
			default:
				logger.debug("Sleep for a while...");
				
				try {
					Thread.sleep(DEFAULT_PING_PERIOD * 5 / 2);
				} catch (InterruptedException e) {
					logger.warn(e.getMessage());
					return;
				}
				break;
			}
		}
	}
}
