package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public interface IHSService {

	/**
	 * 
	 * @param ehrRequest
	 * @param cdaBundle
	 * @throws Exception
	 */
	void requestConversion(EHRRequest ehrRequest, String cdaBundle) throws Exception;
	
	/**
	 * 
	 * @param ehrRequest
	 * @param lang
	 * @return
	 * @throws Exception
	 */
	EHRResponse retrieveConversionResult(EHRRequest ehrRequest) throws Exception;
}
