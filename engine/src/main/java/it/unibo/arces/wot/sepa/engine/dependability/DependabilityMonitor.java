package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.HashMap;

import it.unibo.arces.wot.sepa.engine.bean.AuthorizationManagerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;

public class DependabilityMonitor implements DependabilityMonitorMBean {
	
	public DependabilityMonitor() {
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}
	
	@Override
	public long getTokenExpiringPeriod() {
		return AuthorizationManagerBeans.getTokenExpiringPeriod();
	}

	@Override
	public void setTokenExpiringPeriod(long period) {
		AuthorizationManagerBeans.setTokenExpiringPeriod(period);
	}

	@Override
	public void addAuthorizedIdentity(String id) {
		AuthorizationManagerBeans.getAuthorizedIdentities().put(id, true);
	}

	@Override
	public void removeAuthorizedIdentity(String id) {
		AuthorizationManagerBeans.getAuthorizedIdentities().remove(id);
	}

	@Override
	public HashMap<String, Boolean> getAuthorizedIdentities() {
		return AuthorizationManagerBeans.getAuthorizedIdentities();
	}

	@Override
	public String getIssuer() {
		return AuthorizationManagerBeans.getIssuer();
	}

	@Override
	public void setIssuer(String issuer) {
		AuthorizationManagerBeans.setIssuer(issuer);
	}

	@Override
	public String getHttpsAudience() {
		return AuthorizationManagerBeans.getHttpsAudience();
	}

	@Override
	public void setHttpsAudience(String audience) {
		AuthorizationManagerBeans.setHttpsAudience(audience);
	}

	@Override
	public String getWssAudience() {
		return AuthorizationManagerBeans.getWssAudience();
	}

	@Override
	public void setWssAudience(String audience) {
		AuthorizationManagerBeans.setWssAudience(audience);
	}

	@Override
	public String getSubject() {
		return AuthorizationManagerBeans.getSubject();
	}

	@Override
	public void setSubject(String sub) {
		AuthorizationManagerBeans.setSubject(sub);
	}

	@Override
	public long getNumberOfGates() {
		return SubscriptionManager.getNumberOfGates();
	}

	@Override
	public boolean getSecure() {
		return Dependability.isSecure();
	}
}
