package eu.interopehrate.r2d.ehr.workflow;

import java.io.File;

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
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;

public class ExtractImageWork implements Work {

	private final Logger logger = LoggerFactory.getLogger(ExtractImageWork.class);
	
	@Override
	public WorkReport execute(WorkContext workContext) {
		String beanName = Configuration.getProperty(Configuration.EHR_IMAGE_EXTRACTOR);
		
		ImageExtractor imageExtractor = (ImageExtractor)
				EHRContextProvider.getApplicationContext().getBean(beanName);
		
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);	
		EHRResponse ehrResponse = (EHRResponse) workContext.get(EHRRequestProcessor.EHR_DATA_KEY);		
		
		try {
			File reducedFile = imageExtractor.createReducedFile(request.getR2dRequestId(), 
					ehrResponse.getResponse());
			
			EHRResponse ehrReducedResponse = new EHRResponse();
			ehrReducedResponse.setContentType(ehrResponse.getContentType());
			ehrReducedResponse.setOnFile(true);
			ehrReducedResponse.setStatus(EHRResponseStatus.COMPLETED);
			ehrReducedResponse.setResponse(reducedFile.getName());
			workContext.put(EHRRequestProcessor.EHR_REDUCED_DATA_KEY, ehrReducedResponse);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception | Error e) {
			logger.error(String.format("Task '%s' completed with error: %s", getClass().getSimpleName() ,e.getMessage()), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}
		
	}

}
