package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRRequest;

public interface R2DAccessService {
	
	
	void sendSuccesfulResponse(EHRRequest ehrRequest, String fhirBundle) throws Exception;

	
	void sendUnsuccesfulResponse(EHRRequest ehrRequest, String errorMsg) throws Exception;

}
