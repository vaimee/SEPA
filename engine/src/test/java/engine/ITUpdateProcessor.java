package engine;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.engine.processing.UpdateProcessor;
import it.unibo.arces.wot.sepa.engine.processing.UpdateResponseWithAR;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ITUpdateProcessor {

    private UpdateProcessor updateProcessor;

    @Before
    public void init() throws SEPAPropertiesException, SEPAProtocolException {
        final SPARQL11SEProperties sparql11SEProperties = ConfigurationProvider.GetTestEnvConfiguration();
        updateProcessor = new UpdateProcessor(sparql11SEProperties, null);
    }
    @Test
    public void testInsertAddRemoved(){
        UpdateRequest updateRequest = new UpdateRequest("INSERT{<test://it/update> <test://it/update/pre> <test://it/update/obj>}Where{}");
        updateRequest.setTimeout(5000);
        Response process = updateProcessor.process(updateRequest);

        assertTrue("Not a UpdateResponse",process instanceof UpdateResponseWithAR);
        UpdateResponseWithAR uAR = (UpdateResponseWithAR) process;

        assertTrue("Added is empty",!uAR.getAdded().isEmpty());
        assertTrue("Removed is not empty",uAR.getRemoved().isEmpty());

        String sub = uAR.getAdded().getBindings().get(0).getValue("subject");
        String pred = uAR.getAdded().getBindings().get(0).getValue("predicate");
        String obj = uAR.getAdded().getBindings().get(0).getValue("object");

        assertEquals("test://it/update",sub);
        assertEquals("test://it/update/pre",pred);
        assertEquals("test://it/update/obj",obj);
    }

    @Test
    public void testDeleteAddRemoved(){
        UpdateRequest updateRequest = new UpdateRequest("DELETE{<test://it/update> <test://it/update/pre> <test://it/update/obj>}Where{}");
        Response process = updateProcessor.process(updateRequest);
        updateRequest.setTimeout(5000);

        assertTrue("Not a UpdateResponse",process instanceof UpdateResponseWithAR);
        UpdateResponseWithAR uAR = (UpdateResponseWithAR) process;

        assertTrue("Removed is empty",!uAR.getRemoved().isEmpty());
        assertTrue("Added is not empty",uAR.getAdded().isEmpty());

        String sub = uAR.getRemoved().getBindings().get(0).getValue("subject");
        String pred = uAR.getRemoved().getBindings().get(0).getValue("predicate");
        String obj = uAR.getRemoved().getBindings().get(0).getValue("object");

        assertEquals("test://it/update",sub);
        assertEquals("test://it/update/pre",pred);
        assertEquals("test://it/update/obj",obj);
    }
}
