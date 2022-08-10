package eu.interopehrate.r2d.ehr.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.sr2dsm.SR2DSM;
import eu.interopehrate.sr2dsm.model.ResponseDetails;
import eu.interopehrate.sr2dsm.model.UserDetails;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: HAPI FHIR interceptor used to validate the incoming
 * requests sent by the R2D Access Server.
 */

@Interceptor
public class R2DRequestValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(R2DRequestValidator.class);
				
	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void doFilter(RequestDetails theRequestDetails) throws IOException {
		// LOGGER.info(String.format("Received the following R2D request: %s", theRequestDetails.getCompleteUrl()));
		// Checks mandatory header parameters		
		// #1 checks for R2DRequest id
		Optional<String> reqIdOpt = Optional.fromNullable(theRequestDetails.getHeader(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME));
		if (!reqIdOpt.isPresent()) {
			LOGGER.error("The request lacks the R2D request ID header parameter, thus cannot be processed.");
			throw new AuthenticationException("The request lacks the R2D request ID header parameter, thus cannot be processed.");		
		}
		
		// #2 checks for the eidas token
		Optional<String> authTokenOpt = Optional.fromNullable(theRequestDetails.getHeader(SecurityConstants.R2D_REQUEST_CITIZEN_PARAM_NAME));
		if (!authTokenOpt.isPresent()) {
			LOGGER.error("The request lacks the R2D citizen token header parameter, thus cannot be processed.");
			throw new AuthenticationException("The request lacks the R2D citizen token header parameter, thus cannot be processed.");		
		}
				
		// #3 validates the EIDAS token
		Citizen theCitizen = null;
		try {
			// LOGGER.debug("Verifying EIDAS token...");
			String oAuthToken = authTokenOpt.get().substring(SecurityConstants.OAUTH_PREFIX.length()).trim();
			ResponseDetails tokenDetails = SR2DSM.decode(oAuthToken);
			theCitizen = buildCitizen(tokenDetails);
			theRequestDetails.setAttribute(SecurityConstants.CITIZEN_ATTR_NAME, theCitizen);
		    LOGGER.info("Received request {} for citizen {}", reqIdOpt.get(), theCitizen.getPersonIdentifier());
		} catch (Exception e) {
			LOGGER.error("Authentication token is not valid! Request cannot be processed.", e);
			throw new AuthenticationException("Authentication token is not valid! Request cannot be processed.");		
		}
		
		// #4 checks for a preferred language
		String preferredLanguages = theRequestDetails.getHeader("Accept-Language");

		// #5 Creates the EHRRequest and store it as a request attribute
		EHRRequest request = new EHRRequest(reqIdOpt.get(), theCitizen, preferredLanguages);
		theRequestDetails.setAttribute(SecurityConstants.EHR_REQUEST_ATTR_NAME, request);
		
	}

	private Citizen buildCitizen(ResponseDetails rd) {
		Citizen c = new Citizen();
		
		final UserDetails ud = rd.getUserDetails();
		c.setFirstName(ud.getFirstName());
		c.setFamilyName(ud.getFamilyName());
		c.setDateOfBirth(ud.getDateOfBirth());
		c.setGender(ud.getDateOfBirth());
		c.setPersonIdentifier(ud.getPersonIdentifier());
		
		return c;
	}
}
