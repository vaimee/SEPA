package com.vaimee.sepa.api.commons.properties;

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

	
	private String path = "/sparql";
	private QueryHTTPMethod method = QueryHTTPMethod.URL_ENCODED_POST;
	private QueryResultsFormat format = QueryResultsFormat.JSON;
	
	public void merge(QueryProperties query) {
		if (query.getPath() != null) this.setPath(query.getPath());
		if (query.getMethod() != null) this.setMethod(query.getMethod());
		if (query.getFormat() != null) this.setFormat(query.getFormat());
	}

	public QueryHTTPMethod getMethod() {
		return method;
	}

	public void setMethod(QueryHTTPMethod method) {
		this.method = method;
	}

	public QueryResultsFormat getFormat() {
		return format;
	}

	public void setFormat(QueryResultsFormat format) {
		this.format = format;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
