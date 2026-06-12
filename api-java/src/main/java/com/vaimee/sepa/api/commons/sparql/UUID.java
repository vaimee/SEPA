package com.vaimee.sepa.api.commons.sparql;

public class UUID extends RDFTermURI{
	public UUID() {
		super("urn:uuid:"+java.util.UUID.randomUUID().toString());
	}
	
	@Override
	public boolean equals(Object t) {
		if (t == this)
			return true;
		if (!t.getClass().equals(UUID.class))
			return false;

		return this.getValue().equals(((UUID) t).getValue());
	}
}
