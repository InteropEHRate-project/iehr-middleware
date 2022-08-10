package eu.interopehrate.r2d.ehr.services;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of the R2DAccessService. It defines services
 * to send notifications about request processing to the R2DAccess Server.
 * 
 */

public interface R2DAccessService {
	
	
	void sendSuccesfulResponse(EHRRequest ehrRequest, EHRResponse ihsResponse) throws Exception;

	
	void sendUnsuccesfulResponse(EHRRequest ehrRequest, String errorMsg) throws Exception;

}
