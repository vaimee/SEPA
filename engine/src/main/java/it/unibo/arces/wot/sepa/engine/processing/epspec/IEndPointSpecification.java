package it.unibo.arces.wot.sepa.engine.processing.epspec;

import java.util.ArrayList;

import it.unibo.arces.wot.sepa.engine.processing.ARQuadsAlgorithm;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory.EndPointSpec;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.IAsk;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.UpdateExtractedData;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

public interface IEndPointSpecification {

	public boolean asksAsSelectExistListCompare(String value);
	
	public String s();	
	public String p();
	public String o();
	public String g();
	public ArrayList<String> vars();
	public IAsk getAsk(ArrayList<UpdateExtractedData> ueds,InternalUpdateRequest req, ARQuadsAlgorithm algorithm);
	public EndPointSpec getEndPointName();
	
}
