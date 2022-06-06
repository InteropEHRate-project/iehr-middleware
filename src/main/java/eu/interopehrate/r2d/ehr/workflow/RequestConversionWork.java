package eu.interopehrate.r2d.ehr.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.IHSService;
import eu.interopehrate.r2d.ehr.services.LocalConversionService;

class RequestConversionWork implements Work {

	@Autowired(required = true)
	private IHSService ihsService;
	
	@Autowired(required = true)
	private LocalConversionService localConversionService;
	
	private final Logger logger = LoggerFactory.getLogger(RequestConversionWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		// Original request
		EHRRequest ehrRequest = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);
		// response produced by EHR
		EHRResponse ehrResponse = (EHRResponse)workContext.get(EHRRequestProcessor.CDA_DATA_KEY);
		
		// Local conversion
		if (isLocal(ehrRequest)) {
			try {
				EHRResponse convResponse = localConversionService.convert(ehrRequest, ehrResponse);
				workContext.put(EHRRequestProcessor.FHIR_DATA_KEY, convResponse);
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
			} catch (Exception e) {
				logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
		} 
		
		// Remote conversion with IHS
		else {
			// #1 submit first request to the IHS Service for requesting a conversion
			try {
				ihsService.requestConversion(ehrRequest, ehrResponse);			
			} catch (Exception e) {
				logger.error("Task completed with error: " + e.getMessage(), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
			
			
			
			
			// #2 sends a second request to the IHS Service for retrieving the results
			try {
				EHRResponse ihsResponse = ihsService.retrieveFHIRHealthRecord(ehrRequest);
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

	
	private boolean isLocal(EHRRequest ehrRequest) {
		String propertyName = "";
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER)
			propertyName = "conversion.encounterlist";
		else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING)
			propertyName = "conversion.encounterEverything";
		else if (ehrRequest.getOperation() == R2DOperation.PATIENT_SUMMARY)
			propertyName = "conversion.patientSummary";
		
		String value = Configuration.getProperty(propertyName);
		if (value != null && "local".equalsIgnoreCase(value))
			return true;
		
		return false;
	}
	
}
