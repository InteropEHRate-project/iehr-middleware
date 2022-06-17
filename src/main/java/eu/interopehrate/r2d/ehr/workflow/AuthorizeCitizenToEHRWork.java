package eu.interopehrate.r2d.ehr.workflow;

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
import eu.interopehrate.r2d.ehr.services.EHRService;

class AuthorizeCitizenToEHRWork implements Work {
	@Autowired(required = true)
	private EHRService ehrService;
	private final Logger logger = LoggerFactory.getLogger(AuthorizeCitizenToEHRWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
		//logger.info(String.format("Started Task %s ...", getClass().getSimpleName()));
	
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
