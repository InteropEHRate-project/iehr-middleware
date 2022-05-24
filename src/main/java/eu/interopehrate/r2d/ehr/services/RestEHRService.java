package eu.interopehrate.r2d.ehr.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestEHRService implements EHRService {
	private final OkHttpClient client;
	private static final Logger logger = LoggerFactory.getLogger(RestEHRService.class);
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd");
	private final static String PATIENT_ID_PLACEHOLDER = "$patientId$";
	private final static String ENCOUNTER_ID_PLACEHOLDER = "$encounterId$";
	
	private String storagePath;
	private String fileExtension;
	private String ehrName;

	public RestEHRService() {
		// Checks for proxy settings
		Proxy proxy = Proxy.NO_PROXY;
		String proxyEndpoint = Configuration.getProperty("ehr.proxy.endpoint");
		String proxyPort = Configuration.getProperty("ehr.proxy.port");
		if (proxyEndpoint != null && proxyEndpoint.trim().length() > 0) {
			proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyEndpoint, Integer.valueOf(proxyPort)));
		}
		
		// Retrieves storage path from config file
		storagePath = Configuration.getDBPath();
		if (!storagePath.endsWith("/"))
			storagePath += "/";
		
		// Retrieves file extension
		fileExtension = Configuration.getProperty("ehr.fileExtension");
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
		
		// Retrieves ehr name
		ehrName = Configuration.getProperty("ehr.name");
		
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
		Response response = executeBasicGET(serviceURL.toString());
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT);
		
		//
		// checks for status code and return value
		//
		if (response.isSuccessful()) {
			if (response.body().contentLength() > 0) {
				ehrResponse.setResponse(response.body().string());
					
				return ehrResponse;	
			} else {
				ehrResponse.setStatus(EHRResponseStatus.FAILED);
				String msg = String.format("No patient found in the EHR for citizen: %s %s, %s", 
						theCitizen.getFirstName(), theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
				logger.error(msg);
				ehrResponse.setMessage(msg);				
			}			
		} else if (response.code() == HttpStatus.SC_NOT_FOUND) {
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
			String msg = String.format("No patient found in the EHR for citizen: %s %s, %s", 
					theCitizen.getFirstName(), theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
			logger.error(msg);
			ehrResponse.setMessage(msg);
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR: %s", 
					response.code(), serviceURL.toString(), response.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}
		
		return ehrResponse;
	}

	
	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_PATIENT_SUMMARY_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation getPatientSummary not supported by EHR.");
		
		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		URL serviceURL = createServiceURL(GET_PATIENT_SUMMARY_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURL.toString(), theRequestId);
	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
			String ehrPatientId) throws Exception {		
		// #1 builds service URL
		String servicePath = Configuration.getProperty(SEARCH_ENCOUNTER_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation searchEncounter not supported by EHR.");

		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		if (theFromDate != null) {
			servicePath = servicePath + String.format("&from=%s", dateFormatter.format(theFromDate));	
		}
		URL serviceURL = createServiceURL(GET_PATIENT_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURL.toString(), theRequestId);
	}
	

	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_ENCOUNTER_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation getEncounterEverything not supported by EHR.");

		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		servicePath = servicePath.replace(ENCOUNTER_ID_PLACEHOLDER, theEncounterId);
		URL serviceURL = createServiceURL(GET_PATIENT_SERVICE_NAME, servicePath);
		
		return executeGET(serviceURL.toString(), theRequestId);
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
	private Response executeBasicGET(String URL) throws Exception {
		// #2 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(URL.toString())
                .get()
                .build();
		
		// #3 execute GET
		try {
			if (logger.isDebugEnabled())
				logger.debug("Invoking service of {}-EHR: {}", ehrName, URL.toString());
			return client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of EHR", ioe.getMessage()));
			throw ioe;
		}	
	}
	
	
	/**
	 * Execute the GET operation
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
	 */
	private EHRResponse executeGET(String URL, String theRequestId) throws Exception {
		// #2 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(URL.toString())
                .get()
                .build();
		
		// #3 execute GET
		Response httpResponse = null;
		try {
			if (logger.isDebugEnabled())
				logger.debug("Invoking service of {}-EHR: {}", ehrName, URL.toString());
			httpResponse = client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of EHR", ioe.getMessage()));
			throw ioe;
		}
		
		// #4 Checks the response
		if (httpResponse.isSuccessful()) {
			// Copy results to file
			final File out = new File(storagePath + theRequestId + fileExtension);
			try (InputStream inStream = httpResponse.body().byteStream();
				OutputStream outStream = new BufferedOutputStream(new FileOutputStream(out));) {
				IOUtils.copy(inStream, outStream);				
			}
			
			// create response 
			final EHRResponse ehrResponse = new EHRResponse();
			ContentType mime = ContentType.getContentType(Configuration.getProperty("ehr.mime"));
			ehrResponse.setContentType(mime);
			ehrResponse.setOnFile(true);
			ehrResponse.setResponse(theRequestId + fileExtension);
			ehrResponse.setStatus(EHRResponseStatus.COMPLETED);
			logger.debug("Saved to file '{}' {} Kb received from EHR", 
					ehrResponse.getResponse(),
					NumberFormat.getInstance().format(out.length() / 1024D));
			return ehrResponse;
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR: %s", 
	    			httpResponse.code(), URL.toString(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}
	}
	
	
	/**
	 * Execute the GET operation
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
	@SuppressWarnings("unused")
	private EHRResponse oldexecuteGET(String URL, String theRequestId) throws Exception {
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
			if (httpResponse.body().contentLength() > 0) {
				ehrResponse.setResponse(httpResponse.body().string());
			} else
				ehrResponse.setResponse("");
				
			return ehrResponse;
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR: %s", 
	    			httpResponse.code(), URL.toString(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}		
	}
		 */

}
