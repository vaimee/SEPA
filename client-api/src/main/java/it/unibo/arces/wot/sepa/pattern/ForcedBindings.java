package it.unibo.arces.wot.sepa.pattern;

import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermBNode;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

public class ForcedBindings extends Bindings {
	/**
	 * Sets the binding value.
	 *
	 * @param variable the variable
	 * @param value the value
	 * @param datatype the datatype
	 */
	 public void setBindingValue(String variable,String value,String datatype) throws SEPABindingsException {
		if (variable == null || value == null) throw new SEPABindingsException("One or more arguments are null");
		try {
			solution.getAsJsonObject(variable).add("value", new JsonPrimitive(value));
			solution.getAsJsonObject(variable).add("datatype",new JsonPrimitive(datatype));
		}
		catch(Exception e) {
			throw new SEPABindingsException(String.format("Variable not found: %s",variable));
		}		
	}
	
	/**
	 * Sets the binding value.
	 *
	 * @param variable the variable
	 * @param value the RDFTerm
	 * @see RDFTerm
	 */
	 public void setBindingValue(String variable,RDFTerm value) throws SEPABindingsException  {
		if (variable == null || value == null) throw new SEPABindingsException("One or more arguments are null");
			
		try {
			if (isLiteral(variable)) {
				if(!value.getClass().equals(RDFTermLiteral.class)) throw new SEPABindingsException("Value of variable: "+variable+" must be a literal");
				if (((RDFTermLiteral) value).getDatatype() != null) solution.getAsJsonObject(variable).add("datatype", new JsonPrimitive(((RDFTermLiteral) value).getDatatype()));
			}
			if (isURI(variable) && !value.getClass().equals(RDFTermURI.class))throw new SEPABindingsException("Value of variable: "+variable+" must be an URI");
			if (isBNode(variable)  && !value.getClass().equals(RDFTermBNode.class)) throw new SEPABindingsException("Value of variable: "+variable+" must be a b-node");
			
			solution.getAsJsonObject(variable).add("value", new JsonPrimitive(value.getValue()));
		}
		catch(Exception e) {
			throw new SEPABindingsException(String.format("Variable not found or bad type: %s",variable));
		}		
	}
}
