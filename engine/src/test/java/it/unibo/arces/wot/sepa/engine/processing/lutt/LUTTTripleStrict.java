package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.List;

public class LUTTTripleStrict extends LUTTTriple{


	public LUTTTripleStrict(LUTTTriple lutt) {
		super(lutt.str_subject,lutt.str_predicate,lutt.str_object);
	}


	@Override
	public boolean equals(Object obj) {
		
		if(obj.getClass().equals(LUTTTripleStrict.class)) {
			LUTTTripleStrict temp = (LUTTTripleStrict)obj;
			
			return temp.getObject().compareTo(this.str_object)==0
					&& temp.getPredicate().compareTo(this.str_predicate)==0
					&& temp.getSubject().compareTo(this.str_subject)==0;
			
		}	
		
		return false;

	}
	
	public boolean isIn(List<LUTTTriple> list) {
		for(int x=0;x<list.size();x++) {
			if((new LUTTTripleStrict(list.get(x))).equals(this)) {
				return true;
			}
		}
		return false;

	}
	





}
