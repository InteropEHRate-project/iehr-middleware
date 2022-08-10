package eu.interopehrate.r2d.ehr.cda.image;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.image.DicomAnonymizer;
import eu.interopehrate.r2d.ehr.image.DicomAnonymizerUtilities;
import eu.interopehrate.r2d.ehr.image.ImageConstants;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.zip.ZipUtilities;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: This class implements the DicomAnonymizer interface.
 * 
 * Given an image file, this class is able to determine if it is a single
 * DICOM file (.dcm) or a zipped DICOM archive. In the first case, the single image 
 * is directly anonymized, in the latter case, the zip file must be uncompressed
 * every single image has to be anonymized and then the zip file containing 
 * only anonymized images must be recreated.
 * 
 * Input files for this anonymizer are encoded in B64.
 * 
 */
public class CDADicomAnonymizer implements DicomAnonymizer {
	private final Logger logger = LoggerFactory.getLogger(CDADicomAnonymizer.class);

	@Override
	public void anonymizeDicomImages(EHRRequest ehrRequest) throws Exception {
		// retrieves folder from configuration
		String r2dPath = Configuration.getR2DADBPath();		
		String ehrMwPath = Configuration.getDBPath();
		
		DicomAnonymizerUtilities dicomAnonymizer = new DicomAnonymizerUtilities();
		
		// #1 looks for image files to be anonymized, fila name = <req_id>_imagePlaceholder*
		File[] b64ImageFiles = findRequestImageFiles(new File(ehrMwPath), ehrRequest.getR2dRequestId());		
		if (b64ImageFiles.length == 0) {
			logger.info("No files of request {} need to be anonymized", ehrRequest.getR2dRequestId());
			return;
		}

		boolean anonymizationActive = Boolean.valueOf(Configuration.getProperty(Configuration.EHR_ANONYMIZE_IMAGES));
		logger.info("Starting anonymization of {} files", b64ImageFiles.length);
		// #2 Per ogni file 
		File binImageFile;
		for (File b64ImageFile : b64ImageFiles) {
			try {
				logger.info("Processing file: {}", b64ImageFile.getName());
				
				// #2.1 convert b64 input file to binary files and moves them 
				// from EHR MW folder to R2D folder. This is very important!
				binImageFile = new File(r2dPath + b64ImageFile.getName());
				try (Base64InputStream b64is = new Base64InputStream(new FileInputStream(b64ImageFile));
						OutputStream os = new BufferedOutputStream(new FileOutputStream(binImageFile))) {
					IOUtils.copy(b64is, os);
				}

				if (!anonymizationActive) {
					logger.info("Anonymization is disabled, file is only moved to R2D data folder.");
					continue;
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
					File compressedAnonymizedFile = new File(r2dPath + binImageFile.getName() + DicomAnonymizer.ANONYMIZED_FILE_SUFFIX);
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
							new File(r2dPath + binImageFile.getName() + DicomAnonymizer.ANONYMIZED_FILE_SUFFIX));
				}
				
				// #2.3 deletes the b64 file used as initial input
				// this file is replaced by the binary version created at 2.1
				Files.deleteIfExists(b64ImageFile.toPath());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw e;
			}
		}
	}

	
	File[] findRequestImageFiles(File folder, String requestId) {
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
