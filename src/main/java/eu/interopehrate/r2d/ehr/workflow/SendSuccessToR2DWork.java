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
import eu.interopehrate.r2d.ehr.services.R2DAccessService;

public class SendSuccessToR2DWork implements Work {
	
	@Autowired(required = true)
	private R2DAccessService r2dAccessService;
	
	private final Log logger = LogFactory.getLog(RequestToIHSWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
		logger.info("Started Task for request: " + request.getR2dRequestId());
		
		try {
			String fhirBundle = (String) workContext.get(EHRRequestProcessor.FHIR_DATA_KEY);
			r2dAccessService.sendSuccesfulResponse(request, fhirBundle);
			logger.info("Task completed succesfully!");
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception e) {
			logger.error("Task completed with error: " + e.getMessage());
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}

}
