package it.unibo.arces.wot.sepa.commons.jsonld.context;

/**
 * Each term definition consists of 
 * 1) an IRI mapping, 
 * 2) a boolean flag reverse property, 
 * 3) an optional type mapping or language mapping, 
 * 4) an optional context, 
 * 5) an optional nest value, 
 * 6) an optional prefix flag, and 
 * 7) an optional container mapping. 
 * 
 * A term definition can not only be used to map a term to an IRI, but also to map a term to a keyword, in which case it is referred 
 * to as a keyword alias.
 * */

public class TermDefinition {
	private String mapping = null;
	private Boolean keywordAlias = null;	
	private Boolean reversePropertyFlag = null;
	
	private String type = null;					//optional
	private String language = null;				//optional
	private JsonLdContext context = null;		//optional
	private JsonLdContext nestValue = null;		//optional
	private Boolean prefixFlag = null;			//optional
	private JsonLdContext container = null;		//optional
			
	public void setTypeMapping(String mapping) {
		this.type = mapping;
	}

	public void setIriMapping(String mapping) {
		this.mapping = mapping;
	}

	public void setContainerMapping(JsonLdContext mapping) {
		this.container = mapping;
	}

	public void setReversePropertyFlag(boolean flag) {
		this.reversePropertyFlag = new Boolean(flag);
	}

	public void setPrefixFlag(boolean flag) {
		this.prefixFlag = new Boolean(flag);
		
	}

	public String getIriMapping() {
		return this.mapping;
	}

	public void setLocalContext(JsonLdContext localContext) {
		this.context = localContext;
	}

	public void setLanguageMapping(String mapping) {
		this.language = mapping;
	}

	public void setNestValue(JsonLdContext value) {
		nestValue = value;
	}

	public boolean iriMappingEndsWithGenDelim() {
		// TODO Auto-generated method stub
		return false;
	}
}
