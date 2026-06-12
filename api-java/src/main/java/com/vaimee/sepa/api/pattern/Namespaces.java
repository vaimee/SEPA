package com.vaimee.sepa.api.pattern;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.logging.Logging;

import org.apache.commons.text.StringEscapeUtils;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.modify.request.*;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Namespaces {
    private final PrefixMappingMem prefixes;
    private String prologue;
    private static final Set<String> numbersOrBoolean = new HashSet<>();
    public Namespaces() {
        this.prefixes = new PrefixMappingMem();

        // Numbers or boolean
        numbersOrBoolean.add("xsd:integer");
        numbersOrBoolean.add("xsd:decimal");
        numbersOrBoolean.add("xsd:double");
        numbersOrBoolean.add("xsd:boolean");

        numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#integer");
        numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#decimal");
        numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#double");
        numbersOrBoolean.add("http://www.w3.org/2001/XMLSchema#boolean");
    }

    public void buildSPARQLPrefixes(HashMap<String, String> namespaces) {
        // prefixes = "";
        if (namespaces == null)
            return;
        for (String prefix : namespaces.keySet()) {
            prefixes.setNsPrefix(prefix, namespaces.get(prefix));
            // prefixes += "PREFIX " + prefix + ":<" + namespaces.get(prefix) + "> ";
        }
        Logging.trace("Prefixes map: " + prefixes.toString());
        prologue = "";
        StringBuilder sb = new StringBuilder();
        for (String pre : prefixes.getNsPrefixMap().keySet()) {
            sb.append("PREFIX ").append(pre).append(":<").append(prefixes.getNsPrefixURI(pre)).append("> ");
        }
        prologue = sb.toString();
        Logging.trace("Prefixes SPARQL: " + prologue);

    }

    public String addPrefixesAndReplaceMultipleBindings(String sparql, ArrayList<Bindings> bindings)
            throws SEPABindingsException {
        return prologue + replaceMultipleBindings(sparql, bindings);
    }

    public String addPrefixesAndReplaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
        /*
         * Direct Known Subclasses:
         *
         * UpdateBinaryOp, UpdateCreate, UpdateData, UpdateDeleteWhere, UpdateDropClear,
         * UpdateLoad, UpdateWithUsing
         *
         */
        StringBuilder sb = new StringBuilder();

        try {
            UpdateRequest request = UpdateFactory.create();
            request.setPrefixMapping(prefixes);
            UpdateFactory.parse(request, replaceBindings(sparql, bindings));

            for (Update upd : request.getOperations()) {
                IndentedWriter writer = new IndentedLineBuffer();
                UpdateWriterVisitor visitor = new UpdateWriterVisitor(writer, new SerializationContext());
                if (!sb.toString().isEmpty()) sb.append(";");
                else sb.append(prologue);

                if (upd instanceof UpdateDataDelete)
                    visitor.visit((UpdateDataDelete) upd);
                else if (upd instanceof UpdateDataInsert)
                    visitor.visit((UpdateDataInsert) upd);
                else if (upd instanceof UpdateDeleteWhere)
                    visitor.visit((UpdateDeleteWhere) upd);

                else if (upd instanceof UpdateDrop)
                    visitor.visit((UpdateDrop) upd);
                else if (upd instanceof UpdateMove)
                    visitor.visit((UpdateMove) upd);
                else if (upd instanceof UpdateClear)
                    visitor.visit((UpdateClear) upd);
                else if (upd instanceof UpdateLoad)
                    visitor.visit((UpdateLoad) upd);

                else if (upd instanceof UpdateAdd)
                    visitor.visit((UpdateAdd) upd);
                else if (upd instanceof UpdateCopy)
                    visitor.visit((UpdateCopy) upd);
                else if (upd instanceof UpdateCreate)
                    visitor.visit((UpdateCreate) upd);
                else if (upd instanceof UpdateModify)
                    visitor.visit((UpdateModify) upd);

                sb.append(writer);
            }

        } catch (QueryParseException ex) {
            sb.append(prologue).append(replaceBindings(sparql, bindings));
        }

        return sb.toString();
    }

    private static String replaceBindings(String sparql, Bindings bindings) throws SEPABindingsException {
        if (bindings == null || sparql == null)
            return sparql;

        String replacedSparql = String.format("%s", sparql);

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

                if (datatype == null) {
                    if (lang != null)
                        value += "@" + bindings.getLanguage(var);
                    else {
                        value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
                    }
                } else if (!numbersOrBoolean.contains(datatype)) {
                    // Check if datatype is a qname or not
                    URI uri = null;
                    try {
                        uri = new URI(datatype);
                    } catch (URISyntaxException e) {
                        Logging.error(e.getMessage());
                    }

                    if (uri != null) {
                        if (uri.getSchemeSpecificPart().startsWith("/"))
                            datatype = "<" + datatype + ">";
                    }

                    value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
                    value += "^^" + datatype;
                }
            } else if (bindings.isURI(var)) {
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
                    Logging.error(e.getMessage());
                }

                if (uri != null) {
                    if (uri.getSchemeSpecificPart().startsWith("/") || uri.getScheme().equals("urn"))
                        value = "<" + value + ">";
                }
            } else {
                // A blank node
                Logging.trace("Blank node: " + value);

                // Not a BLANK_NODE_LABEL
                // [142] BLANK_NODE_LABEL ::= '_:' ( PN_CHARS_U | [0-9] ) ((PN_CHARS|'.')*
                // PN_CHARS)?
                if (!value.startsWith("_:"))
                    value = "<" + value + ">";
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
                    if (index + var.length() + 1 <= replacedSparql.length() - 1) {
                        int unicode = replacedSparql.codePointAt(index + var.length() + 1);
                        if (!isValidVarChar(unicode)) {
                            replacedSparql = replacedSparql.substring(0, index) + value
                                    + replacedSparql.substring(index + var.length() + 1);
                        }
                    }
                    // END OF STRING
                    else {
                        replacedSparql = replacedSparql.substring(0, index) + value;
                    }

                } else
                    start = index;
            }
        }

        return replacedSparql;
    }

    private static String replaceMultipleBindings(String sparql, ArrayList<Bindings> multipleBindings)
            throws SEPABindingsException {
        Logging.Timestamp start = new Logging.Timestamp();
        if (multipleBindings == null || sparql == null)
            return sparql;

        if (multipleBindings.isEmpty())
            return sparql;

        ArrayList<String> vars = new ArrayList<>(multipleBindings.get(0).getVariables());

        ArrayList<ArrayList<String>> allValues = new ArrayList<>();

        for (Bindings bindings : multipleBindings) {
            ArrayList<String> values = new ArrayList<>();
            for (String var : vars) {
                String value = bindings.getValue(var);
                if (value == null) {
                    // https://www.w3.org/TR/sparql11-query/#inline-data
                    // If a variable has no value for a particular solution in the VALUES clause,
                    // the keyword UNDEF is used instead of an RDF term.
                    values.add("UNDEF");
                    continue;
                }

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

                    if (datatype == null) {
                        if (lang != null)
                            value += "@" + bindings.getLanguage(var);
                        else {
                            value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
                        }
                    } else if (!numbersOrBoolean.contains(datatype)) {
                        // Check if datatype is a qname or not
                        URI uri = null;
                        try {
                            uri = new URI(datatype);
                        } catch (URISyntaxException e) {
                            Logging.error(e.getMessage());
                        }

                        if (uri != null) {
                            if (uri.getSchemeSpecificPart().startsWith("/"))
                                datatype = "<" + datatype + ">";
                        }

                        value = "'''" + StringEscapeUtils.escapeJava(value) + "'''";
                        value += "^^" + datatype;
                    }
                } else if (bindings.isURI(var)) {
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
                        Logging.error(e.getMessage());
                    }

                    if (uri != null) {
                        if (uri.getSchemeSpecificPart().startsWith("/") || uri.getScheme().equals("urn"))
                            value = "<" + value + ">";
                    }
                } else {
                    // A blank node
                    Logging.trace("Blank node: " + value);

                    // Not a BLANK_NODE_LABEL
                    // [142] BLANK_NODE_LABEL ::= '_:' ( PN_CHARS_U | [0-9] ) ((PN_CHARS|'.')*
                    // PN_CHARS)?
                    if (!value.startsWith("_:"))
                        value = "<" + value + ">";
                }

                values.add(value);
            }

            allValues.add(values);
        }

        String replacedSparql = String.format("%s", sparql);

        // VALUES (?book ?title)
        // { (UNDEF "SPARQL Tutorial")
        // (:book2 UNDEF)
        // }
        StringBuilder VARS = new StringBuilder();
        VARS.append("(");
        for (String var : vars) {
            VARS.append("?").append(var).append(" ");
        }
        VARS.append(")");
        StringBuilder VALUES = new StringBuilder();
        VALUES.append("{");
        for (ArrayList<String> values : allValues) {
            VALUES.append("(");
            for (String value : values) {
                VALUES.append(value).append(" ");
            }
            VALUES.append(")");
        }
        VALUES.append("}");

        int end = replacedSparql.lastIndexOf("}");

        String ret = replacedSparql.substring(0, end) + " VALUES " + VARS + VALUES + "}";
        Logging.Timestamp stop = new Logging.Timestamp();
        Logging.logTiming("replaceMultipleBindings", start, stop);
        return ret;
    }

    private static boolean isValidVarChar(int c) {
        return ((c == '_') || (c == 0x00B7) || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040)
                || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9')
                || (0x00C0 <= c && c <= 0x00D6) || (0x00D8 <= c && c <= 0x00F6) || (0x00F8 <= c && c <= 0x02FF)
                || (0x0370 <= c && c <= 0x037D) || (0x037F <= c && c <= 0x1FFF) || (0x200C <= c && c <= 0x200D)
                || (0x2070 <= c && c <= 0x218F) || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
                || (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD) || (0x10000 <= c && c <= 0xEFFFF));
    }

    public String getPrologue() {
        return prologue;
    }
}
