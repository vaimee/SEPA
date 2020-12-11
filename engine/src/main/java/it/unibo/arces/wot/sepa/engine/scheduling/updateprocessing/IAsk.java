package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing;

import java.util.ArrayList;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
public interface IAsk {
	
	public ArrayList<UpdateExtractedData> filter() throws SEPABindingsException;
}
