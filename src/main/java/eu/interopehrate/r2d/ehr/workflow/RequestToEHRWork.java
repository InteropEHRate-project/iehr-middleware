package eu.interopehrate.r2d.ehr.workflow;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.EHRService;

class RequestToEHRWork implements Work {
	@Autowired(required = true)
	private EHRService ehrService;
	private final Logger logger = LoggerFactory.getLogger(RequestToEHRWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);		
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
					workContext.put(EHRRequestProcessor.CDA_DATA_KEY, response);
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
