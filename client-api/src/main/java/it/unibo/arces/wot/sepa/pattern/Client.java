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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;

public abstract class Client implements java.io.Closeable {
	protected final Logger logger = LogManager.getLogger();

	protected static ArrayList<String> numbersOrBoolean = new ArrayList<String>();
	
	protected JSAP appProfile;
	protected String prefixes = "";
	protected boolean isSecure = false;
	
	protected SEPASecurityManager sm = null;
	
	public final boolean isSecure() {return isSecure;}

	public JSAP getApplicationProfile() {
		return appProfile;
	}

	protected final void addNamespaces(JSAP appProfile) {
		Set<String> appPrefixes = appProfile.getPrefixes();
		for (String prefix : appPrefixes) {
			prefixes += "PREFIX " + prefix + ":<" + appProfile.getNamespaceURI(prefix) + "> ";
		}
	}

	protected final String prefixes() {
		return prefixes;
	}

	public Client(JSAP appProfile) throws SEPAProtocolException {
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Application profile is null"));
		}
		this.appProfile = appProfile;

		logger.trace("SEPA parameters: " + appProfile.printParameters());

		addNamespaces(appProfile);
	
		isSecure = appProfile.isSecure();
		
		numbersOrBoolean.add("xsd:integer");
		numbersOrBoolean.add("xsd:decimal");
		numbersOrBoolean.add("xsd:double");
		numbersOrBoolean.add("xsd:boolean");
		
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#integer");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#decimal");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#double");
		numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#boolean");
	}

	public Client(JSAP appProfile,SEPASecurityManager sm) throws SEPAProtocolException {
		this(appProfile);
		
		if (sm == null & appProfile.isSecure()) throw new IllegalArgumentException("Security is enabled but security manager is null");
		
		this.sm = sm;
	}
	
	protected final String replaceBindings(String sparql, Bindings bindings) {
		if (bindings == null || sparql == null)
			return sparql;

		String replacedSparql = String.format("%s", sparql);
		String selectPattern = "";

		if (sparql.toUpperCase().contains("SELECT")) {
			selectPattern = replacedSparql.substring(0, sparql.indexOf('{'));
			replacedSparql = replacedSparql.substring(sparql.indexOf('{'), replacedSparql.length());
		}
		for (String var : bindings.getVariables()) {
			String value = bindings.getValue(var);
			if (value == null)
				continue;

			/*
			 * 4.1.2 Syntax for Literals
			 * 
			 * The general syntax for literals is a string (enclosed in either double
			 * quotes, "...", or single quotes, '...'), with either an optional language tag
			 * (introduced by @) or an optional datatype IRI or prefixed name (introduced by
			 * ^^).
			 * 
			 * As a convenience, integers can be written directly (without quotation marks
			 * and an explicit datatype IRI) and are interpreted as typed literals of
			 * datatype xsd:integer; decimal numbers for which there is '.' in the number
			 * but no exponent are interpreted as xsd:decimal; and numbers with exponents
			 * are interpreted as xsd:double. Values of type xsd:boolean can also be written
			 * as true or false.
			 * 
			 * To facilitate writing literal values which themselves contain quotation marks
			 * or which are long and contain newline characters, SPARQL provides an
			 * additional quoting construct in which literals are enclosed in three single-
			 * or double-quotation marks.
			 * 
			 * Examples of literal syntax in SPARQL include:
			 * 
			 * - "chat" - 'chat'@fr with language tag "fr" -
			 * "xyz"^^<http://example.org/ns/userDatatype> - "abc"^^appNS:appDataType -
			 * '''The librarian said, "Perhaps you would enjoy 'War and Peace'."''' - 1,
			 * which is the same as "1"^^xsd:integer - 1.3, which is the same as
			 * "1.3"^^xsd:decimal - 1.300, which is the same as "1.300"^^xsd:decimal -
			 * 1.0e6, which is the same as "1.0e6"^^xsd:double - true, which is the same as
			 * "true"^^xsd:boolean - false, which is the same as "false"^^xsd:boolean
			 * 
			 * Tokens matching the productions INTEGER, DECIMAL, DOUBLE and BooleanLiteral
			 * are equivalent to a typed literal with the lexical value of the token and the
			 * corresponding datatype (xsd:integer, xsd:decimal, xsd:double, xsd:boolean).
			 */

			if (bindings.isLiteral(var)) {
				String datatype = bindings.getDatatype(var);
				String lang = bindings.getLanguage(var);
				
				// Not a number of boolean
				if (!numbersOrBoolean.contains(datatype)) {
					value = "'" + value + "'";
					
					// Check if datatype is a qname or not
					URI uri = null;
					try {
						uri = new URI(datatype);
					} catch (URISyntaxException e) {
						logger.error(e.getMessage());
					}
					
					if (uri != null) {
						if (uri.getSchemeSpecificPart().startsWith("/"))
							datatype = "<" + datatype + ">";
					}
					
					if (lang != null)
						value += "@" + bindings.getLanguage(var);
					else
						value += "^^" + datatype;
				}
			} else {
				// See https://www.w3.org/TR/rdf-sparql-query/#QSynIRI
				// https://docs.oracle.com/javase/7/docs/api/java/net/URI.html

				// [scheme:]scheme-specific-part[#fragment]
				// An absolute URI specifies a scheme; a URI that is not absolute is said to be
				// relative.
				// URIs are also classified according to whether they are opaque or
				// hierarchical.

				// An opaque URI is an absolute URI whose scheme-specific part does not begin
				// with a slash character ('/').
				// Opaque URIs are not subject to further parsing.

				// A hierarchical URI is either an absolute URI whose scheme-specific part
				// begins with a slash character,
				// or a relative URI, that is, a URI that does not specify a scheme.
				// A hierarchical URI is subject to further parsing according to the syntax
				// [scheme:][//authority][path][?query][#fragment]

				URI uri = null;
				try {
					uri = new URI(value);
				} catch (URISyntaxException e) {
					logger.error(e.getMessage());
				}

				if (uri != null) {
					if (uri.getSchemeSpecificPart().startsWith("/"))
						value = "<" + value + ">";
				}
			}
			// Matching variables
			/*
			 * [108] Var ::= VAR1 | VAR2 [143] VAR1 ::= '?' VARNAME [144] VAR2 ::= '$'
			 * VARNAME [164] PN_CHARS_BASE ::= [A-Z] | [a-z] | [#x00C0-#x00D6] |
			 * [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] | [#x037F-#x1FFF] |
			 * [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] |
			 * [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF] [165] PN_CHARS_U ::=
			 * PN_CHARS_BASE | '_' [166] VARNAME ::= ( PN_CHARS_U | [0-9] ) ( PN_CHARS_U |
			 * [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040] )*
			 */
			int start = 0;
			while (start != -1) {
				int index = replacedSparql.indexOf("?" + var, start);
				if (index == -1)
					index = replacedSparql.indexOf("$" + var, start);
				if (index != -1) {
					start = index + 1;
					int unicode = replacedSparql.codePointAt(index + var.length() + 1);
					if (!isValidVarChar(unicode)) {
						replacedSparql = replacedSparql.substring(0, index) + value
								+ replacedSparql.substring(index + var.length() + 1);
					}
				} else
					start = index;
			}

			selectPattern = selectPattern.replace("?" + var, "");
		}

		return selectPattern + replacedSparql;
	}

	private boolean isValidVarChar(int c) {
		return ((c == '_') || (c == 0x00B7) || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040)
				|| ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9')
				|| (0x00C0 <= c && c <= 0x00D6) || (0x00D8 <= c && c <= 0x00F6) || (0x00F8 <= c && c <= 0x02FF)
				|| (0x0370 <= c && c <= 0x037D) || (0x037F <= c && c <= 0x1FFF) || (0x200C <= c && c <= 0x200D)
				|| (0x2070 <= c && c <= 0x218F) || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
				|| (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD) || (0x10000 <= c && c <= 0xEFFFF));
	}

//	protected boolean getToken() throws SEPASecurityException, IOException, SEPAPropertiesException {
//		try {
//			if (!appProfile.getAuthenticationProperties().isTokenExpired())
//				return true;
//		} catch (Exception e) {
//			logger.error("Authentication properties not found");
//			throw new SEPASecurityException(new NullPointerException("Authentication properties not found"));
//		}
//
//		Response ret = sm.requestToken(appProfile.getAuthenticationProperties().getTokenRequestUrl(),
//				appProfile.getAuthenticationProperties().getBasicAuthorizationHeader());
//
//		if (ret.isJWTResponse()) {
//			JWTResponse token = (JWTResponse) ret;
//			Date expires = new Date();
//			expires.setTime(expires.getTime() + (1000 * token.getExpiresIn()));
//			appProfile.getAuthenticationProperties().setJWT(token.getAccessToken(), expires, token.getTokenType());
//			return true;
//		}
//
//		logger.error(ret);
//		return false;
//	}
}
