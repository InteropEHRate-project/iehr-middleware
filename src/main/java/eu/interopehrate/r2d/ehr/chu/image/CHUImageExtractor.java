package eu.interopehrate.r2d.ehr.chu.image;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.image.ImageConstants;
import eu.interopehrate.r2d.ehr.image.ImageExtractor;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: files downloaded from the EHR of CHU does not 
 * contains images, because they are in separate files. So this
 * class has been implemented in order to be compliant with the
 * overall processing of a request that contains an activity 
 * that extract images from the files that must be sent to the
 * IHS. 
 * 
 * This class does not extract any image, it only applies a renaming policy
 * so that images are stored where the R2DAccessServer expects them to be.
 */
public class CHUImageExtractor implements ImageExtractor {
	private static final String PDF_PLACEHOLDER = "_pdfPlaceholder";
	private static final String ECG_PDF_FILE_TYPE = "63914";
	private static final String ECHO_DICOM_FILE_TYPE = "63314";
	
	private Logger logger = LoggerFactory.getLogger(CHUImageExtractor.class);

	@Override
	public EHRFileResponse extractImageFromFiles(String requestId, EHRFileResponse fileResponse) throws Exception {
		// Retrieves storage path(s) from config file
		String ehrMWStoragePath = Configuration.getDBPath();
		String r2daStoragePath = Configuration.getR2DADBPath();
		
		// Retrieves file extension
		String fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
		
		// file that contains ECG: 63914
		String ecgFileName = requestId + "_" + ECG_PDF_FILE_TYPE + fileExtension;
		
		// file that contains ECHO: 63314
		String echoFileName = requestId + "_" + ECHO_DICOM_FILE_TYPE + fileExtension;
		
		EHRFileResponse reducedResponse = new EHRFileResponse();
		reducedResponse.setContentType(fileResponse.getContentType());
		reducedResponse.setStatus(fileResponse.getStatus());
		
		File ehrFile, renamedFile;
		// String newFileName;
		for (int i = 0; i < fileResponse.getResponseFileSize(); i++) {
			ehrFile = fileResponse.getResponseFile(i);
			if (ecgFileName.equals(ehrFile.getName())) {
				// the ECG file of CHU is a PDF that does not need to be anonymized
				renamedFile = new File(r2daStoragePath + requestId + 
						PDF_PLACEHOLDER + "_" + ECG_PDF_FILE_TYPE);
				logger.debug("Renaming file {}, to {}", ehrFile.getName(), renamedFile.getAbsolutePath());
				ehrFile.renameTo(renamedFile);
			} else if (echoFileName.equals(ehrFile.getName())) {
				renamedFile = new File(ehrMWStoragePath + requestId + 
						ImageConstants.IMAGE_PLACEHOLDER + "_" + ECHO_DICOM_FILE_TYPE);
				logger.debug("Renaming file {}, to {}", ehrFile.getName(), renamedFile.getAbsolutePath());
				ehrFile.renameTo(renamedFile);
			} else {
				// only files that do not contain images or binary data 
				// must be added to the reducedResponse and then sent
				// to the IHS
				reducedResponse.addResponseFile(ehrFile);
			}
		}
		
		return reducedResponse;
	}

}