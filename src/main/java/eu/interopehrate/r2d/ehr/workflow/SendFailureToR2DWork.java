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
import eu.interopehrate.r2d.ehr.services.R2DAccessService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Implementation of a Work class to handle the activity 
 * to notify the R2DAccess Server that a workflow ended with error.
 */

public class SendFailureToR2DWork implements Work {

	@Autowired(required = true)
	private R2DAccessService r2dAccessService;
	
	private final Logger logger = LoggerFactory.getLogger(SendFailureToR2DWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
		// logger.info(String.format("Started Task %s ...", getClass().getSimpleName()));
		
		try {
			String errorMsg = (String) workContext.get(EHRRequestProcessor.ERROR_MESSAGE_KEY);
			r2dAccessService.sendUnsuccesfulResponse(request, errorMsg);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}
}
