/* This is the SEPA producer interface
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

package com.vaimee.sepa.pattern;

import com.vaimee.sepa.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.response.Response;

public interface IProducer {
	 public Response update(long timeout,long nRetry) throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException;
	 public Response update() throws SEPASecurityException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException;
}
