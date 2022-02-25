package eu.interopehrate.r2d.ehr.providers;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import eu.interopehrate.r2d.ehr.workflow.EHRRequestController;

@Service
public class PatientResourceProvider implements IResourceProvider {

	@Autowired(required = true)
	private EHRRequestController requestController;

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}

	
	@Operation(name="$patient-summary", idempotent=true)
	public Bundle patientSummary(HttpServletRequest theRequest) {
		// Retrieves requestId and Citizen from request
		String theRequestId = theRequest.getHeader(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		Citizen theCitizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);

		EHRRequest request = new EHRRequest(theRequestId, theCitizen, R2DOperation.PATIENT_SUMMARY);

		// starts asynchronous request processing
		requestController.startRequestProcessing(request);
		
		return new Bundle();
	}

}
