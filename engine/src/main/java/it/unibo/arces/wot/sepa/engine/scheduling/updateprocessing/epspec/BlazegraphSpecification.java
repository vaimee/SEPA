package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec;

import java.util.ArrayList;

public class BlazegraphSpecification implements IEndPointSpecification {

	public boolean asksAsSelectExistListCompare(String value) {
		return value.compareTo("true")==0;
	}
	
	public String s() {
		// TODO Auto-generated method stub
		return "subject";
	}

	public String p() {
		// TODO Auto-generated method stub
		return "predicate";
	}

	public String o() {
		// TODO Auto-generated method stub
		return "object";
	}

	public String g() {
		// TODO Auto-generated method stub
		return "g";
	}
	
	public ArrayList<String> vars() {
		ArrayList<String> vars = new ArrayList<String>();
		vars.add(s());
		vars.add(p());
		vars.add(o());
		return vars;
	}

}
