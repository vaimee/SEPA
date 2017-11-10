package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.util.HashSet;
import java.util.Set;

import it.unibo.arces.wot.framework.interaction.ActionPublisher;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.COLOR;

public class ActionManager {
	// Actions
	private static final String LCD_ACTION = "wot:LCDWriteAction";
	private static final String LED_COLOR_ACTION = "wot:ChangeColourAction";
	private static final String LED_FREQUENCY_ACTION = "wot:ChangeFrequencyAction";
	
	private ActionPublisher actionOnLCD;
	private ActionPublisher actionOnLEDColor;
	private ActionPublisher actionOnLEDFrequency;
	
	public ActionManager() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException  {
		actionOnLCD = new ActionPublisher(LCD_ACTION);
		actionOnLEDColor = new ActionPublisher(LED_COLOR_ACTION);
		actionOnLEDFrequency = new ActionPublisher(LED_FREQUENCY_ACTION);
	}
	
	public void setColors(Set<COLOR> colors) {
		int r = 0, g = 0, b = 0;
		for (COLOR c : colors) {
			if (c.equals(COLOR.RED))
				r = 1;
			else if (c.equals(COLOR.GREEN))
				g = 1;
			else if (c.equals(COLOR.BLUE))
				b = 1;
		}
		String json = String.format("{\"r\":%d,\"g\":%d,\"b\":%d}", r, g, b);
		actionOnLEDColor.post(json, "wot:ChangeRGBColorInputType");
	}
	
	public void setText(String message) {
		actionOnLCD.post(message, "xsd:string");
	}
	
	public void setBlinking(boolean on) {
		if (on) actionOnLEDFrequency.post("{\"frequency\":3}", "xsd:integer");
		else actionOnLEDFrequency.post("{\"frequency\":0}", "xsd:integer");
	}

	public void clearColors() {
		HashSet<COLOR> empty = new HashSet<COLOR>();
		setColors(empty);	
		//setBlinking(false);
	}
}
