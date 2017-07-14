package it.unibo.arces.wot.framework.elements;

/**
 * From Web Things TD:
 * 
 *  {
      "@type": ["Action","actuator:on", "building:DrainLiquidAction"],
      "name": "pumpOn",
      "links": [{
        "href" : "coap://w3cwot.sytes.net:5689/on",
        "mediaType": "application/json"
        }]
    }
    
    {
      "@type": ["Property", "building:OverflowStatus"],
      "name": "overflowStatus",
      "outputData": {"valueType": { "type": "boolean" }},
      "writable": false,
      "links": [{
        "href" : "coap://w3cwot.sytes.net:5689/overflowStatus",
        "mediaType": "application/json"
        }
        
 * @author luca
 *
 */
public class Action {
	
	private String name;
	private String href;
	private String mediaType;

	public Action(String thingURI,String name,String href) {
		this.name = name;
		this.href = href;
	}
}
