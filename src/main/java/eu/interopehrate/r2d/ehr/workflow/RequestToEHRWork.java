package eu.interopehrate.r2d.ehr.workflow;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private final Log logger = LogFactory.getLog(RequestToEHRWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
		// logger.info(String.format("Started Task %s ...", getClass().getSimpleName()));
		
		String ehrPatientId = (String)workContext.get(EHRRequestProcessor.PATIENT_ID_KEY);

		EHRResponse response = null;
		try {
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
			
			// Check response
			if (response.getStatus() == EHRResponseStatus.FAILED) {
				logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,response.getMessage()));
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, response.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			} else {
				// logger.info("Task completed succesfully!");
				workContext.put(EHRRequestProcessor.CDA_DATA_KEY, response.getResponse());
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);				
			}		
		} catch (Exception e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}			
	}

}
