package eu.interopehrate.r2d.ehr.workflow;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.services.EHRService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Implementation of a Work class to handle the activity 
 * to authenticate the citizen to the EHR.
 */

class AuthorizeCitizenToEHRWork implements Work {

	private final Logger logger = LoggerFactory.getLogger(AuthorizeCitizenToEHRWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		if (logger.isDebugEnabled())
			logger.debug("Starting Task: 'Authenticate citizen to EHR'");
		
		String beanName = Configuration.getProperty(Configuration.EHR_EHR_SERVICE_BEAN);
		EHRService ehrService = (EHRService)
				EHRContextProvider.getApplicationContext().getBean(beanName);

		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
	
		EHRResponse response = null;
		try {
			response = ehrService.executeGetPatient(request.getR2dRequestId(), request.getCitizen());
			
			// Check response
			if (response.getStatus() == EHRResponseStatus.FAILED) {
				logger.error("Task completed with error: " + response.getMessage());
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, response.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			} else {
				//logger.info("Task completed succesfully!");
				workContext.put(EHRRequestProcessor.PATIENT_ID_KEY, response.getResponse());
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);				
			}		
		} catch (Exception | Error e) {
			logger.error("Task completed with error: " + e.getMessage(), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}			
	}

}
