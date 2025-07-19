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

package com.vaimee.sepa.api.pattern;

import java.io.IOException;
import java.util.ArrayList;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.OAuthProperties;
import com.vaimee.sepa.api.commons.security.ClientSecurityManager;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.RDFTerm;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.logging.Logging;

public abstract class Client implements java.io.Closeable {

	protected long TIMEOUT = 60000;
	protected long NRETRY = 3;

	protected JSAP appProfile;
	protected ClientSecurityManager sm = null;

	public final boolean isSecure() {
		return appProfile.isSecure();
	}

	public JSAP getApplicationProfile() {
		return appProfile;
	}
	
	public Client(JSAP appProfile) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		if (appProfile == null) {
			Logging.getLogger().fatal("Application profile is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Application profile is null"));
		}
		this.appProfile = appProfile;

		Logging.getLogger().trace("SEPA parameters: " + appProfile);

		// Security manager
		if (appProfile.isSecure()) {
			OAuthProperties oauth = appProfile.getAuthenticationProperties();
			
			sm = new ClientSecurityManager(oauth);
			if (!oauth.isClientRegistered()) {
				sm.registerClient(oauth.getClientRegistrationId(),oauth.getUsername(),oauth.getInitialAccessToken());
			}
			
			if (oauth.isTokenExpired()) {
				Response ret = sm.refreshToken();
				if (ret.isError()) {
					Logging.getLogger().error(ret);
					throw new SEPASecurityException(ret.toString());
				}
			}
			
			oauth.storeProperties();
		}
	}

	@Override
	public void close() throws IOException {
		if (sm != null) sm.close();
	}

	/**
	 * Add the datatype for literals that are not specified in "bindings" but are
	 * specified in the corresponding JSAP entry (query or update) identified by
	 * "id".
	 * 
	 * <pre>
	17.1 Operand Data Types (https://www.w3.org/TR/sparql11-query/#operandDataTypes)
	 * </pre>
	 * 
	 * SPARQL functions and operators operate on RDF terms and SPARQL variables. A
	 * subset of these functions and operators are taken from the XQuery 1.0 and
	 * XPath 2.0 Functions and Operators [FUNCOP] and have XML Schema typed value
	 * arguments and return types. RDF typed literals passed as arguments to these
	 * functions and operators are mapped to XML Schema typed values with a string
	 * value of the lexical form and an atomic datatype corresponding to the
	 * datatype IRI. The returned typed values are mapped back to RDF typed literals
	 * the same way.
	 * <p>
	 * SPARQL has additional operators which operate on specific subsets of RDF
	 * terms. When referring to a type, the following terms denote a typed literal
	 * with the corresponding XML Schema [XSDT] datatype IRI:
	 * 
	 * <pre>
	xsd:integer
	xsd:decimal
	xsd:float
	xsd:double
	xsd:string
	xsd:boolean
	xsd:dateTime
	
	The following terms identify additional types used in SPARQL value tests:
	
	numeric denotes typed literals with datatypes xsd:integer, xsd:decimal, xsd:float, and xsd:double.
	simple literal denotes a plain literal with no language tag.
	RDF term denotes the types IRI, literal, and blank node.
	variable denotes a SPARQL variable.
	
	The following types are derived from numeric types and are valid arguments to functions and operators taking numeric arguments:
	
	xsd:nonPositiveInteger
	xsd:negativeInteger
	xsd:long
	xsd:int
	xsd:short
	xsd:byte
	xsd:nonNegativeInteger
	xsd:unsignedLong
	xsd:unsignedInt
	xsd:unsignedShort
	xsd:unsignedByte
	xsd:positiveInteger
	
	SPARQL language extensions may treat additional types as being derived from XML schema datatypes.
	 * </pre>
	 **/

	protected ArrayList<Bindings> addUpdateDefaultDatatype(ArrayList<Bindings> bindings, String id,boolean query) throws SEPABindingsException {
		ArrayList<Bindings> temp = new ArrayList<>();
		for (Bindings b : bindings) temp.add(addUpdateDefaultDatatype(b,id,query));
		return temp;
	}
	protected Bindings addUpdateDefaultDatatype(Bindings bindings, String id,boolean query) throws SEPABindingsException {
		if (id == null)
			return bindings;
		if (bindings == null)
			return null;

		// Forced bindings by JSAP
		Bindings jsap_template;
		if (query)
			jsap_template = appProfile.getQueryBindings(id);
		else
			jsap_template = appProfile.getUpdateBindings(id);

		// Add missing datatype, if any
		Bindings retBindings = new ForcedBindings();
		for (String varString : bindings.getVariables()) {
			RDFTerm term = bindings.getRDFTerm(varString);
			if (term == null) {
				retBindings.addBinding(varString,null);
				continue;
			}
			if (term.isLiteral()) {
				RDFTermLiteral literal = (RDFTermLiteral) term;
				if (literal.getDatatype() == null && jsap_template.getDatatype(varString) != null)
					retBindings.addBinding(varString,
							new RDFTermLiteral(literal.getValue(), jsap_template.getDatatype(varString)));
				else
					retBindings.addBinding(varString, term);

			} else
				retBindings.addBinding(varString, term);
		}
		return retBindings;
	}
}
