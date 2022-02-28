package it.unibo.arces.wot.sepa.engine.processing.lutt;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

public class LUTTTriple {
	//null value is as jolly *
	private String str_subject;
	private String str_object;
	private String str_predicate;


	public LUTTTriple(String str_subject, String str_predicate,String str_object) {
		this.str_subject = str_subject;
		this.str_object = str_object;
		this.str_predicate = str_predicate;
	}




	public String getSubject() {
		return str_subject;
	}

	public String getObject() {
		return str_object;
	}

	public String getPredicate() {
		return str_predicate;
	}




	@Override
	public boolean equals(Object obj) {
		if(obj.getClass().equals(LUTTTriple.class)) {
			LUTTTriple temp = (LUTTTriple)obj;
			if(temp.getObject()==null) {
				if(this.str_object!=null) {
					return false;
				}
			}else {
				if(this.str_object==null || temp.getObject().compareTo(this.str_object)!=0) {
					return false;
				}
			}
			if(temp.getPredicate()==null) {
				if(this.str_predicate!=null) {
					return false;
				}
			}else {
				if(this.str_predicate==null || temp.getPredicate().compareTo(this.str_predicate)!=0) {
					return false;
				}
			}
			if(temp.getSubject()==null) {
				if(this.str_subject!=null) {
					return false;
				}
			}else {
				if(this.str_subject==null || temp.getSubject().compareTo(this.str_subject)!=0) {
					return false;
				}
			}
			return true;
		}
		return false;

	}





}
