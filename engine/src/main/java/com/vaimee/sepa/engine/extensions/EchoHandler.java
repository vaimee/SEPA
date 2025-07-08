package com.vaimee.sepa.engine.extensions;

import com.vaimee.sepa.engine.gates.http.HttpUtilities;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;

public class EchoHandler extends Extension{
    @Override
    public void handleInternal(HttpRequest request, HttpResponse response, HttpContext context, String body) {
        response.setStatusCode(HttpStatus.SC_OK);
        NStringEntity entity = new NStringEntity(
                HttpUtilities.buildEchoResponse(request).toString(),
                ContentType.create("application/json", "UTF-8"));
        response.setEntity(entity);
    }
}
