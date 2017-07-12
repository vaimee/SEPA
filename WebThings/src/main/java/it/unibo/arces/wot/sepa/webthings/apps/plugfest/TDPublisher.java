package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;
//import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.framework.ThingDescription;

public class TDPublisher {
	JsonObject td;

	protected static final Logger logger = LogManager.getLogger("TDPublisher");

	public TDPublisher(String tdFile) throws IOException {

		FileReader in = null;

		in = new FileReader(tdFile);

		if (in != null) {
			td = new JsonParser().parse(in).getAsJsonObject();

			for (Entry<String, JsonElement> thing : td.entrySet()) {
				try {
					String thingURI = thing.getKey();
					String thingName = thing.getValue().getAsJsonObject().get("name").getAsString();

					//String str = UUID.randomUUID().toString();
					ThingDescription thingDescription = new ThingDescription("<http://wot.arces.unibo.it/plugFestThing"+ thingURI+">", thingName);

					String thingBase = null;
					if (thing.getValue().getAsJsonObject().get("base") != null) {
						thingBase = thing.getValue().getAsJsonObject().get("base").getAsString();
					}

					JsonArray interaction = thing.getValue().getAsJsonObject().get("interaction").getAsJsonArray();

					for(JsonElement actionProperty : interaction) {
						String name = actionProperty.getAsJsonObject().get("name").getAsString();
						
						JsonArray link = actionProperty.getAsJsonObject().get("link").getAsJsonArray();
						String href = link.get(0).getAsJsonObject().get("href").getAsString();
						if (thingBase != null) href = thingBase+href;
						JsonArray type = actionProperty.getAsJsonObject().get("@type").getAsJsonArray();
						switch (type.get(0).getAsString()) {
						case "Action":
							thingDescription.addAction("<http://wot.arces.unibo.it/plugFestThing/Action/"+ name+">", href, "wot:http");
							break;
						case "Property":
							thingDescription.addProperty("<http://wot.arces.unibo.it/plugFestThing/Property/"+ name+">", name, "xsd:string", "-1", "true", href);
							break;
						}
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		if (in != null)
			in.close();
	}
	
	public static void main(String[] args) {
		try {
			new TDPublisher("plugfest-td.json");
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
