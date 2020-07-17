/* JMX monitoring of dependability parameters
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

//
//import it.unibo.arces.wot.sepa.engine.bean.AuthorizationManagerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;

public class DependabilityMonitor implements DependabilityMonitorMBean {
	
	public DependabilityMonitor() {
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	@Override
	public long getNumberOfGates() {
		return GatesMonitor.getNumberOfGates();
	}

	@Override
	public boolean getSecure() {
		return Dependability.isSecure();
	}
}
