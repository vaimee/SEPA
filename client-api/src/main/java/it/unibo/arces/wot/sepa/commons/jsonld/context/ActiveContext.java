package it.unibo.arces.wot.sepa.commons.jsonld.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdDictionary;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdError;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdErrorCode;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdException;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions.PROCESSING_MODE;

/**
 * The active context contains the active term definitions which specify how
 * keys and values have to be interpreted as well as the current base IRI, the
 * vocabulary mapping and the default language.
 */
public class ActiveContext {
	private Map<String, TermDefinition> termDefinitions = new HashMap<String, TermDefinition>();
	private String base = null;
	private String vocabulary = null;
	private String language = null;
	private HashMap<JsonLdContext, JsonLdContext> deferencedContexts = new HashMap<JsonLdContext, JsonLdContext>();
	private PROCESSING_MODE processingMode = PROCESSING_MODE.JSON_LD_1_1;

	/**
	 * This algorithm specifies how a new active context is updated with a local
	 * context. The algorithm takes three input variables: an active context, a
	 * local context, and an array remote contexts which is used to detect cyclical
	 * context inclusions. If remote contexts is not passed, it is initialized to an
	 * empty array.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public ActiveContext contextProcessing(ActiveContext active, JsonLdContext local)
			throws JsonLdException, CloneNotSupportedException {
		return contextProcessing(active, local, new ArrayList<JsonLdContext>());
	}

	public ActiveContext contextProcessing(ActiveContext active, JsonLdContext local, ArrayList<JsonLdContext> remote)
			throws JsonLdException, CloneNotSupportedException {
		// 1) Initialize result to the result of cloning active context.
		ActiveContext result = (ActiveContext) active.clone();

		// 2) If local context is not an array, set it to an array containing only local
		// context.
		if (!local.isArray()) {
			ArrayList<JsonLdContext> temp = new ArrayList<JsonLdContext>();
			temp.add(local);
			local = new JsonLdContext(temp);
		}

		// 3) For each item context in local context:
		Iterator<JsonLdContext> it = local.iterator();
		while (it.hasNext()) {
			JsonLdContext context = it.next();
			// 3.1) If context is null, set result to a newly-initialized active context and
			// continue with the next context. In JSON-LD 1.0, the base IRI was given a
			// default value here; this is now described conditionally in section 10. The
			// Application Programming Interface.
			if (context == null)
				continue;

			// 3.2) If context is a string,
			if (context.isString()) {
				// 3.2.1) Set context to the result of resolving value against the base IRI
				// which is
				// established as specified in section 5.1 Establishing a Base URI of [RFC3986].
				// Only the basic algorithm in section 5.2 of [RFC3986] is used; neither
				// Syntax-Based Normalization nor Scheme-Based Normalization are performed.
				// Characters additionally allowed in IRI references are treated in the same way
				// that unreserved characters are treated in URI references, per section 6.5 of
				// [RFC3987].

				// 3.2.2) If context is in the remote contexts array, a recursive context
				// inclusion
				// error has been detected and processing is aborted; otherwise, add context to
				// remote contexts.
				if (remote.contains(context)) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.RECURSIVE_CONTEXT_INCLUSION,
							context.toString());
					throw new JsonLdException(error);
				}
				remote.add(context);

				// 3.2.3) If context was previously dereferenced, then the processor MUST NOT do
				// a
				// further dereference, and context is set to the previously established
				// internal representation.
				JsonLdContext deferenced = null;
				if (deferencedContexts.containsKey(context))
					deferenced = deferencedContexts.get(context);

				// 3.2.4) Otherwise, dereference context, transforming into the internal
				// representation. If context cannot be dereferenced, or cannot be transformed
				// into the internal representation, a loading remote context failed error has
				// been detected and processing is aborted. If the dereferenced document has no
				// top-level dictionary with an @context member, an invalid remote context has
				// been detected and processing is aborted; otherwise, set context to the value
				// of that member.
				else {
					deferenced = deferenceContext(context);
					deferencedContexts.put(context, deferenced);
				}

				// 3.2.5) Set result to the result of recursively calling this algorithm,
				// passing result for active context, context for local context, and a copy of
				// remote contexts.
				result = contextProcessing(result, deferenced, JsonLdContext.copy(remote));

				// 3.2.6) Continue with the next context.
				continue;
			}

			// 3.3) If context is not a dictionary, an invalid local context error has been
			// detected and processing is aborted.
			if (!context.isDictionary()) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_LOCAL_CONTEXT, context.toString());
				throw new JsonLdException(error);
			}

			// 3.4) If context has an @base member and remote contexts is empty, i.e., the
			// currently being processed context is not a remote context:
			if (context.containsMember("@base") && !remote.isEmpty()) {
				// 3.4.1) Initialize value to the value associated with the @base member.
				JsonLdContext value = context.getMember("@base");

				// 3.4.2) If value is null, remove the base IRI of result.
				if (value == null)
					base = null;

				// 3.4.3) Otherwise, if value is an absolute IRI, the base IRI of result is set
				// to
				// value.
				else if (value.isAbsoluteIri())
					base = value.getString();

				// 3.4.4) Otherwise, if value is a relative IRI and the base IRI of result is
				// not null,
				// set the base IRI of result to the result of resolving value against the
				// current base IRI of result.
				else if (base != null)
					base = value.resolve(base);

				// 3.4.5) Otherwise, an invalid base IRI error has been detected and processing
				// is
				// aborted.
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_BASE_IRI,
						"@base: " + value + " base IRI of results: " + base);
				throw new JsonLdException(error);
			}

			// 3.5) If context has an @version member:
			if (context.containsMember("@version")) {
				// 3.5.1) If the associated value is not 1.1, an invalid @version value has been
				// detected, and processing is aborted.
				PROCESSING_MODE value = context.getProcessingMode();
				if (!value.equals(PROCESSING_MODE.JSON_LD_1_1)) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_VERSION_VALUE, "@version is not 1.1");
					throw new JsonLdException(error);
				}

				// 3.5.2) If processing mode is set to json-ld-1.0, a processing mode conflict
				// error
				// has been detected and processing is aborted.
				if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_0)) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_LOCAL_CONTEXT, context.toString());
					throw new JsonLdException(error);
				}

				// 3.5.3) Set processing mode, to json-ld-1.1, if not already set.
				processingMode = value;
			}

			// 3.6) If context has an @vocab member:
			if (context.containsMember("@vocab")) {
				// 3.6.1) Initialize value to the value associated with the @vocab member.
				JsonLdContext value = context.getMember("@vocab");

				// 3.6.2) If value is null, remove any vocabulary mapping from result.
				if (value == null)
					vocabulary = null;

				// 3.6.3) Otherwise, if value the empty string (""), the effective value is the
				// current
				// base IRI.
				else if (value.isEmptyString())
					vocabulary = base;

				// 3.6.4) Otherwise, if value is an absolute IRI or blank node identifier, the
				// vocabulary mapping of result is set to value. If it is not an absolute IRI,
				// or a blank node identifier, an invalid vocab mapping error has been detected
				// and processing is aborted.
				// FEATURE AT RISK
				// The use of blank node identifiers to value for @vocab is obsolete, and may be
				// removed in a future version of JSON-LD.
				else if (value.isAbsoluteIri() || value.isBlankNodeIdentifier())
					vocabulary = value.getString();
				else {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_VOCAB_MAPPING, value.toString());
					throw new JsonLdException(error);
				}
			}
			// 3.7) If context has an @language member:
			if (context.containsMember("@language")) {
				// 3.7.1) Initialize value to the value associated with the @language member.
				JsonLdContext value = context.getMember("@language");

				// 3.7.2) If value is null, remove any default language from result.
				if (value == null)
					language = null;

				// 3.7.3) Otherwise, if value is string, the default language of result is set
				// to
				// lowercased value. If it is not a string, an invalid default language error
				// has been detected and processing is aborted.
				else if (value.isString())
					language = value.getString().toLowerCase();
				else {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_DEFAULT_LANGUAGE, value.toString());
					throw new JsonLdException(error);
				}
			}

			// 3.8) Create a dictionary defined to use to keep track of whether or not a
			// term has
			// already been defined or currently being defined during recursion.
			HashMap<String, Boolean> defined = new HashMap<String, Boolean>();

			// 3.9) For each key-value pair in context where key is not @base, @vocab,
			// @language,
			// or @version, invoke the Create Term Definition algorithm, passing result for
			// active context, context for local context, key, and defined.
			for (String term : context.geyKeys()) {
				createTermDefinition(result, context, term, defined);
			}
		}
		// 4) Return result.
		return result;
	}

	/**
	 * Term definitions are created by parsing the information in the given local
	 * context for the given term. If the given term is a compact IRI, it may omit
	 * an IRI mapping by depending on its prefix having its own term definition. If
	 * the prefix is a member in the local context, then its term definition must
	 * first be created, through recursion, before continuing. Because a term
	 * definition can depend on other term definitions, a mechanism must be used to
	 * detect cyclical dependencies. The solution employed here uses a map, defined,
	 * that keeps track of whether or not a term has been defined or is currently in
	 * the process of being defined. This map is checked before any recursion is
	 * attempted. After all dependencies for a term have been defined, the rest of
	 * the information in the local context for the given term is taken into
	 * account, creating the appropriate IRI mapping, container mapping, and type
	 * mapping or language mapping for the term.
	 */
	private void createTermDefinition(ActiveContext activeContext, JsonLdContext localContext, String term,
			HashMap<String, Boolean> defined) throws JsonLdException {
		// 1. If defined contains the member term and the associated value is true
		// (indicating that the term definition
		// has already been created), return.
		// Otherwise, if the value is false, a cyclic IRI mapping error has been
		// detected and processing is aborted.
		if (defined.containsKey(term)) {
			if (defined.get(term))
				return;
			JsonLdError error = new JsonLdError(JsonLdErrorCode.CYCLIC_IRI_MAPPING, term);
			throw new JsonLdException(error);

		}

		// 2 . Set the value associated with defined's term member to false. This
		// indicates that the term definition is now being created but is not yet
		// complete.
		defined.put(term, false);

		// 3. Initialize value to a copy of the value associated with the member term in
		// local context.
		JsonLdContext value = localContext.getMember(term);

		// 4. If processing mode is json-ld-1.1 and term is @type, value MUST be a
		// dictionary with the member @container and value @set.
		// Any other value means that a keyword redefinition error has been detected and
		// processing is aborted.
		if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_1) && term.equals("@type")) {
			if (value != null) {
				if (!value.isDictionary()) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.KEYWORD_REDEFINITION, term);
					throw new JsonLdException(error);
				}
				if (!value.containsMember("@container") || !value.containsMember("@set")) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.KEYWORD_REDEFINITION, term);
					throw new JsonLdException(error);
				}
			}
		}

		// 5. Otherwise, since keywords cannot be overridden, term MUST NOT be a keyword
		// and a keyword redefinition error has been detected and processing is aborted.
		if (JsonLdContext.isKeyword(term)) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.KEYWORD_REDEFINITION, term);
			throw new JsonLdException(error);
		}

		// 6. Remove any existing term definition for term in active context.
		activeContext.removeTermDefinition(term);

		// 7. If value is null or value is a dictionary containing the key-value pair
		// @id-null,
		// set the term definition in active context to null, set the value associated
		// with defined's member term to true, and return.
		if (value == null) {
			termDefinitions.put(term, null);
			defined.put(term, true);
			return;
		}
		if (value.isDictionary()) {
			if (value.containsMember("@id")) {
				if (value.getMember("@id") == null) {
					termDefinitions.put(term, null);
					defined.put(term, true);
					return;
				}
			}
		}

		// 8. Otherwise, if value is a string, convert it to a dictionary consisting of
		// a single member whose key is @id and whose value is value. Set simple term to
		// true.
		boolean simpleTerm;
		if (value.isString()) {
			simpleTerm = true;
			JsonLdDictionary dict = new JsonLdDictionary();
			dict.addMember("@id", value);
			value = new JsonLdContext(dict);
		}

		// 9. Otherwise, value MUST be a dictionary, if not, an invalid term definition
		// error has been detected and processing is aborted.
		// Set simple term to false.
		else if (!value.isDictionary()) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TERM_DEFINITION, term);
			throw new JsonLdException(error);
		}
		simpleTerm = false;

		// 10 . Create a new term definition, definition.
		TermDefinition definition = new TermDefinition();

		// 11. If value contains the member @type:
		if (value.containsMember("@type")) {
			// 11.1 Initialize type to the value associated with the @type member, which
			// MUST be a string. Otherwise, an invalid type mapping error has been detected
			// and processing is aborted.
			JsonLdContext type = value.getMember("@type");
			if (!type.isString()) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TYPE_MAPPING, term);
				throw new JsonLdException(error);
			}

			// 11.2 Set type to the result of using the IRI Expansion algorithm, passing
			// active context, type for value, true for vocab, local context, and defined.
			// If the expanded type is neither @id, nor @vocab, nor an absolute IRI, an
			// invalid type mapping error has been detected and processing is aborted.
			String iri = iriExpansion(activeContext, type.getString(), false, true, localContext, defined);
			if (!iri.equals("@id") && !iri.equals("@vocab") && !JsonLdContext.isAbsoluteIri(iri)) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TYPE_MAPPING, term);
				throw new JsonLdException(error);
			}

			// 11. 3 Set the type mapping for definition to type.
			definition.setTypeMapping(iri);
		}

		// 12. If value contains the member @reverse:
		if (value.containsMember("@reverse")) {
			// 12.1 If value contains @id or @nest, members, an invalid reverse property
			// error has been detected and processing is aborted.
			if (value.containsMember("@id") || value.containsMember("@nest")) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_REVERSE_PROPERTY, term);
				throw new JsonLdException(error);
			}

			// 12.2 If the value associated with the @reverse member is not a string, an
			// invalid IRI mapping error has been detected and processing is aborted.
			if (!value.getMember("@reverse").isString()) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_IRI_MAPPING, term);
				throw new JsonLdException(error);
			}

			// 12.3 Otherwise, set the IRI mapping of definition to the result of using the
			// IRI Expansion algorithm, passing active context, the value associated with
			// the @reverse member for value, true for vocab, local context, and defined.
			// If the result is neither an absolute IRI nor a blank node identifier, i.e.,
			// it contains no colon (:), an invalid IRI mapping error has been detected and
			// processing is aborted.
			else {
				String mapping = iriExpansion(activeContext, value.getMember("@reverse").getString(), false, true,
						localContext, defined);
				if (!JsonLdContext.isAbsoluteIri(mapping) && !JsonLdContext.isBlankNodeIdentifier(mapping)) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_IRI_MAPPING, term);
					throw new JsonLdException(error);
				}
				definition.setIriMapping(mapping);
			}

			// 12.4 If value contains an @container member, set the container mapping of
			// definition to its value; if its value is neither @set, nor @index, nor null,
			// an invalid reverse property error has been detected (reverse properties only
			// support set- and index-containers) and processing is aborted.
			if (value.containsMember("@container")) {
				JsonLdContext itsValue = value.getMember("@container");
				if (itsValue != null) {
					if (!itsValue.equals("@set") && !itsValue.getString().equals("@index")) {
						JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_REVERSE_PROPERTY, term);
						throw new JsonLdException(error);
					}
				}
				definition.setContainerMapping(itsValue);
			}

			// 12.5 Set the reverse property flag of definition to true.
			definition.setReversePropertyFlag(true);

			// 12.6 Set the term definition of term in active context to definition and the
			// value associated with defined's member term to true and return.
			termDefinitions.put(term, definition);
			defined.put(term, true);
			return;
		}

		// 13. Set the reverse property flag of definition to false.
		definition.setReversePropertyFlag(false);

		// 14. If value contains the member @id and its value does not equal term:
		if (value.containsMember("@id")) {
			if (value.getMember("@id").equals(term)) {
				// 14.1 If the value associated with the @id member is not a string, an invalid
				// IRI mapping error has been detected and processing is aborted.
				if (!value.isString()) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_IRI_MAPPING, term);
					throw new JsonLdException(error);
				}

				// 14.2 Otherwise, set the IRI mapping of definition to the result of using the
				// IRI Expansion algorithm, passing active context, the value associated with
				// the @id member for value, true for vocab, local context, and defined.
				// If the resulting IRI mapping is neither a keyword, nor an absolute IRI, nor a
				// blank
				// node identifier, an invalid IRI mapping error has been detected and
				// processing is aborted; if it equals @context, an invalid keyword alias error
				// has been detected and processing is aborted.
				String iri = iriExpansion(activeContext, value.getMember("@id").getString(), false, true, localContext,
						defined);
				if (!JsonLdContext.isKeyword(iri) && !JsonLdContext.isAbsoluteIri(iri)
						&& !JsonLdContext.isBlankNodeIdentifier(iri)) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_IRI_MAPPING, term);
					throw new JsonLdException(error);
				}
				if (iri.equals("@context")) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_KEYWORD_ALIAS, term);
					throw new JsonLdException(error);
				}
				definition.setIriMapping(iri);

				// 14.3 If term does not contain a colon (:), simple term is true, and the, IRI
				// mapping of definition ends with a URI gen-delim character, set the prefix
				// flag in definition to true.
				if (!term.contains(":") && simpleTerm && definition.iriMappingEndsWithGenDelim()) {
					simpleTerm = true;
					if (JsonLdContext.endsWithUriGenDelimChar(iri)) {
						definition.setPrefixFlag(true);
					}
				}
			}
		}

		// 15. Otherwise if the term contains a colon (:):
		if (term.contains(":")) {
			// 15.1 If term is a compact IRI with a prefix that is a member in local context
			// a dependency has been found. Use this algorithm recursively passing active
			// context, local context, the prefix as term, and defined.
			if (JsonLdContext.isCompactIri(term)) {
				String prefix = JsonLdContext.getPrefixFromCompactIri(term);
				if (localContext.containsMember(term)) {
					createTermDefinition(activeContext, localContext, prefix, defined);
				}

				// 15.2 If term's prefix has a term definition in active context, set the IRI
				// mapping of definition to the result of concatenating the value associated
				// with the prefix's IRI mapping and the term's suffix.
				if (activeContext.termDefinitions.containsKey(term)) {
					TermDefinition termDefinition = activeContext.termDefinitions.get(prefix);
					String iri = termDefinition.getIriMapping() + JsonLdContext.getSuffixFromCompactIri(term);
					definition.setIriMapping(iri);
				}
			}
			// 15.3 Otherwise, term is an absolute IRI or blank node identifier. Set the IRI
			// mapping of definition to term.
			else {
				definition.setIriMapping(term);
			}
		}

		// 16 Otherwise, if term is @type, set the IRI mapping of definition to @type.
		if (term.equals("@type")) {
			definition.setIriMapping("@type");
		}

		// 17 Otherwise, if active context has a vocabulary mapping, the IRI mapping of
		// definition is set to the result of concatenating the value associated with
		// the vocabulary mapping and term. If it does not have a vocabulary mapping, an
		// invalid IRI mapping error been detected and processing is aborted.
		else {
			if (activeContext.hasVocabularyMapping()) {
				String iri = activeContext.getVocabularyMapping() + term;
				definition.setIriMapping(iri);
			} else {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_IRI_MAPPING, term);
				throw new JsonLdException(error);
			}
		}

		// 18 If value contains the member @container:
		if (value.containsMember("@container")) {
			// 18.1 Initialize container to the value associated with the @container member,
			// which MUST be either @graph, @id, @index, @language, @list, @set, or @type.
			// or an array containing exactly any one of those keywords, an array containing
			// @graph and either @id or @index optionally including @set, or an array
			// containing a combination of @set and any of @index, @id, @type, @language in
			// any order . Otherwise, an invalid container mapping has been detected and
			// processing is aborted.
			JsonLdContext container = value.getMember("@container");
			if (!JsonLdContext.isValidContainer(container)) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_CONTAINER_MAPPING, term);
				throw new JsonLdException(error);
			}

			// 18.2 If processing mode is json-ld-1.0 and the container value is @graph,
			// @id, or @type, or is otherwise not a string, an invalid container mapping has
			// been detected and processing is aborted.
			if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_0)) {
				if (!container.isString()) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_CONTAINER_MAPPING, term);
					throw new JsonLdException(error);
				} else {
					String s = container.getString();
					if (s.equals("@graph") || s.equals("@id") || s.equals("@type")) {
						JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_CONTAINER_MAPPING, term);
						throw new JsonLdException(error);
					}
				}
			}

			// 18.3 Set the container mapping of definition to container.
			definition.setContainerMapping(container);
		}

		// 19 If value contains the member @context:
		if (value.containsMember("@context")) {
			// 19.1 If processing mode is json-ld-1.0, an invalid term definition has been
			// detected and processing is aborted.
			if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_0)) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TERM_DEFINITION, term);
				throw new JsonLdException(error);
			}

			// 19.2 Initialize context to the value associated with the @context member,
			// which is treated as a local context.
			JsonLdContext context = value.getMember("@context");

			// 19.3 Invoke the Context Processing algorithm using the active context and
			// context as local context. If any error is detected, an invalid scoped context
			// error has been detected and processing is aborted.
			try {
				contextProcessing(activeContext, context, new ArrayList<JsonLdContext>());
			} catch (CloneNotSupportedException e) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_SCOPED_CONTEXT, term);
				throw new JsonLdException(error);
			}

			// 19.4 Set the local context of definition to context.
			definition.setLocalContext(context);
		}

		// 20 If value contains the member @language and does not contain the member
		// @type:
		if (value.containsMember("@language") && !value.containsMember("@type")) {
			// 20.1 Initialize language to the value associated with the @language member,
			// which MUST be either null or a string. Otherwise, an invalid language mapping
			// error has been detected and processing is aborted.
			if (value.getMember("@language") != null) {
				if (!value.isString()) {
					JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_LANGUAGE_MAPPING, term);
					throw new JsonLdException(error);
				} else {
					// 20.2 If language is a string set it to lowercased language. Set the language
					// mapping of definition to language.
					String language = value.getString().toLowerCase();
					definition.setLanguageMapping(language);
				}
			}
		}

		// 21 If value contains the member @nest:
		if (value.containsMember("@nest")) {
			// 21.1 If processing mode is json-ld-1.0, an invalid term definition has been
			// detected and processing is aborted.
			if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_0)) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TERM_DEFINITION, term);
				throw new JsonLdException(error);
			}

			// 21.2 Initialize nest value in definition to the value associated with the
			// @nest member, which MUST be a string and MUST NOT be a keyword other than
			// @nest. Otherwise, an invalid @nest value error has been detected and
			// processing is aborted.
			if (!value.getMember("@nest").isString()) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_NEST_VALUE, term);
				throw new JsonLdException(error);
			} else {
				if (!value.getMember("@nest").getString().equals("@nest")) {
					if (JsonLdContext.isKeyword(value.getMember("@nest").getString())) {
						JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_NEST_VALUE, term);
						throw new JsonLdException(error);
					}
				}
			}
			definition.setNestValue(value.getMember("@nest"));
		}

		// 22 If value contains the member @prefix:
		if (value.containsMember("@prefix")) {
			// 22.1 If processing mode is json-ld-1.0, or if term contains a colon (:), an
			// invalid term definition has been detected and processing is aborted.
			if (processingMode.equals(PROCESSING_MODE.JSON_LD_1_0) || term.contains(":")) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TERM_DEFINITION, term);
				throw new JsonLdException(error);
			}

			// 22.2 Initialize the prefix flag to the value associated with the @prefix
			// member, which MUST be a boolean. Otherwise, an invalid @prefix value error
			// has been detected and processing is aborted.
			if (!value.getMember("@prefix").isBoolean()) {
				JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_PREFIX_VALUE, term);
				throw new JsonLdException(error);
			}
			definition.setPrefixFlag(value.getMember("@prefix").getBoolean());
		}

		// 23 If the value contains any member other than @id, @reverse, @container,
		// @context, @language, @nest, @prefix, or @type, an invalid term definition
		// error has been detected and processing is aborted.
		Set<String> members = value.getMembers();
		members.remove("@id");
		members.remove("@reverse");
		members.remove("@container");
		members.remove("@context");
		members.remove("@language");
		members.remove("@nest");
		members.remove("@prefix");
		members.remove("@type");
		if (!members.isEmpty()) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TERM_DEFINITION, term);
			throw new JsonLdException(error);
		}

		// 24 Set the term definition of term in active context to definition and set
		// the value associated with defined's member term to true.
		activeContext.termDefinitions.put(term, definition);
		defined.put(term, true);
	}

	/**
	 * The algorithm takes two required and four optional input variables. The
	 * required inputs are an active context and a value to be expanded. The
	 * optional inputs are two flags, document relative and vocab, that specifying
	 * whether value can be interpreted as a relative IRI against the document's
	 * base IRI or the active context's vocabulary mapping, respectively, and a
	 * local context and a map defined to be used when this algorithm is used during
	 * Context Processing. If not passed, the two flags are set to false and local
	 * context and defined are initialized to null.
	 * 
	 * @throws JsonLdException
	 */
	private String iriExpansion(ActiveContext activeContext, String value, boolean documentRelative, boolean vocab,
			JsonLdContext localContext, HashMap<String, Boolean> defined) throws JsonLdException {
		// 1. If value is a keyword or null, return value as is.
		if (JsonLdContext.isKeyword(value))
			return value;

		// 2. If local context is not null, it contains a member with a key that equals
		// value,
		// and the value of the member for value in defined is not true, invoke the
		// Create Term Definition algorithm,
		// passing active context, local context, value as term, and defined.
		// This will ensure that a term definition is created for value in active
		// context during Context Processing.
		if (localContext != null) {
			if (localContext.containsMember(value)) {
				Boolean def = defined.get(value);
				if (def == null) {
					createTermDefinition(activeContext, localContext, value, defined);
				} else if (!def) {
					createTermDefinition(activeContext, localContext, value, defined);
				}
			}
		}

		// 3. If active context has a term definition for value, and the associated IRI
		// mapping is a keyword, return that keyword.
		if (activeContext.containsTermDefinition(value)) {
			if (JsonLdContext.isKeyword(activeContext.getIriMapping(value)))
				return activeContext.getIriMapping(value);
		}

		// 4. If vocab is true and the active context has a term definition for value,
		// return the associated IRI mapping.
		if (vocab && activeContext.containsTermDefinition(value))
			return activeContext.getIriMapping(value);

		// 5. If value contains a colon (:), it is either an absolute IRI, a compact
		// IRI, or a blank node identifier:
		if (value.contains(":")) {
			// 5.1 Split value into a prefix and suffix at the first occurrence of a colon
			// (:).
			String prefix = value.split(":")[0];
			String suffix = value.split(":")[1];

			// 5.2 If prefix is underscore (_) or suffix begins with double-forward-slash
			// (//),
			// return value as it is already an absolute IRI or a blank node identifier.
			if (prefix.equals("_") || suffix.startsWith("//"))
				return value;

			// 5.3 If local context is not null, it contains a prefix member, and the value
			// of the prefix member in defined is not true,
			// invoke the Create Term Definition algorithm, passing active context, local
			// context, prefix as term, and defined.
			// This will ensure that a term definition is created for prefix in active
			// context during Context Processing.
			if (localContext != null) {
				if (localContext.containsMember(prefix)) {
					Boolean prefixValue = defined.get(prefix);
					if (prefixValue == null) {
						createTermDefinition(activeContext, localContext, prefix, defined);
					} else if (!prefixValue) {
						createTermDefinition(activeContext, localContext, prefix, defined);
					}
				}
			}

			// 5.4 If active context contains a term definition for prefix, return the
			// result of concatenating the IRI mapping associated with prefix and suffix.
			if (activeContext.containsTermDefinition(prefix))
				return activeContext.getIriMapping(prefix) + suffix;

			// 5.5 If value has the form of an absolute IRI, return value.
			if (JsonLdContext.isAbsoluteIri(value))
				return value;
		}

		// 6. If vocab is true, and active context has a vocabulary mapping,
		// return the result of concatenating the vocabulary mapping with value.
		if (vocab && activeContext.hasVocabularyMapping())
			return activeContext.getVocabularyMapping() + value;

		// 7. Otherwise, if document relative is true set value to the result of
		// resolving value against the base IRI.
		// Only the basic algorithm in section 5.2 of [RFC3986] is used; neither
		// Syntax-Based Normalization nor
		// Scheme-Based Normalization are performed. Characters additionally allowed in
		// IRI references are treated in the
		// same way that unreserved characters are treated in URI references, per
		// section 6.5 of [RFC3987].
		if (documentRelative)
			return JsonLdContext.resolve(base, value);

		// 8. Return value as is.
		return value;
	}

	private String getVocabularyMapping() {
		return vocabulary;
	}

	private boolean hasVocabularyMapping() {
		return vocabulary != null;
	}

	private String getIriMapping(String term) {
		return termDefinitions.get(term).getIriMapping();
	}

	private boolean containsTermDefinition(String value) {
		return termDefinitions.containsKey(value);
	}

	private void removeTermDefinition(String term) {
		termDefinitions.remove(term);
	}

	/**
	 * contextProcessing 3.2.4) Otherwise, dereference context, transforming into
	 * the internal representation. If context cannot be dereferenced, or cannot be
	 * transformed into the internal representation, a loading remote context failed
	 * error has been detected and processing is aborted. If the dereferenced
	 * document has no top-level dictionary with an @context member, an invalid
	 * remote context has been detected and processing is aborted; otherwise, set
	 * context to the value of that member.
	 */
	private static JsonLdContext deferenceContext(JsonLdContext context) throws JsonLdException {
		InputStream in = null;
		
		try {
			URL url = new URL(context.getString());
			final String protocol = url.getProtocol();
			CloseableHttpResponse response = null;
			
			if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
				in = url.openStream();
			} else {
				final HttpUriRequest request = new HttpGet(url.toExternalForm());
				request.addHeader("Accept", "application/ld+json");
				response = HttpClients.createDefault().execute(request);
				final int status = response.getStatusLine().getStatusCode();
				if (status != 200 && status != 203) {
					throw new IOException("Can't retrieve " + url + ", status code: " + status);
				}
				in = response.getEntity().getContent();
			}
		} catch (IOException e) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED,
					context.getString());
			throw new JsonLdException(error);
		}

		return new JsonLdContext(in);
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getVocabulary() {
		return vocabulary;
	}

	public void setVocabulary(String vocabulary) {
		this.vocabulary = vocabulary;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
