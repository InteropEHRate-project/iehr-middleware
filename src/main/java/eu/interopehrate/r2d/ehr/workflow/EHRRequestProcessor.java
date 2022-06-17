package eu.interopehrate.r2d.ehr.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import org.jeasy.flows.engine.WorkFlowEngine;
import org.jeasy.flows.engine.WorkFlowEngineBuilder;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReportPredicate;
import org.jeasy.flows.workflow.ConditionalFlow;
import org.jeasy.flows.workflow.ParallelFlow;
import org.jeasy.flows.workflow.WorkFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.MemoryLogger;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class EHRRequestProcessor /* implements Runnable */ {

	static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";
	static final String REQUEST_KEY = "REQUEST_KEY";
	static final String EHR_DATA_KEY = "EHR_DATA_KEY";
	static final String EHR_REDUCED_DATA_KEY = "EHR_REDUCED_DATA_KEY";
	static final String FHIR_DATA_KEY = "FHIR_DATA_KEY";
	static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";
	
	private final Logger logger = LoggerFactory.getLogger(EHRRequestProcessor.class);	
	private EHRRequest ehrRequest;
	
	public EHRRequestProcessor(EHRRequest ehrRequest) {
		super();
		this.ehrRequest = ehrRequest;
	}

	public void run() {
		Work authorizeCitizenToEHR = (Work)EHRContextProvider.getApplicationContext().getBean("AuthorizeCitizenToEHRWork");
		Work requestToEHR = (Work)EHRContextProvider.getApplicationContext().getBean("RequestToEHRWork");
		Work extractImages = (Work)EHRContextProvider.getApplicationContext().getBean("ExtractImagesWork");
		Work anonymizeImages = (Work)EHRContextProvider.getApplicationContext().getBean("AnonymizeImagesWork");
		Work requestConversion = (Work)EHRContextProvider.getApplicationContext().getBean("RequestConversionWork");
		Work SendFailureToR2D = (Work)EHRContextProvider.getApplicationContext().getBean("SendFailureToR2DWork");
		Work SendSuccessToR2D = (Work)EHRContextProvider.getApplicationContext().getBean("SendSuccessToR2DWork");
		
		// Builds the workflow
		/*
		WorkFlow localConversion = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Convert data to FHIR")
				.execute(requestConversion)
				.when(WorkReportPredicate.ALWAYS_TRUE)
				.then(SendSuccessToR2D)
				.otherwise(SendFailureToR2D)
				.build();		
		 
		WorkFlow conversionWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Convert data workflow")
				.execute(requestConversion)
				.when(WorkReportPredicate.COMPLETED)
				.then(SendSuccessToR2D)
				.otherwise(SendFailureToR2D)
				.build();
		*/
		
		WorkFlow parallelWorkFlow = ParallelFlow.Builder.aNewParallelFlow()
				.named("Parallerl Conversion and Anonymization")
				.execute(anonymizeImages, requestConversion)
				.with(Executors.newFixedThreadPool(2))
				.build();
		
		
		WorkFlow conversionAndAnonymizationWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Conversion and Anonymization")
				.execute(parallelWorkFlow)
				.when(WorkReportPredicate.COMPLETED)
				.then(SendSuccessToR2D)
				.otherwise(SendFailureToR2D)
				.build();
		

		WorkFlow imageReductionWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Image extraction workflow")
				.execute(extractImages)
				.when(WorkReportPredicate.COMPLETED)
				.then(conversionAndAnonymizationWorkFlow)
				.otherwise(SendFailureToR2D)
				.build();

		
		WorkFlow ehrWorkFlow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("Request To EHR workflow")
				.execute(requestToEHR)
				.when(WorkReportPredicate.COMPLETED)
				.then(imageReductionWorkFlow)
				.otherwise(SendFailureToR2D)
				.build();

		
		WorkFlow mainWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("R2DRequest Management workflow")
				.execute(authorizeCitizenToEHR)
				.when(WorkReportPredicate.COMPLETED)
				.then(ehrWorkFlow)
				.otherwise(SendFailureToR2D)
				.build();

		// creates the engine instance
		WorkFlowEngine workFlowEngine = WorkFlowEngineBuilder.aNewWorkFlowEngine().build();

		// creates the work context and adds the ehrRequest to it
		WorkContext workContext = new WorkContext();
		workContext.put(REQUEST_KEY, ehrRequest);
		
		// starts the workflow
		logger.info(String.format("Starting processing of request: %s", ehrRequest.getR2dRequestId()));
		MemoryLogger.logMemory();

		workFlowEngine.run(mainWorkflow, workContext);
		
		// final log message
		String errorMsg = (String) workContext.get(ERROR_MESSAGE_KEY);
		if (errorMsg != null) 
			logger.error("Processing of request: {} completed with ERROR: {}", ehrRequest.getR2dRequestId(), errorMsg);
		else
			logger.info("Processing of request: {} completed with SUCCESS", ehrRequest.getR2dRequestId());
		
		// delete tmp file
		Boolean deleteTempFiles = Boolean.valueOf(Configuration.getProperty(Configuration.EHR_DELETE_TEMP_FILES));
		if (deleteTempFiles)
			deleteTempFiles();

		// prints memory
		MemoryLogger.logMemory();
	}
	
	
	private void deleteTempFiles() {
		String ehrFileName = String.format("%s%s.%s", Configuration.getDBPath(), 
				ehrRequest.getR2dRequestId(),
				Configuration.getProperty(Configuration.EHR_FILE_EXT));
		Path filePath = Paths.get(ehrFileName);
		try {
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			logger.warn("Error while deleting file {}", ehrFileName);
		}
	}

}
