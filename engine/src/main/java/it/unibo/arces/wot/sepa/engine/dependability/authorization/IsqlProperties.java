package it.unibo.arces.wot.sepa.engine.dependability.authorization;

public class IsqlProperties {
	private final String isqlPath;
	private final String isqlHost;
	private final String isqlUser;
	private final String isqlPass;
	
	public IsqlProperties(String isqlPath,String isqlHost,String isqlUser,String isqlPass) {
		this.isqlHost = isqlHost;
		this.isqlPass = isqlPass;
		this.isqlPath = isqlPath;
		this.isqlUser = isqlUser;
	}

	public String getIsqlPath() {
		return isqlPath;
	}

	public String getIsqlHost() {
		return isqlHost;
	}

	public String getIsqlUser() {
		return isqlUser;
	}

	public String getIsqlPass() {
		return isqlPass;
	}
}
