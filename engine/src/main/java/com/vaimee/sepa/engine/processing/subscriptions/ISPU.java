/* Interface to be implemented by a Subscription Processing Unit (SPU)
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

import java.io.IOException;

import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.response.Response;
import com.vaimee.sepa.commons.sparql.BindingsResults;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;

interface ISPU {
	String getSPUID();
	
    Response init() throws SEPASecurityException, IOException;

    BindingsResults getLastBindings();

    void postUpdateProcessing(Response res);
    void preUpdateProcessing(InternalUpdateRequest req);
}
