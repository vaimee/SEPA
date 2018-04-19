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
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;

public abstract class Client {	
	private final Logger logger = LogManager.getLogger("Client");
	
	protected HashMap<String,String> prefix2URIMap = new HashMap<String,String>();	
	protected ApplicationProfile appProfile;
	protected SPARQL11SEProtocol protocolClient = null;
	
	public ApplicationProfile getApplicationProfile() {
		return appProfile;
	}
	
	private void addNamespaces(ApplicationProfile appProfile) {
		Set<String> prefixes = appProfile.getPrefixes();
		for (String prefix : prefixes) {
			if (prefix2URIMap.containsKey(prefix)) {
				prefix2URIMap.remove(prefix);
			}
			prefix2URIMap.put(prefix, appProfile.getNamespaceURI(prefix));
		}
	}
	
	protected String prefixes() {
		String ret = "";
		for (String prefix : prefix2URIMap.keySet())
			ret += "PREFIX " + prefix + ":<" + prefix2URIMap.get(prefix) + "> ";
		return ret;
	}
	
	public Client(ApplicationProfile appProfile) throws SEPAProtocolException {
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Application profile is null"));
		}
		this.appProfile = appProfile;
		
		logger.debug("SEPA parameters: "+appProfile.printParameters());
		
		addNamespaces(appProfile);
	}
	
	protected String replaceBindings(String sparql, Bindings bindings){
		if (bindings == null || sparql == null) return sparql;
		
		String replacedSparql = String.format("%s", sparql);
		String selectPattern = "";
		
		if (sparql.toUpperCase().contains("SELECT")) {
			selectPattern = replacedSparql.substring(0, sparql.indexOf('{'));
			replacedSparql = replacedSparql.substring(sparql.indexOf('{'), replacedSparql.length());
		}
		for (String var : bindings.getVariables()) {
			if (bindings.getBindingValue(var) == null) continue;
			
			String value = bindings.getBindingValue(var);
			
			//Use single quote "'" so that the literal value can contain also double quotes """
			if (bindings.isLiteral(var)) value = "'"+value+"'";
			else {
				// See https://www.w3.org/TR/rdf-sparql-query/#QSynIRI
				// https://docs.oracle.com/javase/7/docs/api/java/net/URI.html
				
				// [scheme:]scheme-specific-part[#fragment]
				// An absolute URI specifies a scheme; a URI that is not absolute is said to be relative. 
				// URIs are also classified according to whether they are opaque or hierarchical.
				
				// An opaque URI is an absolute URI whose scheme-specific part does not begin with a slash character ('/'). 
				// Opaque URIs are not subject to further parsing.
				
				// A hierarchical URI is either an absolute URI whose scheme-specific part begins with a slash character, 
				// or a relative URI, that is, a URI that does not specify a scheme. 
				// A hierarchical URI is subject to further parsing according to the syntax
				// [scheme:][//authority][path][?query][#fragment]
				
				URI uri = null;
				try {
					uri = new URI(value);
				} catch (URISyntaxException e) {
					logger.error("Not a URI: "+value);
				}
				
				if (uri != null) {
					if(uri.getSchemeSpecificPart().startsWith("/")) value = "<"+value+">";	
				}
			}
			//Matching variables
			/*
			    [108]  	Var	  ::=  	VAR1 | VAR2
			    [143]  	VAR1	  ::=  	'?' VARNAME
				[144]  	VAR2	  ::=  	'$' VARNAME
			    [164]  	PN_CHARS_BASE	  ::=  	[A-Z] | [a-z] | [#x00C0-#x00D6] | [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] | [#x037F-#x1FFF] 
			    									| [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
				[165]  	PN_CHARS_U	  ::=  	PN_CHARS_BASE | '_'
  				[166]  	VARNAME	  ::=  	( PN_CHARS_U | [0-9] ) ( PN_CHARS_U | [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040] )*
			 * */
			int start = 0;
			while (start != -1) {
				int index = replacedSparql.indexOf("?"+var,start);
				if (index == -1) index = replacedSparql.indexOf("$"+var,start);
				if (index != -1) {
					start = index +1;
					int unicode = replacedSparql.codePointAt(index+var.length()+1);
					if (!isValidVarChar(unicode)) {
						replacedSparql = replacedSparql.substring(0, index) + value + replacedSparql.substring(index+var.length()+1);
					}
				} else start = index;
			}
			
			selectPattern = selectPattern.replace("?"+var, "");
		}
		
		return selectPattern+replacedSparql;
	}
	
	private boolean isValidVarChar(int c) {
		return ((c == '_') || (c == 0x00B7) || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040) || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9') || (0x00C0 <= c && c <= 0x00D6) || (0x00D8 <= c && c <= 0x00F6) || (0x00F8 <= c && c <= 0x02FF) || (0x0370 <= c && c <= 0x037D) 
				|| (0x037F <= c && c <= 0x1FFF) || (0x200C <= c && c <= 0x200D) || (0x2070 <= c && c <= 0x218F) || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF) 
				|| (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD) || (0x10000 <= c && c <= 0xEFFFF));
	}
}
