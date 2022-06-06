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
import eu.interopehrate.r2d.ehr.services.IHSService;

@Deprecated
class RequestToIHSWork implements Work {

	@Autowired(required = true)
	private IHSService ihsService;
	
	private final Logger logger = LoggerFactory.getLogger(RequestToIHSWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
		// #1 submit first request to the IHS Service for requesting a conversion
		try {
			EHRResponse ehrResponse = (EHRResponse)workContext.get(EHRRequestProcessor.CDA_DATA_KEY);
			ihsService.requestConversion(request, ehrResponse);			
		} catch (Exception e) {
			logger.error("Task completed with error: " + e.getMessage(), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
		
		// #2 sends a second request to the IHS Service for retrieving the results
		try {
			EHRResponse ihsResponse = ihsService.retrieveFHIRHealthRecord(request);
			if (ihsResponse.getStatus() == EHRResponseStatus.COMPLETED) {
				workContext.put(EHRRequestProcessor.FHIR_DATA_KEY, ihsResponse);
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
			} else {
				logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,ihsResponse.getMessage()));
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, ihsResponse.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);				
			}
		} catch (Exception e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}

}
