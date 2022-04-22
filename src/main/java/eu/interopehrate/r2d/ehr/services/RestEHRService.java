package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.text.SimpleDateFormat;
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
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd");
	private final static String PATIENT_ID_PLACEHOLDER = "$patientId$";
	private final static String ENCOUNTER_ID_PLACEHOLDER = "$encounterId$";

	public RestEHRService() {
		// Checks for proxy settings
		Proxy proxy = Proxy.NO_PROXY;
		String proxyEndpoint = Configuration.getProperty("ehr.proxy.endpoint");
		String proxyPort = Configuration.getProperty("ehr.proxy.port");
		
		if (proxyEndpoint != null && proxyEndpoint.trim().length() > 0) {
			proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyEndpoint, Integer.valueOf(proxyPort)));
		}
		
		// Creates the client
		Integer timeOutInMinutes = Integer.valueOf(Configuration.getProperty("ehr.timeoutInMinutes"));
		client = new OkHttpClient.Builder()
			      .readTimeout(timeOutInMinutes, TimeUnit.MINUTES)
			      .writeTimeout(timeOutInMinutes, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .proxy(proxy)
			      .build();
	}
	
	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_PATIENT_SERVICE_NAME + ".PATH");
		// #2 placeholders replacement
		servicePath = servicePath.replace("$firstName$", theCitizen.getFirstName());
		servicePath = servicePath.replace("$familyName$", theCitizen.getFamilyName());
		servicePath = servicePath.replace("$dateOfBirth$", theCitizen.getDateOfBirth());
		URL serviceURL = createServiceURL(GET_PATIENT_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURL.toString());
		
		/*
		// #1 builds service URL
		StringBuilder serviceURL = new StringBuilder();
		serviceURL.append(Configuration.getProperty(Configuration.EHR_ENDPOINT));
		serviceURL.append("/citizen/$authorize");
		
		// #2 Creates the body of the POST request
		RequestBody body = new FormBody.Builder()
				.add()
				.add("FamilyName", theCitizen.getFamilyName())
				.add("DateOfBirth", theCitizen.getDateOfBirth())
				.add("PersonIdentifier", theCitizen.getPersonIdentifier())
				.build();
		
		// #3 Creates the GET request
		Request getRequest = new Request.Builder()
                .url(serviceURL.toString())
                .get()
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
		*/
	}

	
	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_PATIENT_SUMMARY_SERVICE_NAME + ".PATH");
		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		URL serviceURL = createServiceURL(GET_PATIENT_SUMMARY_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURL.toString());

	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
			String ehrPatientId) throws Exception {		
		// #1 builds service URL
		String servicePath = Configuration.getProperty(SEARCH_ENCOUNTER_SERVICE_NAME + ".PATH");
		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		if (theFromDate != null) {
			servicePath = servicePath + String.format("&from=%s", dateFormatter.format(theFromDate));	
		}
		URL serviceURL = createServiceURL(GET_PATIENT_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURL.toString());
	}
	

	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_ENCOUNTER_SERVICE_NAME + ".PATH");
		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		servicePath = servicePath.replace(ENCOUNTER_ID_PLACEHOLDER, theEncounterId);
		URL serviceURL = createServiceURL(GET_PATIENT_SERVICE_NAME, servicePath);
		
		return executeGET(serviceURL.toString());
	}
	
	/**
	 * Builds the serviceURL reading the configuration file
	 * 
	 * @param serviceName
	 * @param servicePath
	 * @return
	 * @throws NumberFormatException
	 * @throws MalformedURLException
	 */
	private URL createServiceURL(String serviceName, String servicePath) throws NumberFormatException, MalformedURLException {
		String ehrProtocol = Configuration.getProperty("ehr.protocol");
		String ehrHost = Configuration.getProperty("ehr.host");
		String ehrPort = Configuration.getProperty("ehr.port");
		
		String servicePort = Configuration.getProperty(serviceName + ".PORT");
		if (servicePort != null && servicePort.trim().length() > 0)
			ehrPort = servicePort;
		
		return new URL(ehrProtocol, ehrHost, new Integer(ehrPort), servicePath);
	}
	
	/**
	 * Execute the GET operation
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
	 */
	private EHRResponse executeGET(String URL) throws Exception {
		// #2 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(URL.toString())
                .get()
                .build();
		
		// #3 execute GET
		Response httpResponse = null;
		try {
			if (logger.isDebugEnabled())
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
