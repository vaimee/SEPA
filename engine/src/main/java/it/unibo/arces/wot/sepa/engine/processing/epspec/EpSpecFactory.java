package it.unibo.arces.wot.sepa.engine.processing.epspec;

import java.io.FileReader;

import com.google.gson.JsonParser;

public class EpSpecFactory {

	public enum EndPointSpec {
		VIRTUOSO("VIRTUOSO"),
		BLAZEGRAPH("BLAZEGRAPH");		
		private final String eps;
		 
		EndPointSpec(final String eps) {
		        this.eps = eps;
	    }
	  
	    @Override
	    public String toString() {
	        return eps;
	    }
		    
	}
	

	
	
	private static IEndPointSpecification instance = new VirtuosoSpecification();
	
	public static IEndPointSpecification getInstance() {
			return instance;
	}
	
	public static void setInstance(EndPointSpec eps){
			if(eps==EndPointSpec.BLAZEGRAPH) {
				instance = new BlazegraphSpecification();
			}else{// default--> (eps==EndPointSpec.VIRTUOSO) {
				instance = new VirtuosoSpecification();
			}
	}
	
	public static void setInstance(String eps){
		String fixed = eps.trim().toUpperCase().replace("\"", "");
		if(fixed.compareTo(EndPointSpec.BLAZEGRAPH.toString())==0) {
			instance = new BlazegraphSpecification();
		}else if(fixed.compareTo(EndPointSpec.VIRTUOSO.toString())==0){
			instance = new VirtuosoSpecification();
		}else {
			//default + warning
			instance = new VirtuosoSpecification();
			System.out.println("Warning: end point name not found for "+ eps+ ", EpSpecification setted as defualt (VIRTUOSO).");
		}
	}
	
	public static void setInstanceFromFile(String endpointJpar)  {
		String name = "VIRTUOSO";
		try {
			FileReader in = new FileReader(endpointJpar);
			name= new JsonParser().parse(in).getAsJsonObject().get("endpointname").getAsString();
		}
//		catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Warning: impossible to read end point name from "+endpointJpar+", EpSpecification is setted as defualt (VIRTUOSO).");
		}	
		setInstance(name);
	}
}
