package it.unibo.arces.wot.sepa.commons.jsonld;

/**
 * The JsonLdErrorCode represents the collection of valid JSON-LD error codes.
<pre>
<b>colliding keywords</b>
Two properties which expand to the same keyword have been detected. This might occur if a keyword and an alias thereof are used at the same time.
<b>conflicting indexes</b>
Multiple conflicting indexes have been found for the same node.
<b>cyclic IRI mapping</b>
A cycle in IRI mappings has been detected.
<b>invalid @id value</b>
An @id member was encountered whose value was not a string.
<b>invalid @index value</b>
An @index member was encountered whose value was not a string.
<b>invalid @nest value</b>
An invalid value for @nest has been found.
<b>invalid @prefix value</b>
An invalid value for @prefix has been found.
<b>invalid @reverse value</b>
An invalid value for an @reverse member has been detected, i.e., the value was not a dictionary.
<b>invalid @version value</b>
The @version member was used in a context with an out of range value.
<b>invalid base IRI</b>
An invalid base IRI has been detected, i.e., it is neither an absolute IRI nor null.
<b>invalid container mapping</b>
An @container member was encountered whose value was not one of the following strings: @list, @set, or @index.
<b>invalid default language</b>
The value of the default language is not a string or null and thus invalid.
<b>invalid IRI mapping</b>
A local context contains a term that has an invalid or missing IRI mapping.
<b>invalid keyword alias</b>
An invalid keyword alias definition has been encountered.
<b>invalid language map value</b>
An invalid value in a language map has been detected. It MUST be a string or an array of strings.
<b>invalid language mapping</b>
An @language member in a term definition was encountered whose value was neither a string nor null and thus invalid.
<b>invalid language-tagged string</b>
A language-tagged string with an invalid language value was detected.
<b>invalid language-tagged value</b>
A number, true, or false with an associated language tag was detected.
<b>invalid local context</b>
In invalid local context was detected.
<b>invalid remote context</b>
No valid context document has been found for a referenced remote context.
<b>invalid reverse property</b>
An invalid reverse property definition has been detected.
<b>invalid reverse property map</b>
An invalid reverse property map has been detected. No keywords apart from @context are allowed in reverse property maps.
<b>invalid reverse property value</b>
An invalid value for a reverse property has been detected. The value of an inverse property must be a node object.
<b>invalid scoped context</b>
The local context defined within a term definition is invalid.
<b>invalid script element</b>
A script element in HTML input which is the target of a fragment identifier does not have an appropriate type attribute.
<b>invalid set or list object</b>
A set object or list object with disallowed members has been detected.
<b>invalid term definition</b>
An invalid term definition has been detected.
<b>invalid type mapping</b>
An @type member in a term definition was encountered whose value could not be expanded to an absolute IRI.
<b>invalid type value</b>
An invalid value for an @type member has been detected, i.e., the value was neither a string nor an array of strings.
<b>invalid typed value</b>
A typed value with an invalid type was detected.
<b>invalid value object</b>
A value object with disallowed members has been detected.
<b>invalid value object value</b>
An invalid value for the @value member of a value object has been detected, i.e., it is neither a scalar nor null.
<b>invalid vocab mapping</b>
An invalid vocabulary mapping has been detected, i.e., it is neither an absolute IRI nor null.
<b>keyword redefinition</b>
A keyword redefinition has been detected.
<b>loading document failed</b>
The document could not be loaded or parsed as JSON.
<b>loading remote context failed</b>
There was a problem encountered loading a remote context.
<b>multiple context link headers</b>
Multiple HTTP Link Headers [RFC8288] using the http://www.w3.org/ns/json-ld#context link relation have been detected.
<b>processing mode conflict</b>
An attempt was made to change the processing mode which is incompatible with the previous specified version.
<b>recursive context inclusion</b>
A cycle in remote context inclusions has been detected.
</pre>
 * */
public enum JsonLdErrorCode {
	COLLIDING_KEYWORDS, 
	CONFLICTING_INDEXES, 
	CYCLIC_IRI_MAPPING, 
	INVALID_ID_VALUE, 
	INVALID_INDEX_VALUE, 
	INVALID_NEST_VALUE, 
	INVALID_PREFIX_VALUE, 
	INVALID_REVERSE_VALUE, 
	INVALID_VERSION_VALUE, 
	INVALID_BASE_IRI, 
	INVALID_CONTAINER_MAPPING, 
	INVALID_DEFAULT_LANGUAGE, 
	INVALID_IRI_MAPPING, 
	INVALID_KEYWORD_ALIAS, 
	INVALID_LANGUAGE_MAP_VALUE, 
	INVALID_LANGUAGE_MAPPING, 
	INVALID_LANGUAGE_TAGGED_STRING, 
	INVALID_LANGUAGE_TAGGED_VALUE, 
	INVALID_LOCAL_CONTEXT, 
	INVALID_REMOTE_CONTEXT, 
	INVALID_REVERSE_PROPERTY, 
	INVALID_REVERSE_PROPERTY_MAP, 
	INVALID_REVERSE_PROPERTY_VALUE, 
	INVALID_SCOPED_CONTEXT, 
	INVALID_SCRIPT_ELEMENT, 
	INVALID_SET_OR_LIST_OBJECT, 
	INVALID_TERM_DEFINITION, 
	INVALID_TYPE_MAPPING, 
	INVALID_TYPE_VALUE, 
	INVALID_TYPED_VALUE, 
	INVALID_VALUE_OBJECT, 
	INVALID_VALUE_OBJECT_VALUE, 
	INVALID_VOCAB_MAPPING, 
	KEYWORD_REDEFINITION, 
	LOADING_DOCUMENT_FAILED, 
	LOADING_REMOTE_CONTEXT_FAILED, 
	MULTIPLE_CONTEXT_LINK_HEADERS, 
	PROCESSING_MODE_CONFLICT, 
	RECURSIVE_CONTEXT_INCLUSION
}