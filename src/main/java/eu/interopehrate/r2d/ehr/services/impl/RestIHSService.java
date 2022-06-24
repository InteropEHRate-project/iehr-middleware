package eu.interopehrate.r2d.ehr.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;

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
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.HeaderParam;
import eu.interopehrate.r2d.ehr.services.HttpInvoker;
import eu.interopehrate.r2d.ehr.services.IHSService;

public class RestIHSService implements IHSService {

	private static final String FILE_EXT = ".json";
	private final Logger logger = LoggerFactory.getLogger(RestIHSService.class);
	private String storagePath;
	
	@Autowired
	private HttpInvoker httpInvoker;

	public RestIHSService() {
		// Retrieves storage path from config file
		storagePath = Configuration.getR2DADBPath();
	}

	@Override
	public void requestConversion(EHRRequest ehrRequest, EHRFileResponse ehrResponse) throws Exception {
		if (ehrResponse.getResponseFileSize() <= 0) {
			throw new IllegalStateException("Response retrieved from EHR not found on file as expected!");
		}
		
		// #1 Creates the base URL for sending conversion requests to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = null;
		for (int i = 0; i < ehrResponse.getResponseFileSize(); i++) {
			
			// #2 Customize the URL for the current conversion requests
			serviceURL = new StringBuilder(ihsBase);
			serviceURL.append("/requestConversion?resourceName=");
			serviceURL.append(detectConversionTarget(ehrRequest, ehrResponse.getResponseFile(i)));
			// #3 Invokes IHS service for requesting conversion
			try {
				logger.debug("Requesting to IHS conversion of file {}", ehrResponse.getFirstResponseFile().getName());
				int returnCode = httpInvoker.executePost(
						new URI(serviceURL.toString()), 
						ehrResponse.getFirstResponseFile(),
						Configuration.getProperty(Configuration.EHR_MIME));
				
				if (returnCode != HttpStatus.SC_OK) {
					String errMsg = String.format("Error %d while invoking service of IHS", returnCode);
					logger.error(errMsg);
					throw new IOException(errMsg);				
				} 
			} catch (IOException ioe) {
				logger.error("Error '{}' while invoking requestConversion of IHS", ioe.getMessage());
				throw ioe;			
			}			
		}
	}

	
	@Override
	public EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest) throws Exception {
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
			serviceURL.append(String.format("encounter/%s/everything", 
					ehrRequest.getParameter(EHRRequest.PARAM_RESOURCE_ID)));
		} else {
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		}
		
		// #3 Executes the request
		try {
			final File out = new File(storagePath + ehrRequest.getR2dRequestId() + FILE_EXT);
			int returnCode = httpInvoker.executeGet(
					new URI(serviceURL.toString()), 
					out,
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
	 * Determins what parameters must be sento to IHS to request the conversion 
	 * of a file
	 */
	private String detectConversionTarget(EHRRequest ehrRequest, File ehrFile) throws Exception {
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			return "Encounter";
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			return detectConversionTargetForEncounterEverything(ehrRequest, ehrFile);
		} else
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
	}
	
	/*
	 * Extracts from the name of the file the type of data it contains.
	 * The list of codes to be searched is stored in the configuration file
	 */
	protected String detectConversionTargetForEncounterEverything(EHRRequest ehrRequest, 
			File ehrFile) throws Exception {
		final StringTokenizer codes = 
				new StringTokenizer(Configuration.getProperty(Configuration.IHS_MAPPING_CODES), ";");
		final String fileName = ehrFile.getName();
		String currentCode;
		while (codes.hasNext()) {
			currentCode = codes.next();
			if (fileName.endsWith("_" + currentCode + FILE_EXT))
				return currentCode;
			
			if (fileName.endsWith("_" + currentCode + "_reduced" + FILE_EXT))
				return currentCode;
		}
		
		throw new IllegalArgumentException("Error during detection of conversion target for file: " + fileName);
	}
	
	
}
