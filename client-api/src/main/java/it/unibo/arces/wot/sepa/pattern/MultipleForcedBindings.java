package it.unibo.arces.wot.sepa.pattern;

import com.google.gson.JsonObject;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;

import java.util.ArrayList;
import java.util.List;

public class MultipleForcedBindings extends ForcedBindings {
    ArrayList<Bindings> multipleForcedBindings;

    public MultipleForcedBindings(JsonObject solution) {
        super(solution);
        multipleForcedBindings = new ArrayList<Bindings>();
    }

    public MultipleForcedBindings() {
        super();
        multipleForcedBindings = new ArrayList<Bindings>();
    }
    
    public MultipleForcedBindings(ArrayList<Bindings> bindings) {
        super();
        multipleForcedBindings = bindings;
    }

    public final void add(ArrayList<String> variables, ArrayList<ArrayList<RDFTerm>> values) throws SEPABindingsException {
        for(ArrayList<RDFTerm> value:values) {
            Bindings b = new Bindings();
            for(int i=0; i < variables.size(); i++) {
                b.addBinding(variables.get(i),value.get(i));
            }
            multipleForcedBindings.add(b);
        }
    }
    
    public final void add(List<Bindings> b) throws SEPABindingsException {
    	multipleForcedBindings.addAll(b);
 
    }

    public final ArrayList<Bindings> getBindings() {
        return multipleForcedBindings;
    }
}
