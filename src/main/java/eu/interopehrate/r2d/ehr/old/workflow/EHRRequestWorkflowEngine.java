package eu.interopehrate.r2d.ehr.old.workflow;

import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRRequestStatus;

@Deprecated
public class EHRRequestWorkflowEngine {
	
	private static final String REQUEST_TO_EHR_TASK = "RequestToEHRTask";
	private static final String REQUEST_TO_IHS_TASK = "RequestToIHSTask";
	private static final String SEND_TO_R2D_TASK = "SendToR2DTask";
	
	@Autowired(required = true)
	private Executor executor;

	private final Log logger = LogFactory.getLog(EHRRequestWorkflowEngine.class);	
    
	/*
	 * Deve sbloccare il Thread
	 */
	public void startRequestProcessing(EHRRequest request) {
		logger.info(String.format("Processing of request %s started.", request.getR2dRequestId()));
		// creates the execution context
		Activity task = (Activity)EHRContextProvider.getApplicationContext().getBean(REQUEST_TO_EHR_TASK);
		task.setEhrRequest(request);
		executor.execute(task);
		request.setStatus(EHRRequestStatus.TO_EHR);
	}
		
	void onTaskCompleted(EHRRequest request, Activity completedTask) {
		logger.info(String.format("Task %s completed succesfully [request %s]", 
				completedTask.getName(), request.getR2dRequestId()));
		
		if (completedTask.getName().equals(REQUEST_TO_EHR_TASK)) {	
			request.setStatus(EHRRequestStatus.TO_IHS);
			Activity task = (Activity)EHRContextProvider.getApplicationContext().getBean(REQUEST_TO_IHS_TASK);
			task.setEhrRequest(request);
			executor.execute(task);
		} else if (completedTask.getName().equals(REQUEST_TO_IHS_TASK)) {
			request.setStatus(EHRRequestStatus.EXECUTED);
			Activity task = (Activity)EHRContextProvider.getApplicationContext().getBean(SEND_TO_R2D_TASK);
			task.setEhrRequest(request);
			executor.execute(task);
		} else if (completedTask.getName().equals(SEND_TO_R2D_TASK)) {				
			logger.info(String.format("Processing of request %s completed succesfully.", request.getR2dRequestId()));
			request.setEhrResponse(null);
			request.setIhsResponse(null);
			request.setStatus(EHRRequestStatus.COMPLETED);
		}
	}

	void onTaskCompletedWithError(EHRRequest request, Activity completedTask, String error) {
		// TODO: add error to Request and invoke error callback
		// TODO: Handle retry
		logger.info(String.format("Task %s completed with error, Request %s FAILED)", 
				completedTask.getName(), request.getR2dRequestId()));

		if (completedTask.getName().equals(REQUEST_TO_EHR_TASK)) {	
			request.setStatus(EHRRequestStatus.EHR_FAILED);
			Activity task = (Activity)EHRContextProvider.getApplicationContext().getBean(SEND_TO_R2D_TASK);
			task.setEhrRequest(request);
			executor.execute(task);
		} else if (completedTask.getName().equals(REQUEST_TO_IHS_TASK)) {
			request.setStatus(EHRRequestStatus.IHS_FAILED);
			Activity task = (Activity)EHRContextProvider.getApplicationContext().getBean(SEND_TO_R2D_TASK);
			task.setEhrRequest(request);
			executor.execute(task);
		} else if (completedTask.getName().equals(SEND_TO_R2D_TASK)) {
			request.setStatus(EHRRequestStatus.R2D_FAILED);
			request.setEhrResponse(null);
			request.setIhsResponse(null);
		}	
	}

}
