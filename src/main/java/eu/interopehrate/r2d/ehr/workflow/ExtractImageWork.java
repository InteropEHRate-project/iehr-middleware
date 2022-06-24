package eu.interopehrate.r2d.ehr.workflow;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.image.ImageExtractor;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class ExtractImageWork implements Work {

	private final Logger logger = LoggerFactory.getLogger(ExtractImageWork.class);
	
	@Override
	public WorkReport execute(WorkContext workContext) {
		// retrieves image extractor bean
		String beanName = Configuration.getProperty(Configuration.EHR_IMAGE_EXTRACTOR_BEAN);
		ImageExtractor imageExtractor = (ImageExtractor)
				EHRContextProvider.getApplicationContext().getBean(beanName);
		
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);	
		EHRFileResponse ehrResponse = (EHRFileResponse) workContext.get(EHRRequestProcessor.EHR_DATA_KEY);		
		
		try {
			EHRFileResponse ehrReducedResponse = imageExtractor.extractImageFromFiles(
									request.getR2dRequestId(), 
									ehrResponse);
			
			workContext.put(EHRRequestProcessor.EHR_REDUCED_DATA_KEY, ehrReducedResponse);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception | Error e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
	}

}
