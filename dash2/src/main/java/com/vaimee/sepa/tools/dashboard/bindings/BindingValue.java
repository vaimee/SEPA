package com.vaimee.sepa.tools.dashboard.bindings;

public class BindingValue implements Comparable<BindingValue> {
	private boolean added = true;
	private String value;
	private boolean literal = true;
	private String dataType = null;

	public BindingValue(String value, boolean literal, String dataType, boolean added) {
		this.value = value;
		this.added = added;
		this.literal = literal;
		this.dataType = dataType;
	}

	public boolean isAdded() {
		return added;

	}

	public String get() {
		return value;
	}

	public String getDataType() {
		return dataType;
	}

	public boolean isLiteral() {
		return literal;
	}

	@Override
	public int compareTo(BindingValue o) {
		return value.compareTo(o.get());
	}
}
