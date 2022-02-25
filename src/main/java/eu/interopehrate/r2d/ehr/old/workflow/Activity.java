package eu.interopehrate.r2d.ehr.old.workflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

@Deprecated
abstract class Activity implements Runnable {

	protected final Log logger = LogFactory.getLog(Activity.class);
	// private ArrayBlockingQueue<EHRRequest> queue = new ArrayBlockingQueue<>(20, true);

	@Autowired(required=true)
	protected EHRRequestWorkflowEngine workflowEngine;
	
	protected EHRRequest ehrRequest;
	protected EHRResponse ehrResponse;
	protected Status status;
		
	@Override
	public void run() {
		logger.info(String.format("Task %s started - [request %s] ", getName(),
				ehrRequest.getR2dRequestId()));
		try {
			processRequest(ehrRequest);
			workflowEngine.onTaskCompleted(ehrRequest, this);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			workflowEngine.onTaskCompletedWithError(ehrRequest, this, e.getMessage());
		}			
	}

	/**
	 * 
	 * @param request
	 * @throws Exception
	 */
	protected abstract void processRequest(EHRRequest request) throws Exception;

	public String getName() {
		return getClass().getSimpleName();
	}
	
	public EHRRequest getEhrRequest() {
		return ehrRequest;
	}

	public void setEhrRequest(EHRRequest ehrRequest) {
		this.ehrRequest = ehrRequest;
	}

}
