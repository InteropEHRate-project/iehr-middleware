package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public interface IHSService {

	
	void requestConversion(EHRRequest ehrRequest, String cdaBundle) throws Exception;
	
	
	EHRResponse retrieveConversionResult(EHRRequest ehrRequest) throws Exception;
}
