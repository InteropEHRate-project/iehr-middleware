package eu.interopehrate.r2d.ehr.image;

import eu.interopehrate.r2d.ehr.model.EHRRequest;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of the DicomAnonymizer. Implementations 
 * of this interface are used by the workflow to anonymize the 
 * DICOM images downloaded from the EHR.
 * 
 */
public interface DicomAnonymizer {
	public static final String ANONYMIZED_FILE_SUFFIX = "_anon";
	
	public void anonymizeDicomImages(EHRRequest ehrRequest) throws Exception;

}
