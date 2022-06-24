package eu.interopehrate.r2d.ehr.workflow;

import java.util.Date;

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
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.EHRService;

class RequestToEHRWork implements Work {	
	private final Logger logger = LoggerFactory.getLogger(RequestToEHRWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		// retrieves EHR service bean
		String beanName = Configuration.getProperty(Configuration.EHR_EHR_SERVICE_BEAN);
		EHRService ehrService = (EHRService)
				EHRContextProvider.getApplicationContext().getBean(beanName);
		
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);		
		String ehrPatientId = (String)workContext.get(EHRRequestProcessor.PATIENT_ID_KEY);

		EHRResponse response = null;
		try {
			// #1 Invokes EHR operation
			if (request.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
				response = ehrService.executeSearchEncounter(
						(Date) request.getParameter(EHRRequest.PARAM_FROM),
						request.getR2dRequestId(), 
						ehrPatientId);
	
			} else if (request.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
				// step #1: retrieves a descriptor of the structure of the enc env
				response = ehrService.executeEncounterEverything(
						(String) request.getParameter(EHRRequest.PARAM_RESOURCE_ID),
						request.getR2dRequestId(), 
						ehrPatientId);
				
			} else if (request.getOperation() == R2DOperation.PATIENT_SUMMARY) {
				response = ehrService.executeGetPatientSummary(
						request.getR2dRequestId(), 
						ehrPatientId);
			} else {
				logger.error(String.format("Task '%s' completed with error: EHR Operation %s not implemented", 
						getClass().getSimpleName(), request.getOperation()));
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, 
						"EHR Operation " + request.getOperation() + " not implemented.");
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
			
			// #2 Check response
			if (response == null) {
				logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName(), "No response produced."));
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, "No response produced.");
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			} else {
				if (response.getStatus() == EHRResponseStatus.COMPLETED) {
					workContext.put(EHRRequestProcessor.EHR_DATA_KEY, response);
					return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);				
				} else {
					logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName(), response.getMessage()));
					workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, response.getMessage());
					return new DefaultWorkReport(WorkStatus.FAILED, workContext);
				}
			}
		} catch (Exception | Error e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}		
	}

}
