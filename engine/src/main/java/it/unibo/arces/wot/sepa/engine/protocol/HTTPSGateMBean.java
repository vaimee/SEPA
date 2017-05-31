package it.unibo.arces.wot.sepa.engine.protocol;

public interface HTTPSGateMBean {
	long getRegistrationTransactions();
	long getRequestTokenTransactions();
	long getSecureSPARQLTransactions();
}
