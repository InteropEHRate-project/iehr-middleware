package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestEHRService implements EHRService {
	private final OkHttpClient client;
	private static final Log logger = LogFactory.getLog(RestEHRService.class);

	
	public RestEHRService() {
		client = new OkHttpClient.Builder()
			      .readTimeout(2, TimeUnit.MINUTES)
			      .writeTimeout(2, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .build();
	}


	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		return null;
	}

	
	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, Citizen theCitizen) throws Exception {
		return null;
	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
			Citizen theCitizen) throws Exception {
		logger.debug(String.format("Executing Encounter.search..."));
		
		// #1 builds service URL
		StringBuilder serviceURL = new StringBuilder();
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/Encounter?citizenId=").append(theCitizen.getPersonIdentifier());
		
		return executeGET(serviceURL.toString());
	}
	

	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			Citizen theCitizen) throws Exception {
		logger.debug(String.format("Executing Encounter/%s/$everything", theEncounterId));

		// builds callback URL
		StringBuilder serviceURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/Encounter/everything?citizenId=").append(theCitizen.getPersonIdentifier());
		serviceURL.append("&encounterId=").append(theEncounterId);
		
		return executeGET(serviceURL.toString());
	}
	
	
	private EHRResponse executeGET(String URL) throws Exception {
		// #2 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(URL.toString())
                .get()
                .build();
		
		// #3 execute GET
		Response httpResponse = null;
		try {
			logger.debug(String.format("Submitting request to EHR: %s", URL.toString()));
			httpResponse = client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while sending POST request to IHS Server", ioe.getMessage()));
			throw ioe;
		}
		
		// #4 Checks the response
		if (httpResponse.isSuccessful()) {
			EHRResponse ehrResponse = new EHRResponse();
			ehrResponse.setContentType(ContentType.XML_CDA);
			ehrResponse.setResponse(httpResponse.body().string());
			return ehrResponse;
		} else {
			String errMsg = String.format("Error %d while sending request to IHS Server: %s", 
	    			httpResponse.code(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}		
	}
	
	
}
