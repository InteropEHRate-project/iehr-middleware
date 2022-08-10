package eu.interopehrate.r2d.ehr.workflow;

import java.util.HashMap;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.converter.Converter;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.IHSService;
import eu.interopehrate.r2d.ehr.services.impl.LocalConversionService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Implementation of a Work class to handle the activity 
 * to download data from EHR.
 */

class RequestConversionWork implements Work {

	@Autowired(required = true)
	private LocalConversionService localConversionService;
	
	private final Logger logger = LoggerFactory.getLogger(RequestConversionWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		if (logger.isDebugEnabled())
			logger.debug("Starting Task: 'Conversion of data downloaded from EHR'");
		
		// retrieves IHS service bean
		String beanName = Configuration.getProperty(Configuration.IHS_SERVICE_BEAN);
		IHSService ihsService = (IHSService)
				EHRContextProvider.getApplicationContext().getBean(beanName);

		// Original request
		EHRRequest ehrRequest = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
		// retrieve patiend Id
		String patientId = (String)workContext.get(EHRRequestProcessor.PATIENT_ID_KEY);
		// response produced by EHR and reduced by Image Extractor
		EHRFileResponse ehrReducedResponse = (EHRFileResponse)workContext.get(
				EHRRequestProcessor.EHR_REDUCED_DATA_KEY);

		// Local conversion
		if (isLocal(ehrRequest)) {
			try {
				HashMap<String, String> properties = new HashMap<String, String>();
				properties.put(Converter.PATIENT_ID_KEY, patientId);
				
				EHRResponse convResponse = localConversionService.convert(ehrRequest, ehrReducedResponse, properties);
				workContext.put(EHRRequestProcessor.FHIR_DATA_KEY, convResponse);
				return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
			} catch (Exception | Error e) {
				logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
		} 
		
		// Remote conversion with IHS
		else {
			// #1 submit first request to the IHS Service for requesting a conversion
			// TO FIX: test with UNITN
			/*
			try {
				ihsService.startTransaction();		
			} catch (Exception | Error e) {
				logger.error("Task completed with error: " + e.getMessage(), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
			*/
						
			// #2 submit first request to the IHS Service for requesting a conversion
			try {
				ihsService.requestConversion(ehrRequest, ehrReducedResponse, patientId);			
			} catch (Exception | Error e) {
				logger.error("Task completed with error: " + e.getMessage(), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
			
			// this sleep is needed because sometimes happens that the 
			// retrieveFHIRHealthRecord starts when some operations of
			// requestConversion method are still executing. 
			// Probably the IHS-DB uses some async operations
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				logger.error("Error during Thread.sleep()");				
			}
			
			// #3 sends a second request to the IHS Service for retrieving the results
			try {
				EHRResponse ihsResponse = ihsService.retrieveFHIRHealthRecord(ehrRequest, patientId);
				if (ihsResponse.getStatus() == EHRResponseStatus.COMPLETED) {
					workContext.put(EHRRequestProcessor.FHIR_DATA_KEY, ihsResponse);
					return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
				} else {
					logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,ihsResponse.getMessage()));
					workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, ihsResponse.getMessage());
					return new DefaultWorkReport(WorkStatus.FAILED, workContext);				
				}
			} catch (Exception | Error e) {
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
