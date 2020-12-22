package it.unibo.arces.wot.sepa.engine.processing.updateprocessing;

import java.io.IOException;
import java.util.ArrayList;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
public interface IAsk {
	
	public ArrayList<UpdateExtractedData> filter() throws SEPABindingsException ,SEPASecurityException, IOException, SEPASparqlParsingException ;
}
