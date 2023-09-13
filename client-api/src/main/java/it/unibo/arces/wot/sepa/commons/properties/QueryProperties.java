package it.unibo.arces.wot.sepa.commons.properties;

public class QueryProperties {
	/**
	 * The Enum HTTPMethod (GET,POST,URL_ENCODED_POST).
	 */
	public enum QueryHTTPMethod {
		/** The get. */
		GET("application/sparql-query"),
		/** The post. */
		POST("application/sparql-query"),
		/** The url encoded post. */
		URL_ENCODED_POST("application/x-www-form-urlencoded");
		
		private final String label;
		
		private QueryHTTPMethod(String value) {
			label = value;
		}

		public String getQueryContentTypeHeader() {
			return label;
    	}
	};
	

	/**
	 * The Enum QueryResultsFormat (JSON,XML,CSV).
	 */
	public enum QueryResultsFormat {
		/** The json. */
		JSON("application/sparql-results+json"),
		/** The xml. */
		XML("application/sparql-results+xml"),
		/** The csv. */
		CSV("text/csv");

		private final String label;
		
		private QueryResultsFormat(String value) {
			label = value;
		}
		
		public String getQueryAcceptHeader() {
			return label;
		}
	};

	
	public String path = "/sparql";
	public QueryHTTPMethod method = QueryHTTPMethod.URL_ENCODED_POST;
	public QueryResultsFormat format = QueryResultsFormat.JSON;
	
	public void merge(QueryProperties query) {
		if (query.path != null) this.path = query.path;
		if (query.method != null) this.method = query.method;
		if (query.format != null) this.format = query.format;
	}
}
