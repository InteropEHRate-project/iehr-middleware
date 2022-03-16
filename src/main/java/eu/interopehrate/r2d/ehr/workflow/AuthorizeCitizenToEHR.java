package eu.interopehrate.r2d.ehr.workflow;

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
import eu.interopehrate.r2d.ehr.services.EHRService;

class AuthorizeCitizenToEHR implements Work {
	@Autowired(required = true)
	private EHRService ehrService;
	private final Log logger = LogFactory.getLog(AuthorizeCitizenToEHR.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
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
		} catch (Exception e) {
			logger.error("Task completed with error: " + e.getMessage());
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}			
	}

}
