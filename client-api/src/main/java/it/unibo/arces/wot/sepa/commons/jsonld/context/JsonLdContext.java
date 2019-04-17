package it.unibo.arces.wot.sepa.commons.jsonld.context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdDictionary;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdError;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdErrorCode;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdException;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions.PROCESSING_MODE;

/**
 * The JsonLdContext type is used to refer to a value that may be 1) a
 * dictionary, 2) a string representing an IRI, 3) or an array of dictionaries
 * and strings.
 */
public class JsonLdContext {
	/**
	 * In the JSON serialization, an array structure is represented as square
	 * brackets surrounding zero or more values. Values are separated by commas. In
	 * the internal representation, an array is an ordered collection of zero or
	 * more values. While JSON-LD uses the same array representation as JSON, the
	 * collection is unordered by default. While order is preserved in regular JSON
	 * arrays, it is not in regular JSON-LD arrays unless specifically defined (see
	 * Sets and Lists in the JSON-LD Syntax specification [JSON-LD11]).
	 */
	private final ArrayList<JsonLdContext> array = new ArrayList<JsonLdContext>();
	private JsonLdDictionary dictionary;

	/**
	 * An Internationalized Resource Identifier as described in [RFC3987].
	 */
	private String string;

	/**
	 * 4.1.1 JSON-LD 1.1 Processing Mode This section is non-normative.New features
	 * defined in JSON-LD 1.1 are available when the processing mode is set to
	 * json-ld-1.1. This may be set using the @version member in a context set to
	 * the value 1.1 as a number, or through an API option. The processing mode
	 * defines how a JSON-LD document is processed. By default, all documents are
	 * assumed to be conformant with JSON-LD 1.0 [JSON-LD]. By defining a different
	 * version using the @version member in a context, or via explicit API option,
	 * other processing modes can be accessed. This specification defines extensions
	 * for the json-ld-1.1 processing mode.
	 */
	private final PROCESSING_MODE processingMode;

	/**
	 * Boolean values (@prefix)
	 */
	private Boolean bool;

	/**
	 * Number values (@version)
	 */
	private Number number;

	public JsonLdContext() {
		this.string = null;
		this.dictionary = null;
		this.bool = null;
		this.number = null;
		this.processingMode = PROCESSING_MODE.JSON_LD_1_0;
	}

	public JsonLdContext(String string) {
		this();
		this.string = string;
	}

	public JsonLdContext(boolean b) {
		this();
		this.bool = new Boolean(b);
	}

	public JsonLdContext(JsonLdDictionary dictionary) {
		this();
		this.dictionary = dictionary;
	}

	public JsonLdContext(ArrayList<JsonLdContext> array) {
		this();
		this.array.addAll(array);
	}

	public JsonLdContext(InputStream in) throws JsonLdException {
		this();

		InputStreamReader reader = new InputStreamReader(in);
		JsonElement parsed = null;
		try {
			parsed = new JsonParser().parse(reader);
		} catch (JsonParseException e) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, in.toString());
			throw new JsonLdException(error);
		}

		buildFromJsonElement(parsed);
	}

	public JsonLdContext(JsonElement in) throws JsonLdException {
		this();

		buildFromJsonElement(in);
	}

	private void buildFromJsonElement(JsonElement parsed) throws JsonLdException {
		if (parsed.isJsonPrimitive()) {
			try {
				this.string = parsed.getAsString();
			} catch (ClassCastException | IllegalStateException e) {
				try {
					this.number = parsed.getAsNumber();
				} catch (ClassCastException | IllegalStateException e1) {
					try {
						this.bool = parsed.getAsBoolean();
					} catch (ClassCastException | IllegalStateException e2) {
						JsonLdError error = new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, parsed.toString());
						throw new JsonLdException(error);
					}
				}
			}
		} else if (parsed.isJsonObject()) {
			this.dictionary = new JsonLdDictionary();
			for (Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
				this.dictionary.addMember(entry.getKey(), new JsonLdContext(entry.getValue()));
			}

		} else if (parsed.isJsonArray()) {
			for (JsonElement elem : parsed.getAsJsonArray()) {
				array.add(new JsonLdContext(elem));
			}
		}
	}

	public void add(String string) {
		array.add(new JsonLdContext(string));
	}

	public void add(JsonLdDictionary dictionary) {
		array.add(new JsonLdContext(dictionary));
	}

	public boolean isEmpty() {
		return array.size() == 0;
	}

	public boolean isNumber() {
		return number != null;
	}

	public Number getNumber() {
		return number;
	}

	public boolean isArray() {
		return array.size() > 1;
	}

	public Iterator<JsonLdContext> iterator() {
		return array.iterator();
	}

	public boolean isDictionary() {
		return dictionary != null;
	}

	public boolean isString() {
		return string != null;
	}

	public String getString() {
		return string;
	}

	public boolean isBoolean() {
		return bool != null;
	}

	public boolean getBoolean() {
		return bool;
	}

	@Override
	public int hashCode() {
		if (isString())
			return string.hashCode();
		return dictionary.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonLdContext))
			return false;
		JsonLdContext cmp = (JsonLdContext) obj;
		if (isString())
			return string.equals(cmp.getString());
		return dictionary.equals(cmp.getDictionary());
	}

	private JsonLdDictionary getDictionary() {
		return dictionary;
	}

	public JsonLdContext getMember(String term) throws JsonLdException {
		if (dictionary == null) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_SET_OR_LIST_OBJECT,
					"Context is not a dictionary");
			throw new JsonLdException(error);
		}
		return dictionary.getMember(term);
	}

	/**
	 * An absolute IRI is defined in [RFC3987] containing a scheme along with a path
	 * and optional query and fragment segments.
	 */
	public boolean isAbsoluteIri() throws JsonLdException {
		if (string == null) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TYPE_VALUE, "Context is not a string");
			throw new JsonLdException(error);
		}
		return isAbsoluteIri(string);
	}

	/**
	 * The base IRI is an absolute IRI established in the context, or is based on
	 * the JSON-LD document location. The base IRI is used to turn relative IRIs
	 * into absolute IRIs.
	 */
	public String resolve(String base) {
		return resolve(base, string);
	}

	public PROCESSING_MODE getProcessingMode() throws JsonLdException {
		return processingMode;
	}

	public boolean isEmptyString() throws JsonLdException {
		if (string == null) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_TYPE_VALUE, "Context is not a string");
			throw new JsonLdException(error);
		}
		return string.isEmpty();
	}

	/**
	 * A blank node identifier is a string that can be used as an identifier for a
	 * blank node within the scope of a JSON-LD document. Blank node identifiers
	 * begin with _:.
	 */
	public boolean isBlankNodeIdentifier() {
		if (string == null)
			return false;
		return string.startsWith("_:");
	}

	/**
	 * Get all the keys in the context which are not @base, @vocab, @language
	 * or @version
	 */
	public ArrayList<String> geyKeys() throws JsonLdException {
		if (dictionary == null) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_SET_OR_LIST_OBJECT,
					"Context is not a dictionary");
			throw new JsonLdException(error);
		}
		ArrayList<String> keys = new ArrayList<String>();
		for (String key : dictionary.getKeys()) {
			if (key.equals("@base") || key.equals("@vocab") || key.equals("@language") || key.equals("@version"))
				continue;
			keys.add(key);
		}
		return keys;
	}

	public boolean containsMember(String term) throws JsonLdException {
		if (dictionary == null) {
			JsonLdError error = new JsonLdError(JsonLdErrorCode.INVALID_SET_OR_LIST_OBJECT,
					"Context is not a dictionary");
			throw new JsonLdException(error);
		}
		return dictionary.getMember(term) != null;
	}

	public static boolean isKeyword(String term) {
		return "base".equals(term) || "@container".equals(term) || "@context".equals(term) || "@id".equals(term)
				|| "@index".equals(term) || "@language".equals(term) || "@list".equals(term) || "@nest".equals(term)
				|| "@none".equals(term) || "@prefix".equals(term) || "@reverse".equals(term) || "@set".equals(term)
				|| "@type".equals(term) || "@value".equals(term) || "@vocab".equals(term);
	}

	public static String resolve(String base, String value) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * An absolute IRI is defined in [RFC3987] containing a scheme along with a path and optional query and fragment segments.
	 * */
	public static boolean isAbsoluteIri(String value) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * A compact IRI is has the form of prefix:suffix and is used as a way of expressing an IRI 
	 * without needing to define separate term definitions for each IRI contained within a common vocabulary identified by prefix.
	 * */
	public static boolean isCompactIri(String term) {
		// TODO Auto-generated method stub
		return false;
	}

	public static boolean endsWithUriGenDelimChar(String iri) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * A blank node identifier is a string that can be used as an identifier for a blank node 
	 * within the scope of a JSON-LD document. Blank node identifiers begin with _:.
	 * */
	public static boolean isBlankNodeIdentifier(String mapping) {
		return mapping.startsWith("_:");
	}

	public static String getPrefixFromCompactIri(String term) {
		return term.split(":")[0];
	}

	public static String getSuffixFromCompactIri(String term) {
		return term.split(":")[1];
	}

	/**
	 * MUST be either
	 * 
	 * 1) @graph, @id, @index, @language, @list, @set, or @type. or 
	 * 2) an array containing exactly any one of those keywords, 
	 * 3) an array containing @graph and either @id or @index optionally including @set, or 
	 * 4) an array containing a combination of @set and any of @index, @id, @type, @language in any order
	 */
	public static boolean isValidContainer(JsonLdContext container) {
		// 1
		if (container.isString()) return isValidContainerElement(container.getString());
		else if (container.isArray()) {
			boolean valid = true;
			Iterator<JsonLdContext> it = container.iterator();
			ArrayList<String> elements = new ArrayList<String>();
			while(it.hasNext()) {
				JsonLdContext elem = it.next();
				if (!elem.isString()) return false;
				elements.add(elem.getString());
			}
			
			// 2
			for(String elem : elements) {
				if (!isValidContainerElement(elem)) {
					valid = false;
					break;
				}	
			}
			if (valid) return valid;
			
			// 3
			if (elements.contains("@graph") && (elements.contains("@id") || elements.contains("@index"))) {
				if (elements.size() == 2) return true;
				if (elements.size() == 3 && elements.contains("@set")) return true;
			}

			// 4
			if (!elements.contains("@set")) return false;
			for (String elem: elements) {
				if (!elem.equals("@index") && !elem.equals("@id") && !elem.equals("@type") || !elem.equals("@language")) return false;
			}
			
			return true;
			
		}

		return false;
	}
	
	private static boolean isValidContainerElement(String container) {
		return container.equals("@graph") || container.equals("@id")
				|| container.equals("@index") || container.equals("@language")
				|| container.equals("@list") || container.equals("@set") || container.equals("@type"); 	
	}

	public Set<String> getMembers() {
		if (dictionary == null)
			return null;
		return dictionary.getKeys();
	}

	public static ArrayList<JsonLdContext> copy(ArrayList<JsonLdContext> remote) {
		// TODO Auto-generated method stub
		return null;
	}
}
