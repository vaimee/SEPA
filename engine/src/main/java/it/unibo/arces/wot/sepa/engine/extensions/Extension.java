package it.unibo.arces.wot.sepa.engine.extensions;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.engine.bean.HTTPHandlerBeans;
import com.vaimee.sepa.engine.bean.SEPABeans;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.gates.http.HttpUtilities;
import com.vaimee.sepa.engine.protocol.sparql11.SPARQL11HandlerMBean;
import com.vaimee.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import com.vaimee.sepa.logging.Logging;
import org.apache.http.*;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

public abstract class Extension implements HttpAsyncRequestHandler<HttpRequest>, SPARQL11HandlerMBean {

    protected HTTPHandlerBeans jmx = new HTTPHandlerBeans();

    public Extension() throws IllegalArgumentException {
        // JMX
        SEPABeans.registerMBean("SEPA:type=" + this.getClass().getName(), this);
    }

    public static Extension build(String classPath) throws SEPAPropertiesException {
        Class cls = null;
        try {
            cls = Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Logging.logger.error(e.getMessage());
            throw new SEPAPropertiesException(e);
        }

        @SuppressWarnings("rawtypes")
        Constructor cts = null;
        try {
            cts = cls.getConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Logging.logger.error(e.getMessage());
            throw new SEPAPropertiesException(e);
        }
        Object ret = null;
        try {
            ret = cts.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Logging.logger.error(e.getMessage());
            throw new SEPAPropertiesException(e);
        }
        return (Extension) ret;
    }

    protected ClientAuthorization authorize(HttpRequest request) throws SEPASecurityException {
        return new ClientAuthorization();
    }

    protected boolean corsHandling(HttpAsyncExchange exchange) {
        if (!Dependability.processCORSRequest(exchange)) {
            Logging.logger.log(Logging.getLevel("http"),"CORS origin not allowed");
            jmx.corsFailed();
            HttpUtilities.sendFailureResponse(exchange,
                    new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "cors_error", "CORS origin not allowed"));
            return false;
        }

        if (Dependability.isPreFlightRequest(exchange)) {
            Logging.logger.log(Logging.getLevel("http"),"Preflight request");
            HttpUtilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
            return false;
        }

        return true;
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
            throws HttpException, IOException {
        Logging.logger.log(Logging.getLevel("http"), "@processRequest " + request + " " + context);
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
            throws HttpException, IOException {
        Logging.logger.log(Logging.getLevel("http"), "@handle " + request + " " + context);
        // CORS
        if (!corsHandling(httpExchange))
            return;

        // Authorize
        ClientAuthorization oauth = null;
        try {
            oauth = authorize(httpExchange.getRequest());
        } catch (SEPASecurityException e1) {
            HttpUtilities.sendFailureResponse(httpExchange,
                    new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "oauth_exception", e1.getMessage()));
            jmx.authorizingFailed();
            return;
        }
        if (!oauth.isAuthorized()) {
            Logging.logger.log(Logging.getLevel("oauth"),"*** NOT AUTHORIZED *** " + oauth.getDescription());
            HttpUtilities.sendFailureResponse(httpExchange,
                    new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, oauth.getError(), oauth.getDescription()));
            jmx.authorizingFailed();
            return;
        }

        String body = null;
        if(httpExchange.getRequest().getRequestLine().getMethod().toUpperCase().equals("POST")) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) httpExchange.getRequest()).getEntity();
            try {
                body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
            } catch (ParseException | IOException e) {
                throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST,e.getMessage());
            }
        }

        HttpResponse response = httpExchange.getResponse();
        handleInternal(request, response, context,body);
        httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
    }

    public abstract void handleInternal(HttpRequest request, HttpResponse response, HttpContext context,String body);

    @Override
    public long getRequests() {
        return jmx.getRequests();
    }

    @Override
    public void reset() {
        jmx.reset();
    }

    @Override
    public float getHandlingTime_ms() {
        return jmx.getHandlingTime();
    }

    @Override
    public float getHandlingMinTime_ms() {
        return jmx.getHandlingMinTime();
    }

    @Override
    public float getHandlingAvgTime_ms() {
        return jmx.getHandlingAvgTime();
    }

    @Override
    public float getHandlingMaxTime_ms() {
        return jmx.getHandlingMaxTime_ms();
    }

    @Override
    public long getErrors_Timeout() {
        return jmx.getErrors_Timeout();
    }

    @Override
    public long getErrors_CORSFailed() {
        return jmx.getErrors_CORSFailed();
    }

    @Override
    public long getErrors_ParsingFailed() {
        return jmx.getErrors_ParsingFailed();
    }
}
