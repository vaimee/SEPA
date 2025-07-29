/* A monitor class for subscription management
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

package com.vaimee.sepa.engine.processing.subscriptions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.vaimee.sepa.api.commons.exceptions.SEPANotExistsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.engine.bean.SPUManagerBeans;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.scheduling.InternalSubscribeRequest;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequestWithQuads;
import com.vaimee.sepa.logging.Logging;

/**
 * A monitor class for subscription management
 <pre>
 Terms
  
 - SPU (SPARQL Processing Unit): the thread in charge of processing a SPARQL query (InternalSubscribeRequest). Each SPU is identified by a unique SPUID 
 - Subscriber: a client subscribed to SEPA. Each subscriber is identified by a unique SID. A subscriber knows the SPU and the InternalSubscribeRequest.
 - An SPU can be associated to multiple subscribers (e.g., they share the same SPARQL query)
  
 HashMaps
 - subscribers: SID --> subscriber
 - handlers: SPUID --> Set of subscribers
 - requests:  InternalSubscribeRequest --> SPU
 - spus: SPUID --> SPU
 </pre>
  
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.9.12
 */
public class Subscriptions {
	// SID ==> Subscriber
	private static final HashMap<String, Subscriber> subscribers = new HashMap<String, Subscriber>();

	// SPUID ==> Subscribers
	private static final HashMap<String, HashSet<Subscriber>> handlers = new HashMap<String, HashSet<Subscriber>>();

	// Request ==> SPU
	private static final HashMap<InternalSubscribeRequest, SPU> requests = new HashMap<InternalSubscribeRequest, SPU>();

	// SPUID ==> SPU
	private final static HashMap<String, SPU> spus = new HashMap<String, SPU>();
	
	//TODO: a different SPU can be created based on the InternalSubscribeRequest
	public static SPU createSPU(InternalSubscribeRequest req, SPUManager manager) {
		Logging.log("subscriptions","@createSPU");
		
		try {
			return new SPUNaive(req, manager);
		} catch (SEPAProtocolException e) {
			return null;
		}
	}

	// First level filtering on RDF data set (graph uris)
	public synchronized static Collection<SPU> filterOnGraphs(InternalUpdateRequest update) {
		long start = Logging.getTime();
		
		Collection<SPU> ret = new HashSet<>();
		Set<String> target = update.getRdfDataSet();

		for (InternalSubscribeRequest sub : requests.keySet()) {
			Set<String> context = sub.getRdfDataSet();

			// All graphs: NO FILTER
			// TODO: default graph?
			if (context.isEmpty() && target.isEmpty()) {
				// DEFAULT GRAPHs
				ret.add(requests.get(sub));
			}
			else if (context.contains("*") || target.contains("*")) {
				ret.add(requests.get(sub));
			} else
				for (String graph : target) {
					if (context.contains(graph)) {
						ret.add(requests.get(sub));
						break;
					}
				}
		}
		long stop = Logging.getTime();
		
		SPUManagerBeans.filteringTimings(start, stop);
		
		Logging.log("subscriptions","Filtered spus: " + ret.size());
		
		return ret;
	}

	// Second level filtering (on quads)
	public static Collection<SPU> filterOnQuads(Collection<SPU> activeSpus, InternalUpdateRequestWithQuads update) {
		// TODO implement LUTT filtering
		return activeSpus;
	}
	
	public synchronized static boolean containsSubscribe(InternalSubscribeRequest req) {
		Logging.log("subscriptions","@containsSubscribe");
		return requests.containsKey(req);
	}

	public synchronized static void registerSubscribe(InternalSubscribeRequest req, SPU spu) {
		Logging.log("subscriptions","@registerSubscribe");
		
		if (requests.containsKey(req)) return;
		
		// Link the request with the SPU
		requests.put(req, spu);
		
		// New entry for subscribers
		handlers.put(spu.getSPUID(), new HashSet<Subscriber>());
		
		// Add the SPU to the collection
		spus.put(spu.getSPUID(), spu);
				
		SPUManagerBeans.setActiveSPUs(handlers.size());
				
		Logging.log("subscriptions","@registerSubscribe SPU activated: " + spu.getSPUID() + " total (" + handlers.size() + ")");
	}

	public synchronized static SPU getSPU(InternalSubscribeRequest req) {
		return requests.get(req);
	}

	public synchronized static Subscriber addSubscriber(InternalSubscribeRequest req, SPU spu) {
		Logging.log("subscriptions","@addSubscriber");
		
		// Create a new subscriber
		Subscriber sub = new Subscriber(spu, req);
		
		// Add subscriber to the SPU
		handlers.get(spu.getSPUID()).add(sub);
		
		// Add subscriber to the subscribers map
		subscribers.put(sub.getSID(), sub);

		SPUManagerBeans.addSubscriber();
		
		// Link the subscriber with the gate
		Dependability.onSubscribe(req.getGID(), sub.getSID());

		return sub;
	}

//	public synchronized static Subscriber getSubscriber(String sid) throws SEPANotExistsException {
//		Logging.log(Level.getLevel("subscriptions"),"@getSubscriber "+sid);
//		
//		Subscriber sub = subscribers.get(sid);
//
//		if (sub == null)
//			throw new SEPANotExistsException("Subscriber " + sid + " does not exists");
//		return sub;
//	}

	/**
	 * Remove the subscriber and return true if it is the last of the SPU managed ones 
	 * */
	public synchronized static boolean removeSubscriber(String sid) throws SEPANotExistsException {
		if (!subscribers.containsKey(sid)) {
			Logging.warn("@removeSubscriber SID not found: " + sid);
			throw new SEPANotExistsException("SID not found: " + sid);
		}
		
		Subscriber sub = subscribers.get(sid);
		String spuid = sub.getSPU().getSPUID();

		Logging.log("subscriptions","@removeSubscriber "+sid+" "+spuid);
		
		Logging.log("subscriptions","@removeSubscriber SID: " + sid + " from SPU: " + spuid + " with active subscriptions: "
				+ subscribers.size());

		if (handlers.get(spuid) == null) return false;
		
		// Remove from maps
		subscribers.remove(sid);
		
		handlers.get(spuid).remove(sub);
	
		// No more handlers: return true
		if (handlers.get(spuid).isEmpty()) {
			Logging.log("subscriptions","@removeSubscriber no more subscribers. Kill SPU: " + sub.getSPU().getSPUID());

			requests.remove(sub.getSPU().getSubscribe());
			handlers.remove(spuid);

			// *** Kill SPU ***
			Logging.log("subscriptions", "Interrupt SPU " + spuid);
			spus.get(spuid).interrupt();

			// Clear
			Logging.log("subscriptions", "remove " + spuid);
			spus.remove(spuid);

			Logging.log("subscriptions", "@removeSubscriber active SPUs: " + spus.size());

			SPUManagerBeans.setActiveSPUs(spus.size());
			SPUManagerBeans.removeSubscriber();
			
			return true;
		}

		// More handlers
		return false;
	}

	public synchronized static void notifySubscribers(Notification notify) {
		Logging.log("subscriptions","@notifySubscribers");
		
		String spuid = notify.getSpuid();

		if (!spus.containsKey(spuid)) return;
		
		HashSet<Subscriber> brokenSubscribers = new HashSet<Subscriber>();		
		for (Subscriber client : handlers.get(spuid)) {
			// Dispatching events
			Notification event = new Notification(client.getSID(), notify.getARBindingsResults(),
					client.nextSequence());
			try {
				client.notifyEvent(event);
			} catch (SEPAProtocolException e) {
				Logging.error(e.getMessage());
				if (Logging.isTraceEnabled())
					e.printStackTrace();
				brokenSubscribers.add(client);
			}
		}
		
		for (Subscriber client : brokenSubscribers) {
			try {
				removeSubscriber(client.getSID());
			} catch (SEPANotExistsException e) {
				Logging.error(e.getMessage());
				if (Logging.isTraceEnabled())
					e.printStackTrace();
			}
		}
	}

	public synchronized static long size() {
		return spus.size();
	}

	public synchronized static boolean ping() {
		Logging.log("subscriptions","@ping");
		
		HashSet<Subscriber> brokenSubscribers = new HashSet<Subscriber>();		
		
		for (String spuid : spus.keySet()) {
		for (Subscriber client : handlers.get(spuid)) {
			if (!client.ping()) brokenSubscribers.add(client);
		}
		}
		
		for (Subscriber client : brokenSubscribers) {
			try {
				removeSubscriber(client.getSID());
			} catch (SEPANotExistsException e) {
				Logging.error(e.getMessage());
				if (Logging.isTraceEnabled())
					e.printStackTrace();
			}
		}
		
		return brokenSubscribers.size() != 0;
	}
}
