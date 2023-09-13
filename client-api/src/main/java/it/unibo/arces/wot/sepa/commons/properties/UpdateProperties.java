package it.unibo.arces.wot.sepa.commons.properties;

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
	
	public String path = "/sparql";
	public UpdateHTTPMethod method = UpdateHTTPMethod.URL_ENCODED_POST;
	public UpdateResultsFormat format = UpdateResultsFormat.JSON;
	
	public void merge(UpdateProperties update) {
		if (update.path != null) this.path = update.path;
		if (update.method != null) this.method = update.method;
		if (update.format != null) this.format = update.format;
		
	}

}
