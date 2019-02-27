package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * {
"@type" : "Vehicle",
  "@context" : {
    "accuracy" : "http://missing/accuracy",
    "providedBy" : "http://missing/providedBy",
    "closeTo" : "http://missing/closeTo",
    "object" : "http://uri.etsi.org/ngsi-ld/hasObject",
    "Relationship" : "http://uri.etsi.org/ngsi-ld/Relationship",
    "GeoProperty" : "http://uri.etsi.org/ngsi-ld/GeoProperty",
    "value" : "http://uri.etsi.org/ngsi-ld/hasValue",
    "Point" : "https://purl.org/geojson/vocab#Point",
    "coordinates" : "http://uri.etsi.org/ngsi-ld/coordinates",
    "Property" : "http://uri.etsi.org/ngsi-ld/Property",
    "Vehicle" : "http://uri.fiware.org/ns/datamodels/Vehicle",
    "location" : "http://uri.etsi.org/ngsi-ld/location",
    "speed" : "http://uri.fiware.org/ns/datamodels/speed"
  }
}
 * */

public class EntityJsonLdFrame {
	private final JsonObject frame;
	
	public EntityJsonLdFrame(String type,JsonObject context) {
		frame = new JsonObject();
		frame.add("@type", new JsonPrimitive(type));
		frame.add("@context", context);
	}
	
	public InputStream getFrame() {
		return new ByteArrayInputStream(frame.toString().getBytes(StandardCharsets.UTF_8));
	}
}
