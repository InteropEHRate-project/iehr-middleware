package eu.interopehrate.r2d.ehr.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;

public class RestEHRService implements EHRService {
	private static final Logger logger = LoggerFactory.getLogger(RestEHRService.class);
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd");
	private final static String PATIENT_ID_PLACEHOLDER = "$patientId$";
	private final static String ENCOUNTER_ID_PLACEHOLDER = "$encounterId$";
	
	private String storagePath;
	private String fileExtension;
	@SuppressWarnings("unused")
	private String ehrName;
	
	@Autowired
	private HttpInvoker httpInvoker;

	public RestEHRService() {		
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
	}
	
	
	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_PATIENT_SERVICE_NAME + ".PATH");
		// #2 placeholders replacement
		servicePath = servicePath.replace("$firstName$", theCitizen.getFirstName());
		servicePath = servicePath.replace("$familyName$", theCitizen.getFamilyName());
		servicePath = servicePath.replace("$dateOfBirth$", theCitizen.getDateOfBirth());
		URI serviceURI = createServiceURI(GET_PATIENT_SERVICE_NAME, servicePath);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int retCode = httpInvoker.executeGet(serviceURI, out, new HeaderParam[0]);
		
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT_PLAIN);
		if (retCode == HttpStatus.SC_OK) {
			String patientId = out.toString();
			if (patientId != null && patientId.trim().length() > 0) {
				ehrResponse.setResponse(patientId);
					
				return ehrResponse;	
			} else {
				ehrResponse.setStatus(EHRResponseStatus.FAILED);
				String msg = String.format("Empty Body: no patient found in the EHR for citizen: firstName:%s, "
						+ "familyName: %s, dateOfBirth: %s", theCitizen.getFirstName(), 
						theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
				logger.error(msg);
				ehrResponse.setMessage(msg);				
			}		} else if (retCode == HttpStatus.SC_NOT_FOUND) {
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
			String msg = String.format("404 NOT FOUND: no patient found in the EHR for citizen: firstName:%s, "
					+ "familyName: %s, dateOfBirth: %s", theCitizen.getFirstName(), 
					theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
			logger.error(msg);
			ehrResponse.setMessage(msg);			
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR!", 
					retCode, serviceURI.toString());

			logger.error(errMsg);
			throw new IOException(errMsg);			
		}
		
		
		/*
		// #3 invokes service
		Response response = executeBasicGET(serviceURL.toString());
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT);
		
		//
		// checks for status code and return value
		//
		if (response.isSuccessful()) {
			if (response.body() != null) {
				ehrResponse.setResponse(response.body().string());
					
				return ehrResponse;	
			} else {
				ehrResponse.setStatus(EHRResponseStatus.FAILED);
				String msg = String.format("Empty Body: no patient found in the EHR for citizen: firstName:%s, "
						+ "familyName: %s, dateOfBirth: %s", theCitizen.getFirstName(), 
						theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
				logger.error(msg);
				ehrResponse.setMessage(msg);				
			}			
		} else if (response.code() == HttpStatus.SC_NOT_FOUND) {
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
			String msg = String.format("404 NOT FOUND: no patient found in the EHR for citizen: firstName:%s, "
					+ "familyName: %s, dateOfBirth: %s", theCitizen.getFirstName(), 
					theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
			logger.error(msg);
			ehrResponse.setMessage(msg);
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR: %s", 
					response.code(), serviceURL.toString(), response.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}
		
		response.close();
		*/
		
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
		URI serviceURI = createServiceURI(GET_PATIENT_SUMMARY_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURI, theRequestId);
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
		URI serviceURI = createServiceURI(GET_PATIENT_SERVICE_NAME, servicePath);
		
		// #3 invokes service
		return executeGET(serviceURI, theRequestId);
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
		URI serviceURI = createServiceURI(GET_PATIENT_SERVICE_NAME, servicePath);
		
		return executeGET(serviceURI, theRequestId);
	}
	
	/**
	 * Builds the serviceURL reading the configuration file
	 * 
	 * @param serviceName
	 * @param servicePath
	 * @return
	 * @throws NumberFormatException
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 */
	private URI createServiceURI(String serviceName, String servicePath) throws NumberFormatException, 
															MalformedURLException, URISyntaxException {
		String ehrProtocol = Configuration.getProperty("ehr.protocol");
		String ehrHost = Configuration.getProperty("ehr.host");
		String ehrPort = Configuration.getProperty("ehr.port");
		
		String servicePort = Configuration.getProperty(serviceName + ".PORT");
		if (servicePort != null && servicePort.trim().length() > 0)
			ehrPort = servicePort;
		
		URL url = new URL(ehrProtocol, ehrHost, new Integer(ehrPort), servicePath);
		return url.toURI();
	}

	
	/**
	 * Execute the GET operation
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
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
	 */
	
	
	/**
	 * Execute the GET operation
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
	 */
	private EHRResponse executeGET(URI uri, String theRequestId) throws Exception {
		// #1 creates output file
		final File out = new File(storagePath + theRequestId + fileExtension);
		// #2 executes the GET
		int retCode = httpInvoker.executeGet(uri, out, new HeaderParam[0]);
		final EHRResponse ehrResponse = new EHRResponse();
		if (retCode == HttpStatus.SC_OK) {
			ContentType mime = ContentType.parse(Configuration.getProperty("ehr.mime"));
			ehrResponse.setContentType(mime);
			ehrResponse.setOnFile(true);
			ehrResponse.setResponse(theRequestId + fileExtension);
			ehrResponse.setStatus(EHRResponseStatus.COMPLETED);
		} else if (retCode == HttpStatus.SC_NOT_FOUND){
			logger.warn("The current request received a 404-NOT FOUND from EHR!");
			ehrResponse.setMessage("No data found on EHR.");
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
		} else {
			String errorMsg = String.format("The current request received the following error code from EHR: %d", retCode);
			logger.error(errorMsg);
			ehrResponse.setMessage(errorMsg);
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
		}
		
		return ehrResponse;
		
		
		
		/*
		Request getRequest = new Request.Builder()
                .url(URL.toString())
                .get()
                .build();
		
		// #3 execute GET
		try (Response httpResponse = client.newCall(getRequest).execute();) {
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
			} else if (httpResponse.code() == HttpStatus.SC_NOT_FOUND) {
				logger.warn("The current request received a 404 from EHR!");
				// create response 
				final EHRResponse ehrResponse = new EHRResponse();
				ehrResponse.setMessage("No data found on EHR.");
				ehrResponse.setStatus(EHRResponseStatus.FAILED);
				return ehrResponse;
			} else {
				String errMsg = String.format("Error %d while invoking service %s of EHR: %s", 
		    			httpResponse.code(), URL.toString(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}			
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of EHR", ioe.getMessage()));
			throw ioe;
		}
		*/
		
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
