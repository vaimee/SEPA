package it.unibo.arces.wot.sepa.engine.processing.epspec;

import java.util.ArrayList;

public interface IEndPointSpecification {

	public boolean asksAsSelectExistListCompare(String value);
	
	public String s();	
	public String p();
	public String o();
	public String g();
	public ArrayList<String> vars();
}
