/* This class is used for the smooth closing of a running instance of the engine
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

package it.unibo.arces.wot.sepa.engine.core;

import it.unibo.arces.wot.sepa.logging.Logging;

public class EngineShutdownHook extends Thread {
	private Engine engine;
	
	public EngineShutdownHook(Engine engine) {
		this.engine = engine;
		this.setName("SEPA Shutdown hook");
	}
	
	public void run() {
		try {
			engine.shutdown();
		} catch (InterruptedException e) {
			Logging.logger.error(e.getMessage());
		}
	}
}
