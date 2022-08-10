package eu.interopehrate.r2d.ehr.image;

import eu.interopehrate.r2d.ehr.model.EHRFileResponse;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: defines the interface of a class used to extract images from 
 * a textual file, like an XML/CDA file or a JSON/FHIR file.
 * 
 * This operation is done to diminish the dimension of the files that need to be
 * sent to the IHS to be converted. Image data do not need to be converted and
 * when present represent 90% of the size of a file (and usually .
 * 
 * They are extracted from a file and stored separately in the file system. 
 * 
 * Example file 12345678.xml contains two images. After the ImageExtractor has
 * executed its task, in the file system there will be 3 files stored:
 * 1) 12345678_reduced.xml
 * 2) 12345678_imgPlaceholder_1
 * 3) 12345678_imgPlaceholder_2
 * 
 * The 12345678_reduced.xml does not contain anymore the B64 strings representing 
 * the images, but in their places there are two placeholder one containing the
 * string "_imgPlaceholder_1" and the other one "_imgPlaceholder_2".
 * 
 * The IHS will produce a JSON/FHIR file with Media resource that contains as 
 * values the afore mentioned placeholders. R2DAccessServer will replace placeholders
 * with the content of the two corresponding files.
 * 
 * 
 */
public interface ImageExtractor {

	/**
	 * 
	 * @param requestId
	 * @param fileResponse
	 * @return
	 * @throws Exception
	 */
	EHRFileResponse extractImageFromFiles(String requestId, EHRFileResponse fileResponse) throws Exception;
}
