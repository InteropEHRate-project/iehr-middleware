package eu.interopehrate.r2d.ehr.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import eu.interopehrate.r2d.ehr.workflow.EHRRequestController;

@Service
public class EncounterResourceProvider implements IResourceProvider {

	@Autowired(required = true)
	private EHRRequestController requestController;

	@Override
	public Class<Encounter> getResourceType() {
		return Encounter.class;
	}

	@Search
	public List<Encounter> search(@RequiredParam(name = Encounter.SP_STATUS) String theStatus,
			@OptionalParam(name = Encounter.SP_DATE) DateParam theFromDate,
			HttpServletRequest theRequest) {	
		
		// Retrieves EHRRequest from the HttpRequest
		EHRRequest request = (EHRRequest)theRequest.getAttribute(SecurityConstants.EHR_REQUEST_ATTR_NAME);
		request.setOperation(R2DOperation.SEARCH_ENCOUNTER);
		Date fromDate = (theFromDate == null) ? null : theFromDate.getValue();
		request.addParameter(EHRRequest.PARAM_FROM, fromDate);

		// starts asynchronous request processing
		requestController.startRequestProcessing(request);

		return new ArrayList<Encounter>();
	}
	
	
	@Operation(name="$everything", idempotent=true)
	public Bundle everything(@IdParam IdType theEncounterId, HttpServletRequest theRequest) {
		// Retrieves EHRRequest from the HttpRequest
		EHRRequest request = (EHRRequest)theRequest.getAttribute(SecurityConstants.EHR_REQUEST_ATTR_NAME);
		request.setOperation(R2DOperation.ENCOUNTER_EVERYTHING);
		request.addParameter(EHRRequest.PARAM_RESOURCE_ID, theEncounterId);

		// starts asynchronous request processing
		requestController.startRequestProcessing(request);
		
		return new Bundle();
	}
	
}
