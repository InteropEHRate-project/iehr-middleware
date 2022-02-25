package eu.interopehrate.r2d.ehr.security;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.sr2dsm.SR2DSM;
import eu.interopehrate.sr2dsm.model.ResponseDetails;
import eu.interopehrate.sr2dsm.model.UserDetails;

@Interceptor
public class R2DRequestAuthenticator {
	private static final Logger LOGGER = LoggerFactory.getLogger(R2DRequestAuthenticator.class);
				
	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void doFilter(RequestDetails theRequestDetails) throws IOException {
		LOGGER.info(String.format("Received the following R2D request: %s", theRequestDetails.getCompleteUrl()));
		
		// Checks mandatory header parameters
		
		// #1 is R2D_REQUEST_ID_PARAM_NAME
		Optional<String> tmp = Optional.fromNullable(theRequestDetails.getHeader(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME));
		if (!tmp.isPresent()) {
			LOGGER.error("The request lacks the R2D request ID header parameter, thus cannot be processed.");
			throw new AuthenticationException("The request lacks the R2D request ID header parameter, thus cannot be processed.");		
		}
		
		// #2 is R2D_REQUEST_CITIZEN_PARAM_NAME
		tmp = Optional.fromNullable(theRequestDetails.getHeader(SecurityConstants.R2D_REQUEST_CITIZEN_PARAM_NAME));
		if (!tmp.isPresent()) {
			LOGGER.error("The request lacks the R2D citizen token header parameter, thus cannot be processed.");
			throw new AuthenticationException("The request lacks the R2D citizen token header parameter, thus cannot be processed.");		
		}
		
		// #3 checking EIDAS token
		
		try {
			LOGGER.debug("Verifying EIDAS token...");
			String oAuthToken = tmp.get().substring(SecurityConstants.OAUTH_PREFIX.length()).trim();
			ResponseDetails tokenDetails = SR2DSM.decode(oAuthToken);
			Citizen theCitizen = buildCitizen(tokenDetails);
			theRequestDetails.setAttribute(SecurityConstants.CITIZEN_ATTR_NAME, theCitizen);
		    LOGGER.info("Request is valid and regards citizen " + theCitizen.getPersonIdentifier());
		} catch (Exception e) {
			LOGGER.error("Authentication token is not valid! Request cannot be processed.", e);
			throw new AuthenticationException("Authentication token is not valid! Request cannot be processed.");		
		}
		
	}

	private Citizen buildCitizen(ResponseDetails rd) {
		Citizen c = new Citizen();
		
		final UserDetails ud = rd.getUserDetails();
		c.setFirstName(ud.getFirstName());
		c.setFamilyName(ud.getFamilyName());
		c.setDateOfBirth(LocalDate.parse(ud.getDateOfBirth(), DateTimeFormatter.ofPattern("uuuu-M-d")));
		c.setGender(ud.getDateOfBirth());
		c.setPersonIdentifier(ud.getPersonIdentifier());
		
		return c;
	}
}
