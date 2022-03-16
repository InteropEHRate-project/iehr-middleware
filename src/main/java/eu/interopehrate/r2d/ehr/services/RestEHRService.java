package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestEHRService implements EHRService {
	private final OkHttpClient client;
	private static final Log logger = LogFactory.getLog(RestEHRService.class);
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd");

	public RestEHRService() {
		client = new OkHttpClient.Builder()
			      .readTimeout(5, TimeUnit.MINUTES)
			      .writeTimeout(5, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .build();
	}

	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		// #1 builds service URL
		StringBuilder serviceURL = new StringBuilder();
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/citizen/$authorize");
		
		// #2 Creates the body of the POST request
		RequestBody body = new FormBody.Builder()
				.add("FirstName", theCitizen.getFirstName())
				.add("FamilyName", theCitizen.getFamilyName())
				.add("DateOfBirth", theCitizen.getDateOfBirth())
				.add("PersonIdentifier", theCitizen.getPersonIdentifier())
				.build();
		
		// #3 Creates the POST request
		Request postRequest = new Request.Builder()
                .url(serviceURL.toString())
                .post(body)
                .build();
		
		// #4 execute POST
		Response httpResponse = null;
		try {
			logger.debug(String.format("Invoking service of EHR: %s", serviceURL.toString()));
			httpResponse = client.newCall(postRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error '%s' while invoking service of EHR", ioe.getMessage()));
			throw ioe;
		}
		
		// #5 Checks the response
		if (httpResponse.isSuccessful()) {
			EHRResponse ehrResponse = new EHRResponse();
			ehrResponse.setContentType(ContentType.TEXT);
			ehrResponse.setResponse(httpResponse.body().string());
			logger.debug("Response retrieved from EHR: " + ehrResponse.getResponse());
			return ehrResponse;
		} else {
			String errMsg = String.format("Error %d while invoking service of EHR: %s", 
	    			httpResponse.code(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}
	}

	
	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT);
		ehrResponse.setStatus(EHRResponseStatus.FAILED);
		ehrResponse.setMessage("The Patient Summary is not available.");
		
		return ehrResponse;
	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
			String ehrPatientId) throws Exception {		
		// #1 builds service URL
		StringBuilder serviceURL = new StringBuilder();
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/Encounter?citizenId=").append(ehrPatientId);
		if (theFromDate != null) {
			serviceURL.append("&from=").append(dateFormatter.format(theFromDate));
		}
		
		return executeGET(serviceURL.toString());
	}
	

	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
		// builds callback URL
		// #1 builds service URL
		StringBuilder serviceURL = new StringBuilder();
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/Encounter/everything?citizenId=").append(ehrPatientId);
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
			logger.debug(String.format("Invoking service of EHR: %s", URL.toString()));
			httpResponse = client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of EHR", ioe.getMessage()));
			throw ioe;
		}
		
		// #4 Checks the response
		if (httpResponse.isSuccessful()) {
			EHRResponse ehrResponse = new EHRResponse();
			ehrResponse.setContentType(ContentType.XML_CDA);
			ehrResponse.setResponse(httpResponse.body().string());
			return ehrResponse;
		} else {
			String errMsg = String.format("Error %d while invoking service of EHR: %s", 
	    			httpResponse.code(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}		
	}
	
	
}
