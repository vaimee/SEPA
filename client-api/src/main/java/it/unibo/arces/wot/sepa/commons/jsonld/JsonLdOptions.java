package it.unibo.arces.wot.sepa.commons.jsonld;

import org.apache.commons.rdf.api.IRI;

/**
 * The JsonLdOptions type is used to pass various options to the JsonLdProcessor methods.
 * 
<pre>
<b>base</b>
The base IRI to use when expanding or compacting the document. If set, this overrides the input document's IRI.
<b>compactArrays</b>
If set to true, the JSON-LD processor replaces arrays with just one element with that element during compaction. If set to false, all arrays will remain arrays even if they have just one element.
<b>compactToRelative</b>
Determines if IRIs are compacted relative to the base option or document location when compacting.
<b>documentLoader</b>
The callback of the loader to be used to retrieve remote documents and contexts. If specified, it is used to retrieve remote documents and contexts; otherwise, if not specified, the processor's built-in loader is used.
<b>expandContext</b>
A context that is used to initialize the active context when expanding a document.
<b>extractAllScripts</b>
If set to true, when extracting JSON-LD script elements from HTML, unless a specific fragment identifier is targeted, extracts all encountered JSON-LD script elements using an array form, if necessary.
<b>frameExpansion</b>
Enables special frame processing rules for the Expansion Algorithm.
Enables special rules for the Serialize RDF as JSON-LD Algorithm to use JSON-LD native types as values, where possible.
<b>ordered</b>
If set to true, certain algorithm processing steps where indicated are ordered lexicographically. If false, order is not considered in processing.
<b>processingMode</b>
Sets the processing mode. If set to json-ld-1.0 or json-ld-1.1, the implementation must produce exactly the same results as the algorithms defined in this specification. If set to another value, the JSON-LD processor is allowed to extend or modify the algorithms defined in this specification to enable application-specific optimizations. The definition of such optimizations is beyond the scope of this specification and thus not defined. Consequently, different implementations may implement different optimizations. Developers must not define modes beginning with json-ld as they are reserved for future versions of this specification.
<b>produceGeneralizedRdf</b>
If set to true, the JSON-LD processor may emit blank nodes for triple predicates, otherwise they will be omitted. Generalized RDF Datasets are defined in [RDF11-CONCEPTS].
FEATURE AT RISK
The use of blank node identifiers to label properties is obsolete, and may be removed in a future version of JSON-LD, as is the support for generalized RDF Datasets and thus the produceGeneralizedRdf option may be also be removed.
<b>useNativeTypes</b>
Causes the Serialize RDF as JSON-LD Algorithm to use native JSON values in value objects avoiding the need for an explicity @type.
<b>useRdfType</b>
Enables special rules for the Serialize RDF as JSON-LD Algorithm causing rdf:type properties to be kept as IRIs in the output, rather than use @type.
</pre>
 * */
public class JsonLdOptions {
	public enum PROCESSING_MODE {
		JSON_LD_1_0, JSON_LD_1_1
	}

	IRI base = null;
	boolean compactArrays = true;
	boolean compactToRelative = true;
	// LoadDocumentCallback documentLoader = null;
	// (JsonLdDictionary? or USVString) expandContext = null;
	boolean extractAllScripts = false;
	boolean frameExpansion = false;
	boolean ordered = false;
	PROCESSING_MODE processingMode = PROCESSING_MODE.JSON_LD_1_0;
	boolean produceGeneralizedRdf = true;
	boolean useNativeTypes = false;
	boolean useRdfType = false;
}
