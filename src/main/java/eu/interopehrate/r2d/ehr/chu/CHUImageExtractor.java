package eu.interopehrate.r2d.ehr.chu;

import java.io.File;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.image.ImageExtractor;

public class CHUImageExtractor implements ImageExtractor {

	@Override
	public File createReducedFile(String requestId, String inputFileName) throws Exception {
		// Retrieves storage path from config file
		String ehrMWStoragePath = Configuration.getDBPath();
		
		// Retrieves file extension
		String fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
				
		// String fileToReduceName = requestId + fileExtension;
		// Creates input file
		File fileToReduce = new File(ehrMWStoragePath + inputFileName);
		File reducedFile = new File(ehrMWStoragePath + requestId + "_reduced" + fileExtension);

		fileToReduce.renameTo(reducedFile);
		return reducedFile;
	}

}