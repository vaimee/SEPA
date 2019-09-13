/* This class abstracts a client of the SEPA Application Design Pattern
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

package it.unibo.arces.wot.sepa.pattern;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;

public abstract class Client implements java.io.Closeable {
	protected final Logger logger = LogManager.getLogger();
	
	protected JSAP appProfile;
	protected SEPASecurityManager sm = null;
	
	public final boolean isSecure() {return appProfile.isSecure();}

	public JSAP getApplicationProfile() {
		return appProfile;
	}

	public Client(JSAP appProfile,SEPASecurityManager sm) throws SEPAProtocolException {
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Application profile is null"));
		}
		this.appProfile = appProfile;

		logger.trace("SEPA parameters: " + appProfile.printParameters());
		
		// Security manager
		if (appProfile.isSecure() && sm == null) throw new IllegalArgumentException("Security is enabled but SEPA security manager is null");
		this.sm = sm;
	}
	
	protected Bindings addDefaultDatatype(Bindings bindings,String id,boolean query) throws SEPABindingsException {
		if (id == null) return bindings;
		if (bindings == null)
			return null;
		
		// Forced bindings by JSAP
		Bindings fb;
		if (query) fb = appProfile.getQueryBindings(id);
		else fb = appProfile.getUpdateBindings(id);
		
		// Add missing datatype is any
		Bindings retBindings = new Bindings();
		for (String varString : bindings.getVariables()) {
			RDFTerm term = bindings.getRDFTerm(varString);
			if (term.isLiteral()) {
				RDFTermLiteral literal = (RDFTermLiteral) term;
				if (literal.getDatatype() == null && fb.getDatatype(varString) !=null) retBindings.addBinding(varString, new RDFTermLiteral(literal.getValue(), fb.getDatatype(varString)));
				else retBindings.addBinding(varString, term);
				
			}
			else retBindings.addBinding(varString, term);
		}
		return retBindings;
	}
}
