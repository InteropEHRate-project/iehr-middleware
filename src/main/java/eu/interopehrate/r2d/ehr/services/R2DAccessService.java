package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public interface R2DAccessService {
	
	
	void sendSuccesfulResponse(EHRRequest ehrRequest, EHRResponse ihsResponse) throws Exception;

	
	void sendUnsuccesfulResponse(EHRRequest ehrRequest, String errorMsg) throws Exception;

}
