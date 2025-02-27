package com.vaimee.sepa.commons.properties;

public class UpdateProperties {

	/**
	 * The Enum HTTPMethod (GET,POST,URL_ENCODED_POST).
	 */
	public enum UpdateHTTPMethod {
		/** The post. */
		POST("application/sparql-update"),
		/** The url encoded post. */
		URL_ENCODED_POST("application/x-www-form-urlencoded");
		
		private final String label;
		
		private UpdateHTTPMethod(String value) {
			label = value;
		}

		public String getUpdateContentTypeHeader() {
			return label;
    	}
	};

	/**
	 * The Enum UpdateResultsFormat (JSON,HTML).
	 */
	public enum UpdateResultsFormat {
		/** The html. */
		HTML("application/html"),
		/** The json. */
		JSON("application/json");
		
		private final String label;
		
		private UpdateResultsFormat(String value) {
			label = value;
		}

		public String getUpdateAcceptHeader() {
			return label;
    	}
	};
	
	private String path = "/sparql";
	private UpdateHTTPMethod method = UpdateHTTPMethod.URL_ENCODED_POST;
	private UpdateResultsFormat format = UpdateResultsFormat.JSON;
	
	public void merge(UpdateProperties update) {
		if (update.getPath() != null) this.setPath(update.getPath());
		if (update.getMethod() != null) this.setMethod(update.getMethod());
		if (update.getFormat() != null) this.setFormat(update.getFormat());
		
	}

	public UpdateHTTPMethod getMethod() {
		return method;
	}

	public void setMethod(UpdateHTTPMethod method) {
		this.method = method;
	}

	public UpdateResultsFormat getFormat() {
		return format;
	}

	public void setFormat(UpdateResultsFormat format) {
		this.format = format;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
