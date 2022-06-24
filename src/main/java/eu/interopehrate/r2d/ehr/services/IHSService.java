package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public interface IHSService {

	/**
	 * Invokes the service of EHR to invoke the conversion of a bundle
	 * 
	 * @param ehrRequest
	 * @param cdaBundle
	 * @throws Exception
	 */
	void requestConversion(EHRRequest ehrRequest, EHRFileResponse ehrResponse) throws Exception;
	
	/**
	 * Involkes the service of EHR to get the result of a previously invoked conversion
	 * 
	 * @param ehrRequest
	 * @param lang
	 * @return
	 * @throws Exception
	 */
	EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest) throws Exception;
	
}
