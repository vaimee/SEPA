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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;

public abstract class Client implements IClient {	
	protected HashMap<String,String> URI2PrefixMap = new HashMap<String,String>();
	protected HashMap<String,String> prefix2URIMap = new HashMap<String,String>();
	
	protected SPARQL11SEProtocol protocolClient = null;
	
	private static final Logger logger = LogManager.getLogger("Client");
		
	/*protected void addNamespace(String prefix,String uri){
		if (prefix2URIMap.containsKey(prefix)) removeNamespace(prefix);
		URI2PrefixMap.put(uri, prefix);
		prefix2URIMap.put(prefix, uri);
	}
		
	protected void removeNamespace(String prefix){
		if (!prefix2URIMap.containsKey(prefix)) return;
		String rmURI = prefix2URIMap.get(prefix);
		URI2PrefixMap.remove(rmURI);
		prefix2URIMap.remove(prefix);
	}
	
	protected void clearNamespaces() {
		URI2PrefixMap.clear();
		prefix2URIMap.clear();
	}*/
	
	private void addNamespaces(ApplicationProfile appProfile) {
		Set<String> prefixes = appProfile.getPrefixes();
		for (String prefix : prefixes) {
			if (prefix2URIMap.containsKey(prefix)) {
				String rmURI = prefix2URIMap.get(prefix);
				URI2PrefixMap.remove(rmURI);
				prefix2URIMap.remove(prefix);
			}
			URI2PrefixMap.put(appProfile.getNamespaceURI(prefix), prefix);
			prefix2URIMap.put(prefix, appProfile.getNamespaceURI(prefix));
		}
	}
	
	protected String prefixes() {
		String ret = "";
		for (String prefix : prefix2URIMap.keySet())
			ret += "PREFIX " + prefix + ":<" + prefix2URIMap.get(prefix) + "> ";
		return ret;
	}
	
	public Client(ApplicationProfile appProfile) throws IllegalArgumentException {
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			throw new IllegalArgumentException("Application profile is null");
		}
		
		logger.debug("SEPA parameters: "+appProfile.printParameters());
		
		protocolClient = new SPARQL11SEProtocol(appProfile);
		
		addNamespaces(appProfile);
	}
	
	public Client(String jparFile) throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException {
		protocolClient = new SPARQL11SEProtocol(new SPARQL11SEProperties(jparFile));
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
			if (bindings.isLiteral(var)) 
				replacedSparql = replacedSparql.replace("?"+var,"\""+fixLiteralTerms(bindings.getBindingValue(var))+"\"");
			else	
				replacedSparql = replacedSparql.replace("?"+var,bindings.getBindingValue(var));
			
			selectPattern = selectPattern.replace("?"+var, "");
		}
		
		return selectPattern+replacedSparql;
	}
	
	protected String fixLiteralTerms(String s) {
		if (s == null) return s;
		return s.replace("\"", "\\\"");
	}
}
