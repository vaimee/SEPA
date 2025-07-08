package com.vaimee.sepa.engine.processing.endpoint;

import org.apache.http.HttpStatus;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.request.QueryRequest;
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.response.UpdateResponseWithAR;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.engine.dependability.acl.SEPAUserInfo;
import com.vaimee.sepa.engine.dependability.Dependability;

public class SjenarEndpoint implements SPARQLEndpoint {

	@Override
	public Response query(QueryRequest req,SEPAUserInfo usr) {
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
	public Response update(UpdateRequest req,SEPAUserInfo usr) {
		// TODO Auto-generated method stub

		Response ret = new UpdateResponseWithAR("QUI CI PIAZZI IL JSON CHE RAPPRESENTA A/R");
		ret = new ErrorResponse(0, null, null); // tutto male
		return ret;
	}
}
