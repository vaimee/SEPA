package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import org.apache.http.HttpStatus;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponseWithAR;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;

public class SjenarEndpoint implements SPARQLEndpoint {

	@Override
	public Response query(QueryRequest req) {
		String header = req.getAuthorizationHeader();
		if (!header.toLowerCase().startsWith("bearer"))
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "wrong or missing bearer token",
					"Received header: " + header);
		String jwt = header.substring(7); // o 6 da controllare ==> bearer fhjskahgjdsahgalshdgkjahsjkgsdaljkg
		try {
			ClientAuthorization auth = Dependability.validateToken(jwt);
			// dopo in qualche modo ti faccio arrivare l'utente
		} catch (SEPASecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ErrorResponse(0, null, null); // tutto male da definire bene cosa
		}

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response update(UpdateRequest req) {
		// TODO Auto-generated method stub

		Response ret = new UpdateResponseWithAR("QUI CI PIAZZI IL JSON CHE RAPPRESENTA A/R");
		ret = new ErrorResponse(0, null, null); // tutto male
		return ret;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
