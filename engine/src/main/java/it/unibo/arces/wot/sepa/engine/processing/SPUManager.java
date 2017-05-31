/* This class implements the manager of the Semantic Processing Units (SPUs) of the Semantic Event Processing Architecture (SEPA) Engine
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

package it.unibo.arces.wot.sepa.engine.processing;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;

public class SPUManager extends Observable implements Observer,SPUManagerMBean{
	private static final Logger logger = LogManager.getLogger("SPUManager");
	private SPARQL11Protocol endpoint;
	private HashMap<String,SPU> spus = new HashMap<String,SPU>();
	protected static String mBeanName = "SEPA:type=SPUManager";
	
	//Sequential update processing
	private static int subscriptionsChecked = 0;
	
	public SPUManager(SPARQL11Protocol endpoint) {
		this.endpoint = endpoint;
		SEPABeans.registerMBean(mBeanName, this);
	}
	
	public void processSubscribe(SubscribeRequest req) {
		logger.debug("Process SUBSCRIBE #"+req.getToken());
		
		//TODO: choose different kinds of SPU based on subscription request
		SPU spu = new SPUNaive(req,endpoint);
		spu.addObserver(this);
		
		synchronized(spus) {
			spus.put(spu.getUUID(),spu);
		}
		
		Thread th = new Thread(spu);
		th.setName("SPU_"+spu.getUUID());
		th.start();	
	}
	
	public String processUnsubscribe(UnsubscribeRequest req) {
		logger.debug("Process UNSUBSCRIBE #"+req.getToken());
		String spuid = req.getSubscribeUUID();
		
		synchronized(spus){
			if (spus.containsKey(spuid)){
				spus.get(spuid).stopRunning();
				spus.remove(spuid);
			}
		}
		
		return spuid;
	}
	
	public void processUpdate(UpdateResponse res) {
		logger.debug( "*** PROCESSING UPDATE STARTED ***");
		
		//Sequential update processing
		waitAllSubscriptionChecks(res);
		
		logger.debug( "*** PROCESSING UPDATE FINISHED ***");
	}

	private synchronized void waitAllSubscriptionChecks(UpdateResponse res) {			
		subscriptionsChecked = 0;
		
		synchronized(spus) {
			//Wake-up all SPUs
			logger.debug( "Activate SPUs (Total: "+spus.size()+")");
			for (SPU spu: spus.values()) spu.subscriptionCheck(res);
			
			logger.debug(  "Waiting all SPUs to complete processing...");		
			while (subscriptionsChecked != spus.size()) {
				try {
					wait();
				} catch (InterruptedException e) {
					logger.debug(  "SPUs processing ended "+subscriptionsChecked+"/"+spus.size());
				}
			}
		}
	}
	
	private synchronized void subscriptionProcessingEnded(String spuid){
		subscriptionsChecked++;
		notifyAll();
		logger.debug("SPU processing ended #"+subscriptionsChecked);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg.getClass().equals(Notification.class)){
			Notification ret = (Notification) arg;
			
			//SPU processing ended
			logger.debug( "SPU "+ret.getSPUID()+" proccesing ended");
			subscriptionProcessingEnded(ret.getSPUID());
						
			//Send notification if required
			if (!ret.toBeNotified()) return;
			else {
				logger.debug( "Notify observers");
				setChanged();
				notifyObservers(ret);
			}
		}
		else {
			logger.debug( "Notify observers");
			setChanged();
			notifyObservers(arg);
		}
	}

	@Override
	public long getActiveSPUs() {
		return spus.size();
	}
}
