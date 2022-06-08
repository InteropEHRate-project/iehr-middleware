package eu.interopehrate.r2d.ehr.workflow;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.image.ImageExtractor;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class ImageExtractorWork implements Work {

	private final Logger logger = LoggerFactory.getLogger(ImageExtractorWork.class);

	@Autowired(required = true)
	ImageExtractor imageExtractor;
	
	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.EHR_REQUEST_KEY);		
		
		try {
			imageExtractor.extractImages(request.getR2dRequestId());
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception | Error e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
		
	}

}
