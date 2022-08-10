package eu.interopehrate.r2d.ehr.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.StringTokenizer;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.HeaderParam;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.HttpInvoker;
import eu.interopehrate.r2d.ehr.services.IHSService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Default implementation of a proxy invoking the services
 * of the IHS service to request conversion of data.
 * 
 * The IHS service defines a protocol with two services, where 
 * the first service can be invoked several times requesting conversion
 * to specific data. The second method can be invoked once and is used
 * to retrieve all previously converted data into a Bundle.
 * 
 * Needed parameters to invoke the IHS service are configured in the 
 * application.properties file, using a set of properties starting with
 * ihs.
 * 
 */

public class RestIHSService implements IHSService {

	protected static final String FHIR_FILE_EXT = ".json";
	protected final Logger logger = LoggerFactory.getLogger(RestIHSService.class);
	protected String storagePath;
	
	@Autowired
	protected HttpInvoker httpInvoker;

	public RestIHSService() {
		// Retrieves storage path from config file
		storagePath = Configuration.getR2DADBPath();
	}
	

	@Override
	public void startTransaction() throws Exception {
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/cleanEB");
		
		try {
			int returnCode = httpInvoker.executeGet(
					new URI(serviceURL.toString().trim()), 
					(HeaderParam[])null);
			
			if (returnCode != HttpStatus.SC_OK) {
				String errMsg = String.format("Error %d while invoking service of IHS", returnCode);
				logger.error(errMsg);
				throw new IOException(errMsg);				
			}
		} catch (IOException ioe) {
			logger.error("Error '{}' while invoking the delete of the cache to IHS", ioe.getMessage());
			throw ioe;			
		} 
	}


	@Override
	public void requestConversion(EHRRequest ehrRequest, 
			EHRFileResponse ehrResponse, String patientId) throws Exception {
		if (ehrResponse.getResponseFileSize() <= 0) {
			throw new IllegalStateException("Response retrieved from EHR not found on file as expected!");
		}
		
		// #1 Creates the base URL for sending conversion requests to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		String mimeType;
		StringBuilder serviceURL = null;
		for (int i = 0; i < ehrResponse.getResponseFileSize(); i++) {
			
			// #2 Customize the URL for the current conversion requests
			serviceURL = new StringBuilder(ihsBase);
			serviceURL.append("/requestConversion?resourceName=");
			serviceURL.append(detectResourceName(ehrRequest, ehrResponse.getResponseFile(i)));
			// #3 Invokes IHS service for requesting conversion
			try {
				// detects mime type of the request
				mimeType = getMimeTypeForFile(ehrRequest, ehrResponse.getResponseFile(i));
				
				if (logger.isDebugEnabled())
					logger.debug("Requesting to IHS conversion of file {} with mime {}",
							ehrResponse.getResponseFile(i).getName(), mimeType);
				
				// invokes http rest service
				int returnCode = httpInvoker.executePost(
						new URI(serviceURL.toString().trim()), 
						ehrResponse.getResponseFile(i),
						mimeType);
				
				if (returnCode != HttpStatus.SC_OK) {
					String errMsg = String.format("Error %d while invoking service of IHS", returnCode);
					logger.error(errMsg);
					throw new IOException(errMsg);				
				} 
				
				// this sleep is needed because sometimes happens that the 
				// retrieveFHIRHealthRecord starts when some operations of
				// requestConversion method are still executing. 
				// Probably the IHS-DB uses some async operations
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e1) {
					logger.error("Error during Thread.sleep()");				
				}
				
			} catch (IOException ioe) {
				logger.error("Error '{}' while invoking requestConversion of IHS", ioe.getMessage());
				throw ioe;			
			}			
		}
	}

	
	@Override
	public EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest, 
			String patientId) throws Exception {
		// #1 Creates the base URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/retrieveFHIRHealthRecord?");
		// #2 starts adding URL parameters
		
		// #2.1 handles 'lang' parameter
		//if (ehrRequest.getPreferredLanguage() != null) 
		//	serviceURL.append("lang=").append(ehrRequest.getPreferredLanguage()).append("&");
		
		// force to use defaul lang of the R2DServer
		serviceURL.append("lang=").append(Configuration.getProperty(Configuration.IHS_LANGUAGE)).append("&");
		
		// #2.2 handles 'call' parameter
		serviceURL.append("call=");
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			serviceURL.append("encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			String encounterID = (String) ehrRequest.getParameter(EHRRequest.PARAM_RESOURCE_ID);
			encounterID = URLEncoder.encode(encounterID, StandardCharsets.UTF_8.toString());
			serviceURL.append(String.format("encounter/%s/everything", encounterID));
		} else {
			serviceURL.append(String.format("patient-summary/%s", patientId));
		}
		
		// #3 Executes the request
		try {
			final File out = new File(storagePath + ehrRequest.getR2dRequestId() + FHIR_FILE_EXT);
			int returnCode = httpInvoker.executeGet(
					new URI(serviceURL.toString().trim()), out,
					new HeaderParam("Accept", ContentType.APPLICATION_JSON.toString()));
			
			final EHRFileResponse ihsResponse = new EHRFileResponse();
			ihsResponse.setContentType(ContentType.APPLICATION_JSON);
			if (returnCode == HttpStatus.SC_OK) {
				// create response 
				ihsResponse.addResponseFile(out);
				ihsResponse.setStatus(EHRResponseStatus.COMPLETED);
			} else {
				ihsResponse.setStatus(EHRResponseStatus.FAILED);
				ihsResponse.setMessage(String.format("Error %d while retrieving results from IHS service", returnCode));
				logger.error(ihsResponse.getMessage());			
			} 
			return ihsResponse;
		} catch (IOException ioe) {
			logger.error("Error '{}' while invoking requestConversion of IHS", ioe.getMessage());
			throw ioe;			
		}
	}
	
	/*
	 * Determines what parameters must be sent to IHS to request the conversion 
	 * of a file
	 */
	private String detectResourceName(EHRRequest ehrRequest, File ehrFile) throws Exception {
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			return "Encounter";
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			return detectResourceNameForEncounterEverything(ehrRequest, ehrFile);
		} else if (ehrRequest.getOperation() == R2DOperation.PATIENT_SUMMARY) {
			return detectResourceNameForPatientSummary(ehrRequest, ehrFile);
		} else
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
	}

	/*
	 * Returns the list of parameters neede to convert the patient summary.
	 */
	protected String detectResourceNameForPatientSummary(EHRRequest ehrRequest, File ehrFile) {
		return Configuration.getProperty("ihs.mapping.code.patientSummary");
	}

	/*
	 * Extracts from the name of the file the type of data it contains.
	 * The list of codes to be searched is stored in the configuration file
	 * 
	 * File name convention: <request-id>_<type>.<extension>
	 * 
	 * File name sample: 
	 * 			1f218409-f047-4a09-92ec-ed4982ea29f6_35.chu
	 */
	protected String detectResourceNameForEncounterEverything(
			EHRRequest ehrRequest, File ehrFile) throws Exception {
		// #1 retrieves the list of file types
		final StringTokenizer fileTypes = new StringTokenizer(
				Configuration.getProperty(Configuration.IHS_MAPPING_CODES), ";");
		
		// #2 retrieves the file extension of file downloaded from EHR
		String ehrFileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!ehrFileExtension.startsWith("."))
			ehrFileExtension = "." + ehrFileExtension;
		
		// #3 checks the type of the file from the file name
		final String fileName = ehrFile.getName();
		String currentCode;
		while (fileTypes.hasNext()) {
			currentCode = fileTypes.next();
			if (fileName.endsWith("_" + currentCode + ehrFileExtension)) {
				return Configuration.getProperty("ihs.mapping.code." + currentCode);
			}
		}
		
		throw new IllegalArgumentException("Error during detection of conversion target for file: " + fileName);
	}
	
	/**
	 * 
	 * @param ehrRequest
	 * @param ehrFile
	 * @return
	 * @throws Exception
	 */
	protected String getMimeTypeForFile(EHRRequest ehrRequest, File ehrFile) throws Exception {
		String mime = null;
		
		if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			String ehrFileExt = Configuration.getProperty(Configuration.EHR_FILE_EXT);
			if (!ehrFileExt.startsWith("."))
				ehrFileExt = "." + ehrFileExt;
			
			final String fileName = ehrFile.getName();
			int start = fileName.lastIndexOf("_");
			int end = fileName.indexOf(ehrFileExt);
			String type = fileName.substring(start + 1, end);
			mime = Configuration.getProperty("ihs.mapping.code." + type + ".mime");
		} else if (ehrRequest.getOperation() == R2DOperation.PATIENT_SUMMARY) {
			mime = Configuration.getProperty("ihs.mapping.code.patientSummary.mime");
		}
			
		// checks if a mime type has been found, otherwise returns the default
		if (mime != null && !mime.isEmpty())
			return mime;
		else
			return Configuration.getProperty(Configuration.EHR_MIME);

	}
	
}
