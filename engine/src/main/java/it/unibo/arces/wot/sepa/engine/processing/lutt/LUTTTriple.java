package it.unibo.arces.wot.sepa.engine.processing.lutt;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

public class LUTTTriple {
	//null value is as jolly *
	protected String str_subject;
	protected String str_object;
	protected String str_predicate;


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


	private boolean hitPassed(String a, String b) {
		if(!(a==null || b==null)) {
			if(a.compareTo(b)!=0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		
		if(obj.getClass().equals(LUTTTriple.class)) {
			LUTTTriple temp = (LUTTTriple)obj;
			
			return hitPassed(temp.getObject(),this.str_object) 
					&& hitPassed(temp.getPredicate(),this.str_predicate)
					&& hitPassed(temp.getSubject(),this.str_subject);
			
		}
		
		return false;

	}
	






}
