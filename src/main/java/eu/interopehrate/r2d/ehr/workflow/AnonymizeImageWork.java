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
import eu.interopehrate.r2d.ehr.image.DicomAnonymizer;
import eu.interopehrate.r2d.ehr.model.EHRRequest;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Implementation of a Work class to handle the activity 
 * of DICOM images anonymization.
 */

public class AnonymizeImageWork implements Work {
	
	private final Logger logger = LoggerFactory.getLogger(AnonymizeImageWork.class);

	@Override
	public WorkReport execute(WorkContext workContext) {
		if (logger.isDebugEnabled())
			logger.debug("Starting Task: 'Anonymize images from data downloaded from EHR'");
		
		// retrieves image anonymizer bean
		String beanName = Configuration.getProperty(Configuration.EHR_IMAGE_ANONYMIZER_BEAN);
		DicomAnonymizer dicomAnonymizer = (DicomAnonymizer)
				EHRContextProvider.getApplicationContext().getBean(beanName);
		
		try {
			EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
			dicomAnonymizer.anonymizeDicomImages(request);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
			return new DefaultWorkReport(WorkStatus.FAILED, workContext);
		}		
	}

}
