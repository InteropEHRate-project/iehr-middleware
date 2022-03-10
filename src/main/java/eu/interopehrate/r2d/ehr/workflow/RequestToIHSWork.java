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
import eu.interopehrate.r2d.ehr.services.IHSService;

class RequestToIHSWork implements Work {

	@Autowired(required = true)
	private IHSService ihsService;
	
	private final Log logger = LogFactory.getLog(RequestToIHSWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
		logger.info(String.format("Started Task %s ...", getClass().getSimpleName()));
		
		// #1 submit first request to the IHS Service for requesting a conversion
		try {
			String cdaBundle = (String) workContext.get(EHRRequestProcessor.CDA_DATA_KEY);
			ihsService.requestConversion(request, cdaBundle);
		} catch (Exception e) {
			logger.error("Task completed with error: " + e.getMessage());
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
		
		// #2 sends a second request to the IHS Service for retrieving the results
		try {
			EHRResponse ihsResponse = ihsService.retrieveConversionResult(request);
			if (ihsResponse.getStatus() == EHRResponseStatus.COMPLETED) {
				logger.info("Task completed succesfully!");
				workContext.put(EHRRequestProcessor.FHIR_DATA_KEY, ihsResponse.getResponse());
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
			} else {
				logger.error("Task completed with error: " + ihsResponse.getMessage());
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, ihsResponse.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);				
			}
		} catch (Exception e) {
			logger.error("Task completed with error: " + e.getMessage());
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}

}
