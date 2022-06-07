/* This class belongs to the JMX classes used for the remote monitoring of the engine
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.bean;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Date;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

public class EngineBeans {
	private static Date startDate = new Date();

	private static EngineProperties properties;

	private static String version;

	public static void setVersion(String v) {
		version = v;
	}

	public static String getVersion() {
		return version;
	}

	public static void setEngineProperties(EngineProperties prop) {
		properties = prop;
	}

	public static String getQueryPath() {
		return properties.getQueryPath();
	}

	public static String getUpdatePath() {
		return properties.getUpdatePath();
	}

	public static String getSubscribePath() {
		return properties.getSubscribePath();
	}

	public static String getSecurePath() {
		return properties.getSecurePath();
	}

	public static String getRegisterPath() {
		return properties.getRegisterPath();
	}

	public static String getTokenRequestPath() {
		return properties.getTokenRequestPath();
	}

	public static int getHttpPort() {
		return properties.getHttpPort();
	}

	public static int getHttpsPort() {
		return properties.getHttpsPort();
	}

	public static int getWsPort() {
		return properties.getWsPort();
	}

	public static int getWssPort() {
		return properties.getWssPort();
	}

	public static boolean getSecure() {
		return properties.isSecure();
	}

	public static String getUpTime() {
		return startDate.toString() + " " + Duration.between(startDate.toInstant(), new Date().toInstant()).toString();
	}

	public static void resetAll() {
		QueryProcessorBeans.reset();
		UpdateProcessorBeans.reset();
		SchedulerBeans.reset();
		SPUManagerBeans.reset();		
		GateBeans.reset();
	}

	public static String getHost() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
	
	public static String getQueryURL() {
		String port = "";
		if (getHttpPort() != -1) port = ":"+getHttpPort();
		return "http://"+getHost()+port+getQueryPath();
	}

	public static String getUpdateURL() {
		String port = "";
		if (getHttpPort() != -1) port = ":"+getHttpPort();
		return "http://"+getHost()+port+getUpdatePath();
	}

	public static String getSecureQueryURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getQueryPath();
	}

	public static String getSecureUpdateURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getUpdatePath();
	}

	public static String getRegistrationURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getRegisterPath();
	}

	public static String getTokenRequestURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getTokenRequestPath();
	}
        
        public static boolean  isAclEnabled() {
            return properties.isAclEnabled();
        }
        
        public static String getAclType() {
            return properties.getAclType();
        }
        public static String getAclMode() {
            return properties.getAclMode();
        }
        public static String getAclPath() {
            return properties.getAclPath();
        }
        
        
	public static String getAclQueryPath() {
		return properties.getAclQueryPath();
	}

	public static String getAclUpdatePath() {
		return properties.getAclUpdatePath();
	}
        
	public static String getAclQueryURL() {
		String port = "";
		if (getHttpPort() != -1) 
                    port = ":"+getHttpPort();
		return "http://"+getHost()+port+getAclQueryPath();
	}

	public static String getAclUpdateURL() {
		String port = "";
		if (getHttpPort() != -1) 
                    port = ":"+getHttpPort();
		return "http://"+getHost()+port+getAclUpdatePath();
	}
        
	public static String getSecureAclQueryURL() {
		String port = "";
		if (getHttpsPort() != -1) 
                    port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getAclQueryPath();
	}

	public static String getSecureAclUpdateURL() {
		String port = "";
		if (getHttpsPort() != -1) 
                    port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getAclUpdatePath();
	}
        
        
               
        public static String   getFirstDatasetMode() {
            return properties.getFirstDatasetMode();
        }
        public static String   getFirstDatasetPath() {
            return properties.getFirstDatasetPath();
        }
        public static String   getSecondDatasetMode() {
            return properties.getSecondDatasetMode();
        }
        public static String   getSecondDatasetPath() {
            return properties.getSecondDatasetPath();
        }        
}
