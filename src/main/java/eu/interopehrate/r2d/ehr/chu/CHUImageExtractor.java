package eu.interopehrate.r2d.ehr.chu;

import java.io.File;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.image.ImageExtractor;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;

public class CHUImageExtractor implements ImageExtractor {

	@Override
	public EHRFileResponse extractImageFromFiles(String requestId, EHRFileResponse fileResponse) throws Exception {
		// Retrieves storage path from config file
		String ehrMWStoragePath = Configuration.getDBPath();
		
		// Retrieves file extension
		String fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
		
		EHRFileResponse reducedResponse = new EHRFileResponse();
		reducedResponse.setContentType(fileResponse.getContentType());
		reducedResponse.setStatus(fileResponse.getStatus());
		
		File fileToReduce, reducedFile;
		String newFileName;
		for (int i = 0; i < fileResponse.getResponseFileSize(); i++) {
			fileToReduce = fileResponse.getResponseFile(i);
			
			newFileName = fileToReduce.getName().replace(fileExtension, "_reduced");
			reducedFile = new File(ehrMWStoragePath + newFileName + fileExtension);
			fileToReduce.renameTo(reducedFile);

		}
		
		return reducedResponse;
	}

}