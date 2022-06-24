package eu.interopehrate.r2d.ehr.image;

import eu.interopehrate.r2d.ehr.model.EHRFileResponse;

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
