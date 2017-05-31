/* This class implements a token handler used to assign tokens to requests and responses
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

package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.Vector;

import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

import org.apache.logging.log4j.LogManager;

/**
 * Utility class to handle requests' tokens
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class TokenHandler implements TokenHandlerMBean {
	private static final Logger logger = LogManager.getLogger("TokenHandler");
	
	private EngineProperties properties;
	private Vector<Integer> jar=new Vector<Integer>();
	
	public TokenHandler(EngineProperties properties)  throws IllegalArgumentException{
		if (properties == null) {
			logger.error("Properties are null");
			throw new IllegalArgumentException("Engine properties is null");
		}
		this.properties = properties;
		
		for (int i=0; i < getSchedulingQueueSize(); i++) jar.addElement(i);
		
		SEPABeans.registerMBean("SEPA:type=TokenHandler",this);
	}
	
	/**
	 * Returns a new token if more tokens are available or -1 otherwise
	 * @returns an int representing the token
	 */
	public int getToken()
	{
		Integer token;
		
		synchronized (jar){
			if (jar.size() == 0){
				logger.warn("No token available...wait...");
				try {
					jar.wait(getSchedulingTimeout());
				} catch (InterruptedException e) {
					logger.debug(e.getMessage());
				}
			}
			if (jar.size()==0) return -1;
		
			token =  jar.get(0);
			jar.removeElementAt(0);
		}
		
		logger.debug("Get token #"+token+" (Available: " + jar.size()+")");
		
		return token;	
	}

	/**
	 * Release an used token
	 * @returns true if success, false if the token to be released has not been acquired
	 */
	public boolean releaseToken(Integer token)
	{	
		boolean ret = true;
     	synchronized(jar) {
     		if (jar.contains(token)) {
     			ret = false;
     			logger.warn("Request to release a unused token: "+token+" (Available tokens: " + jar.size()+")");	
     		}
     		else
     		{
         		jar.insertElementAt( token , jar.size());
         		jar.notify();
         		logger.debug("Release token #"+token+" (Available: " + jar.size()+")");
         	}	
     	}
     	
     	return ret;
	}

	@Override
	public int getSchedulingQueueSize() {
		return properties.getSchedulingQueueSize();
	}

	@Override
	public long getSchedulingTimeout() {
		return properties.getSchedulingTimeout();
	}
}
