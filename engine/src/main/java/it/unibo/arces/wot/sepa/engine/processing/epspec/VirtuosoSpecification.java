package it.unibo.arces.wot.sepa.engine.processing.epspec;

import java.util.ArrayList;

import it.unibo.arces.wot.sepa.engine.processing.ARQuadsAlgorithm;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory.EndPointSpec;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.AsksAsSelectGraphAsVar;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.IAsk;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.UpdateExtractedData;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

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
	

	public EndPointSpec getEndPointName() {
		return EndPointSpec.VIRTUOSO;
	}

	@Override
	public IAsk getAsk(ArrayList<UpdateExtractedData> ueds, InternalUpdateRequest req, ARQuadsAlgorithm algorithm){		
		return new AsksAsSelectGraphAsVar(ueds, req, algorithm);
	}


}
