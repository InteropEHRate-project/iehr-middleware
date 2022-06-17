package eu.interopehrate.r2d.ehr.workflow;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.image.DicomAnonymizer;
import eu.interopehrate.r2d.ehr.image.DicomUtilities;
import eu.interopehrate.r2d.ehr.image.ImageConstants;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.zip.ZipUtilities;

public class AnonymizeImageWork implements Work {
	private static final String ANONYMIZED_FILE_SUFFIX = "_anon";
	
	private final Logger logger = LoggerFactory.getLogger(AnonymizeImageWork.class);	

	@Override
	public WorkReport execute(WorkContext workContext) {
		EHRRequest request = (EHRRequest) workContext.get(EHRRequestProcessor.REQUEST_KEY);
		
		String r2dPath = Configuration.getR2DADBPath();		
		String ehrMwPath = Configuration.getDBPath();
		
		DicomAnonymizer dicomAnonymizer = new DicomAnonymizer();
		
		// #1 recupera i file con nome <req_id>_imagePlaceholder*
		File[] b64ImageFiles = getImageFiles(new File(ehrMwPath), request.getR2dRequestId());		
		if (b64ImageFiles.length == 0) {
			logger.info("Starting anonymization of {} files", b64ImageFiles.length);
			return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
		}

		logger.info("Starting anonymization of {} files", b64ImageFiles.length);
		// #2 Per ogni file 
		File binImageFile;
		for (File b64ImageFile : b64ImageFiles) {
			try {
				logger.info("Anonymizing file: {}", b64ImageFile.getName());
				
				// #2.1 convert b64 input file to binary files
				binImageFile = new File(r2dPath + b64ImageFile.getName());
				try (Base64InputStream b64is = new Base64InputStream(new FileInputStream(b64ImageFile));
						OutputStream os = new BufferedOutputStream(new FileOutputStream(binImageFile))) {
					IOUtils.copy(b64is, os);
				}

				// #2.2 check if the binari file is a single image or is a zip file
				if (ZipUtilities.isZipFile(binImageFile)) {
					// #2.2.1 unzip the file
					logger.info("Unzipping folder: {}", binImageFile.getName());
					File uncompressedFolder = new File(binImageFile.getAbsolutePath() + "_unzipped");
					uncompressedFolder.mkdirs();
					ZipUtilities.uncompress(binImageFile, uncompressedFolder);
					
					// #2.2.2 anonymize files in the unzipped folder
					if (logger.isDebugEnabled())
						logger.debug("Anonymizing folder: {}", binImageFile.getName());
					dicomAnonymizer.anonymizeFolder(uncompressedFolder);
					
					// #2.2.3 compress anonymized folder
					File compressedAnonymizedFile = new File(r2dPath + binImageFile.getName() + ANONYMIZED_FILE_SUFFIX);
					if (logger.isDebugEnabled())
						logger.debug("Zipping anonymized folder to file: {}", binImageFile.getName(), compressedAnonymizedFile.getName());
					ZipUtilities.compress(uncompressedFolder, compressedAnonymizedFile);
					
					// #2.2.4 deletes the unzipped folder
					FileUtils.deleteDirectory(uncompressedFolder);
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Anonymizing single image: {}", binImageFile.getName());
					// #2.2.1 anonymize the single image e stores it in the R2DA DB path
					dicomAnonymizer.anonymizeDicomImage(
							binImageFile, 
							new File(r2dPath + binImageFile.getName() + ANONYMIZED_FILE_SUFFIX));
				}
				
				// #2.3 deletes the b64 file used as initial input
				// this file is replaced by the binary version created at 2.1
				FileUtils.deleteQuietly(b64ImageFile);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				workContext.put(EHRRequestProcessor.ERROR_MESSAGE_KEY, e.getMessage());
				return new DefaultWorkReport(WorkStatus.FAILED, workContext);
			}
		}
		
		return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
	}

	
	File[] getImageFiles(File folder, String requestId) {
		return folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				
				if (file.getName().startsWith(requestId + ImageConstants.IMAGE_PLACEHOLDER) )
					return true;
				
				return false;
			}
		});	
	}
	
}
