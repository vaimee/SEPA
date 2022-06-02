package it.unibo.arces.wot.sepa.engine.processing.endpoint;

public class TempQuadForTest {

	private String graph;
	private String subject;
	private String predicate;
	private String object;
	
	
	
	public TempQuadForTest(String graph, String subject, String predicate, String object) {
		super();
		this.graph = graph;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}
	
	public String getGraph() {
		return graph;
	}
	public void setGraph(String graph) {
		this.graph = graph;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getPredicate() {
		return predicate;
	}
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
	public String getObject() {
		return object;
	}
	public void setObject(String object) {
		this.object = object;
	}
	
	@Override
	public String toString() {
		return 	"<"+this.graph+
				"><"+this.subject +
				"><"+this.predicate + 
				"><"+this.object+">";
	}
	
}