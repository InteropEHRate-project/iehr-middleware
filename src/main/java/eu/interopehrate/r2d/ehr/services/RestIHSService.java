package eu.interopehrate.r2d.ehr.services;

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
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;

public class RestIHSService implements IHSService {

	private static final String FILE_EXT = ".json";
	private final Logger logger = LoggerFactory.getLogger(RestIHSService.class);
	private String storagePath;
	
	@Autowired
	private HttpInvoker httpInvoker;

	public RestIHSService() {
		super();
		// Retrieves storage path from config file
		storagePath = eu.interopehrate.r2d.ehr.Configuration.getDBPath();
		if (!storagePath.endsWith("/"))
			storagePath += "/";
	}

	@Override
	public void requestConversion(EHRRequest ehrRequest, EHRResponse ehrResponse) throws Exception {
		if (!ehrResponse.isOnFile() && ehrResponse.getResponse().isEmpty()) {
			throw new IllegalStateException("Response retrieved from EHR not found on file as expected!");
		}
		
		// #1 builds CDA filename, it contains the CDA retrieved by EHR
		final String ehrFileName = storagePath + ehrResponse.getResponse();
		
		// #2 Creates the URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/requestConversion?resourceName=");

		// #3 Customize the URL depending on the type of operation requested
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			serviceURL.append("Encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			serviceURL.append(buildParamString(ehrFileName));
		} else
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		
		// #4 Invokes the HTTP service
		try {
			int returnCode = httpInvoker.executePost(
					new URI(serviceURL.toString()), 
					new File(ehrFileName),
					Configuration.getProperty("ehr.mime"));
			
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

	
	@Override
	public EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest) throws Exception {
		// #1 Creates the base URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/retrieveFHIRHealthRecord?");
		// #2 starts adding URL parameters
		
		// #2.1 handles 'lang' parameter
		if (ehrRequest.getPreferredLanguage() != null) 
			serviceURL.append("lang=").append(ehrRequest.getPreferredLanguage()).append("&");
		
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
			final File out = new File(Configuration.getR2DADBPath() + ehrRequest.getR2dRequestId() + FILE_EXT);
			int returnCode = httpInvoker.executeGet(
					new URI(serviceURL.toString()), 
					out,
					new HeaderParam("Accept", ContentType.APPLICATION_JSON.toString()));
			
			final EHRResponse ihsResponse = new EHRResponse(ContentType.APPLICATION_JSON);
			if (returnCode == HttpStatus.SC_OK) {
				// create response 
				ihsResponse.setOnFile(true);
				ihsResponse.setResponse(ehrRequest.getR2dRequestId() + FILE_EXT);
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
	 * Builds the param string needed when invoking the service for converting results. 
	 * This parameter is built searching specific string(s) in the CDA file containing the
	 * CDA Document to sent to IHS.
	 * 
	 * The list of codes to be searched is stored in the configuration file
	 */
	private String buildParamString(String cdaFileName) throws Exception {
		StringTokenizer codes = new StringTokenizer(Configuration.getProperty("ihs.mapping.codes"), ";");
		StringBuffer paramString = new StringBuffer();		
		Process process;
		String currentCode;
		boolean isFirst = true;

		while (codes.hasNext()) {
			currentCode = codes.next();
			process = Runtime.getRuntime().exec(String.format("grep -i %s %s", currentCode, cdaFileName));
			if (process.waitFor() == 0) {
				if (!isFirst)
					paramString.append(";");
				paramString.append(Configuration.getProperty("ihs.mapping.code." + currentCode));
				isFirst = false;
			}
		}
		
		return paramString.toString();
	}
	
}
