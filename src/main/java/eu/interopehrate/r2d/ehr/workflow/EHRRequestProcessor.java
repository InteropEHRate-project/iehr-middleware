package eu.interopehrate.r2d.ehr.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jeasy.flows.engine.WorkFlowEngine;
import org.jeasy.flows.engine.WorkFlowEngineBuilder;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkReportPredicate;
import org.jeasy.flows.workflow.ConditionalFlow;
import org.jeasy.flows.workflow.WorkFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.MemoryLogger;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Processor class that executes the overall workflow for 
 * handling a request coming from the R2D Access Server. The workflow is
 * composed by the following activities:
 * 1) Authenticate to EHR
 * 2) Download data from EHR
 * 3) Extract images from downloaded data
 * 4) Convert data to FHIR
 * 5) Send successful or unsuccessful notification to the R2DAccess Server
 */

public class EHRRequestProcessor /* implements Runnable */ {

	static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";
	static final String REQUEST_KEY = "REQUEST_KEY";
	static final String EHR_DATA_KEY = "EHR_DATA_KEY";
	static final String EHR_REDUCED_DATA_KEY = "EHR_REDUCED_DATA_KEY";
	static final String FHIR_DATA_KEY = "FHIR_DATA_KEY";
	static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";
	
	private final Logger logger = LoggerFactory.getLogger(EHRRequestProcessor.class);	
	private EHRRequest ehrRequest;
	private Boolean deleteTempFiles;
	
	public EHRRequestProcessor(EHRRequest ehrRequest) {
		super();
		this.ehrRequest = ehrRequest;
		this.deleteTempFiles = Boolean.valueOf(Configuration.getProperty(Configuration.EHR_DELETE_TEMP_FILES));
	}

	public void run() {
		Work authenticateCitizenToEHRWork = (Work)EHRContextProvider.getApplicationContext().getBean("AuthenticateCitizenToEHRWork");
		Work downloadFromEHRWork = (Work)EHRContextProvider.getApplicationContext().getBean("DownloadFromEHRWork");
		Work extractImagesWork = (Work)EHRContextProvider.getApplicationContext().getBean("ExtractImagesWork");
		Work anonymizeImagesWork = (Work)EHRContextProvider.getApplicationContext().getBean("AnonymizeImagesWork");
		Work convertToFHIRWork = (Work)EHRContextProvider.getApplicationContext().getBean("ConvertToFHIRWork");
		Work sendFailureToR2DWork = (Work)EHRContextProvider.getApplicationContext().getBean("SendFailureToR2DWork");
		Work sendSuccessToR2DWork = (Work)EHRContextProvider.getApplicationContext().getBean("SendSuccessToR2DWork");
		
		// Builds the workflow		
		WorkFlow conversionWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Conversion To FHIR workflow")
				.execute(convertToFHIRWork)
				.when(WorkReportPredicate.COMPLETED)
				.then(sendSuccessToR2DWork)
				.otherwise(sendFailureToR2DWork)
				.build();
		
		WorkFlow anonymizationWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Image anonymization workflow")
				.execute(anonymizeImagesWork)
				.when(WorkReportPredicate.COMPLETED)
				.then(conversionWorkFlow)
				.otherwise(sendFailureToR2DWork)
				.build();
		
		WorkFlow imageReductionWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Image extraction workflow")
				.execute(extractImagesWork)
				.when(WorkReportPredicate.COMPLETED)
				.then(anonymizationWorkFlow)
				.otherwise(sendFailureToR2DWork)
				.build();
		
		WorkFlow ehrWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Request To EHR workflow")
				.execute(downloadFromEHRWork)
				.when(WorkReportPredicate.COMPLETED)
				.then(imageReductionWorkFlow)
				.otherwise(sendFailureToR2DWork)
				.build();
		
		WorkFlow mainWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("R2DRequest Management workflow")
				.execute(authenticateCitizenToEHRWork)
				.when(WorkReportPredicate.COMPLETED)
				.then(ehrWorkFlow)
				.otherwise(sendFailureToR2DWork)
				.build();
		
		// creates the engine instance
		WorkFlowEngine workFlowEngine = WorkFlowEngineBuilder.aNewWorkFlowEngine().build();

		// creates the work context and adds the ehrRequest to it
		WorkContext workContext = new WorkContext();
		workContext.put(REQUEST_KEY, ehrRequest);
		
		// starts the workflow
		MemoryLogger.logMemory();
		logger.info(String.format("Starting processing of request: %s", ehrRequest.getR2dRequestId()));		
		WorkReport report = workFlowEngine.run(mainWorkflow, workContext);
		// check status of execution of the workflow
		if (workContext.get(ERROR_MESSAGE_KEY) == null) {
			logger.info("Processing of request: {} completed with SUCCESS", 
					ehrRequest.getR2dRequestId());
		} else {
			final String errorMsg = (String) workContext.get(ERROR_MESSAGE_KEY);
			logger.error("Processing of request: {} completed with ERROR: {}", 
					ehrRequest.getR2dRequestId(), errorMsg);			
		}
		
		// delete tmp files from EHRMW folder 
		if (deleteTempFiles)
			deleteTemporaryFiles();

				
		// prints memory
		MemoryLogger.logMemory();
	}
	
	
	private void deleteTemporaryFiles() {
		if (logger.isDebugEnabled())
			logger.debug("Deleting temporary files downloaded...");

		Path filePath = Paths.get(Configuration.getDBPath());
		final String requestId = ehrRequest.getR2dRequestId();
		try {
			Files.walk(filePath)
			.filter(Files::isRegularFile)
			.filter(tmpFile -> tmpFile.getName(tmpFile.getNameCount() - 1).toString().startsWith(requestId))
			.forEach(tmpFile -> {
				try {
					Files.deleteIfExists(tmpFile);
				} catch (Exception ioe) {
					logger.warn("Not able to delete tmp file {}", tmpFile.toString());
				}
			});
		} catch (IOException e) {
			logger.warn("Not able to delete tmp files...");
		}
	}

}
