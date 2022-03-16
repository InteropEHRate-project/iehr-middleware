package eu.interopehrate.r2d.ehr.workflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jeasy.flows.engine.WorkFlowEngine;
import org.jeasy.flows.engine.WorkFlowEngineBuilder;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReportPredicate;
import org.jeasy.flows.workflow.ConditionalFlow;
import org.jeasy.flows.workflow.WorkFlow;

import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class EHRRequestProcessor implements Runnable {

	static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";
	static final String EHR_REQUEST_KEY = "EHR_REQUEST_KEY";
	static final String CDA_DATA_KEY = "CDA_DATA_KEY";
	static final String FHIR_DATA_KEY = "FHIR_DATA_KEY";
	static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";
	
	private final Log logger = LogFactory.getLog(EHRRequestProcessor.class);	
	private EHRRequest ehrRequest;
	
	public EHRRequestProcessor(EHRRequest ehrRequest) {
		super();
		this.ehrRequest = ehrRequest;
	}

	@Override
	public void run() {
		Work authorizeCitizenToEHR = (Work)EHRContextProvider.getApplicationContext().getBean("AuthorizeCitizenToEHR");
		Work requestToEHR = (Work)EHRContextProvider.getApplicationContext().getBean("RequestToEHRWork");
		Work requestToIHS = (Work)EHRContextProvider.getApplicationContext().getBean("RequestToIHSWork");
		Work SendFailureToR2D = (Work)EHRContextProvider.getApplicationContext().getBean("SendFailureToR2DWork");
		Work SendSuccessToR2D = (Work)EHRContextProvider.getApplicationContext().getBean("SendSuccessToR2DWork");
			
		// Builds the workflow
		WorkFlow secondaryWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("R2DRequest To IHS workflow")
				.execute(requestToIHS)
				.when(WorkReportPredicate.COMPLETED)
				.then(SendSuccessToR2D)
				.otherwise(SendFailureToR2D)
				.build();

		WorkFlow primaryWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("R2DRequest To EHR workflow")
				.execute(requestToEHR)
				.when(WorkReportPredicate.COMPLETED)
				.then(secondaryWorkflow)
				.otherwise(SendFailureToR2D)
				.build();
		
		WorkFlow preliminaryWorkflow = ConditionalFlow.Builder.aNewConditionalFlow()
				.named("R2DRequest management workflow")
				.execute(authorizeCitizenToEHR)
				.when(WorkReportPredicate.COMPLETED)
				.then(primaryWorkflow)
				.otherwise(SendFailureToR2D)
				.build();

		// creates the engine instance
		WorkFlowEngine workFlowEngine = WorkFlowEngineBuilder.aNewWorkFlowEngine().build();

		// creates the work context and adds the ehrRequest to it
		WorkContext workContext = new WorkContext();
		workContext.put(EHR_REQUEST_KEY, ehrRequest);
		
		// starts the workflow
		logger.info(String.format("Starting processing of request: %s", ehrRequest.getR2dRequestId()));
		workFlowEngine.run(preliminaryWorkflow, workContext);
		
		String errorMsg = (String) workContext.get(ERROR_MESSAGE_KEY);
		if (errorMsg != null) 
			logger.error(String.format("Processing of request: %s completed with ERROR: %s",
					ehrRequest.getR2dRequestId(), errorMsg));
		else
			logger.info(String.format("Processing of request: %s completed with SUCCESS",
					ehrRequest.getR2dRequestId()));
	}

}
