package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec;

import java.util.ArrayList;

public class VirtuosoSpecification implements IEndPointSpecification {

	public boolean asksAsSelectExistListCompare(String value) {
		return value.compareTo("1")==0;
	}

	public String s() {
		// TODO Auto-generated method stub
		return "s";
	}

	public String p() {
		// TODO Auto-generated method stub
		return "p";
	}

	public String o() {
		// TODO Auto-generated method stub
		return "o";
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
