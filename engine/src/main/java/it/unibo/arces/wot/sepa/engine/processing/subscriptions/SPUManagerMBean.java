/* JMX class to collect statistics on subscriptions management
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

package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

public interface SPUManagerMBean {
	public long getPreProcessingRequests();
	public long getPostProcessingRequests();
	public long getSubscribeRequests();
	public long getUnsubscribeRequests();

	public long getSPUs_current();
	public long getSPUs_max();
	public long getSubscribers();
	public long getSubscribers_max();
	
	public float getPreProcessing_SPUs_time();
	public float getPreProcessing_SPUs_time_min();
	public float getPreProcessing_SPUs_time_max();	
	public float getPreProcessing_SPUs_time_average();

	public float getPostProcessing_SPUs_time();
	public float getPostProcessing_SPUs_time_min();
	public float getPostProcessing_SPUs_time_max();	
	public float getPostProcessing_SPUs_time_average();
	
	public float getFiltering_time();
	public float getFiltering_time_min();
	public float getFiltering_time_max();	
	public float getFiltering_time_average();
	
	public void reset();
	
	public long getSPUProcessingTimeout();
	public void setSPUProcessingTimeout(long t);
	
	public void scale_ms();
	public void scale_us();
	public void scale_ns();
	public String getUnitScale();
	
	public long getPreProcessingExceptions();
	public long getPostProcessingExceptions();
	public long getNotifyExceptions();
}
