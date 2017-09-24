/* JMX interface for the engine core
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

public interface EngineMBean {	
	public String getUpTime();
	
	public String getURL_Query();
	public String getURL_Update();
	
	public String getURL_SecureQuery();
	public String getURL_SecureUpdate();
	
	public String getURL_Registration();
	public String getURL_TokenRequest();
	
	public String getEndpoint_Host();

	public String getEndpoint_Port();

	public String getEndpoint_QueryPath();

	public String getEndpoint_UpdatePath();

	public String getEndpoint_UpdateMethod();

	public String getEndpoint_QueryMethod();
	
	public void resetAll();
}
