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
import eu.interopehrate.r2d.ehr.services.R2DAccessService;

public class SendSuccessToR2DWork implements Work {
	
	@Autowired(required = true)
	private R2DAccessService r2dAccessService;
	
	private final Logger logger = LoggerFactory.getLogger(SendSuccessToR2DWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);

		try {
			EHRResponse ihsResponse = (EHRResponse) workContext.get(EHRRequestProcessor.FHIR_DATA_KEY);
			r2dAccessService.sendSuccesfulResponse(request, ihsResponse);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}

}
