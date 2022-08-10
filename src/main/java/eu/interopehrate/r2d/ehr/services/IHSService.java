package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of the IHSService. Defines services
 * for requesting conversion of data.
 */

public interface IHSService {

	void startTransaction() throws Exception;
	
	/**
	 * Invokes the service of EHR to request the conversion of some 
	 * health data of the patient retrieve by the EHR.
	 * 
	 * @param ehrRequest
	 * @param ehrResponse
	 * @param patientId
	 * 
	 * @throws Exception
	 */
	void requestConversion(EHRRequest ehrRequest, 
			EHRFileResponse ehrResponse,
			String patientId) throws Exception;
	
	/**
	 * Invokes the IHS to get the result of a previously requested conversion
	 * 
	 * @param ehrRequest
	 * @param patientId
	 * 
	 * @return EHRResponse
	 * @throws Exception
	 */
	EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest,
			String patientId) throws Exception;
	
}
