package eu.interopehrate.r2d.ehr.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.services.EHRService;
import eu.interopehrate.r2d.ehr.services.EncounterEverythingItem;
import eu.interopehrate.r2d.ehr.services.HeaderParam;
import eu.interopehrate.r2d.ehr.services.HttpInvoker;

public class RestEHRService implements EHRService {
	private static final Logger logger = LoggerFactory.getLogger(RestEHRService.class);
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd");
	protected final static String PATIENT_ID_PLACEHOLDER = "$patientId$";
	protected final static String ENCOUNTER_ID_PLACEHOLDER = "$encounterId$";
	
	protected final static String EHR_ADDITIONAL_HEADER = "ehr.header";
	protected final static String EHR_API_KEY = "ehr.apikey";
	
	private String storagePath;
	private String fileExtension;
	@SuppressWarnings("unused")
	private String ehrName;
	
	@Autowired
	private HttpInvoker httpInvoker;

	public RestEHRService() {		
		// Retrieves storage path from config file
		storagePath = Configuration.getDBPath();
		
		// Retrieves file extension
		fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
		
		// Retrieves ehr name
		ehrName = Configuration.getProperty(Configuration.EHR_NAME);
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

		// create response
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT_PLAIN);

		// invokes REST service
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int retCode = httpInvoker.executeGet(serviceURI, out, getAdditionalHeaderParams());
		if (retCode == HttpStatus.SC_OK || retCode == HttpStatus.SC_ACCEPTED) {
			String patientId = out.toString();
			if (patientId != null && patientId.trim().length() > 0) {
				ehrResponse.setResponse(patientId);
			} else {
				ehrResponse.setStatus(EHRResponseStatus.FAILED);
				String msg = String.format(
						"Empty Body: no patient found in the EHR for citizen: firstName:%s, "
								+ "familyName: %s, dateOfBirth: %s",
						theCitizen.getFirstName(), theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
				logger.error(msg);
				ehrResponse.setMessage(msg);
			}
		} else if (retCode == HttpStatus.SC_NOT_FOUND) {
			ehrResponse.setStatus(EHRResponseStatus.FAILED);
			String msg = String.format(
					"404 NOT FOUND: no patient found in the EHR for citizen: firstName:%s, "
							+ "familyName: %s, dateOfBirth: %s",
					theCitizen.getFirstName(), theCitizen.getFamilyName(), theCitizen.getDateOfBirth());
			logger.error(msg);
			ehrResponse.setMessage(msg);
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR", retCode, serviceURI.toString());
			logger.error(errMsg);
			throw new IOException(errMsg);
		}

		return ehrResponse;
	}
	
	
	@Override
	public EHRFileResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_PATIENT_SUMMARY_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation getPatientSummary not supported by EHR.");
		
		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		URI serviceURI = createServiceURI(GET_PATIENT_SUMMARY_SERVICE_NAME, servicePath);
		
		// #3 invokes service		
		return downloadFileFromEHR(serviceURI, theRequestId);
	}

	
	@Override
	public EHRFileResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
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
		return downloadFileFromEHR(serviceURI, theRequestId);
	}
	
	
	@Override
	public EHRFileResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
		
		// #1 retrieves the list of components that are part of the encounter everything
		List<EncounterEverythingItem> items = retrieveEncounterEverythingItems(theEncounterId, 
				theRequestId, ehrPatientId);
		if (items.size() == 0) {
			throw new IllegalStateException("No items found in EHR for encounter: " + theEncounterId);
		}
		
		// #1 retrieves the each individual component
		EHRFileResponse encounterEverythingResponse = new EHRFileResponse();
		ContentType mime = ContentType.parse(Configuration.getProperty(Configuration.EHR_MIME));
		encounterEverythingResponse.setContentType(mime);

		EHRFileResponse individualItemResponse;
		for (EncounterEverythingItem item : items) {
			logger.debug("Requesting element '{}' of encounter {}", item.getType(), theEncounterId);
			individualItemResponse = downloadFileFromEHR(new URI(item.getUrl()), theRequestId);
			if (individualItemResponse.getStatus() == EHRResponseStatus.FAILED) {
				encounterEverythingResponse.setMessage(individualItemResponse.getMessage());
				encounterEverythingResponse.setStatus(EHRResponseStatus.FAILED);
				return encounterEverythingResponse;
			} else {
				encounterEverythingResponse.addResponseFile(individualItemResponse.getResponseFile(0));
			}
		}
		encounterEverythingResponse.setStatus(EHRResponseStatus.COMPLETED);
		return encounterEverythingResponse;
	}
	
	
	protected List<EncounterEverythingItem> retrieveEncounterEverythingItems(String theEncounterId, 
			String theRequestId, String ehrPatientId) throws Exception {
				
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_ENCOUNTER_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation getEncounterEverything not supported by EHR.");

		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		servicePath = servicePath.replace(ENCOUNTER_ID_PLACEHOLDER, theEncounterId);
		final String encodedServicePath = URLEncoder.encode(servicePath, StandardCharsets.UTF_8.toString());
		URI serviceURI = createServiceURI(GET_PATIENT_SERVICE_NAME, encodedServicePath);
		
		// invokes REST service
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int retCode = httpInvoker.executeGet(serviceURI, out, getAdditionalHeaderParams());
		if (retCode == HttpStatus.SC_OK || retCode == HttpStatus.SC_ACCEPTED) {
			String jsonList = out.toString();
			if (jsonList != null && jsonList.trim().length() > 0) {
				// parse list
				Gson gson = new Gson();
				Type encounterListType = new TypeToken<List<EncounterEverythingItem>>(){}.getType();
				List<EncounterEverythingItem> encounterItems = gson.fromJson(jsonList, encounterListType);
				return encounterItems;
			} else {
				String msg = String.format(
						"Empty Body: no items found in the EHR for encounter: %s ", theEncounterId);
				logger.error(msg);
				return new ArrayList<EncounterEverythingItem>();
			}
		} else if (retCode == HttpStatus.SC_NOT_FOUND) {
			String msg = String.format(
					"NOT FOUND 404: no items in the EHR for encounter: %s ", theEncounterId);
			logger.error(msg);			return new ArrayList<EncounterEverythingItem>();
		} else {
			String errMsg = String.format("Error %d while invoking service %s of EHR", retCode, serviceURI.toString());
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
	 */
	protected EHRFileResponse downloadFileFromEHR(URI uri, String theRequestId) throws Exception {
		// #1 creates output file
		File out = new File(storagePath + theRequestId + fileExtension);
		// #2 executes the GET		
		int retCode = httpInvoker.executeGet(uri, out, getAdditionalHeaderParams());
		final EHRFileResponse ehrFileResponse = new EHRFileResponse();
		out = new File(storagePath + theRequestId + fileExtension);
		
		if (retCode == HttpStatus.SC_OK && out.length() > 0) {
			ContentType mime = ContentType.parse(Configuration.getProperty(Configuration.EHR_MIME));
			ehrFileResponse.setContentType(mime);
			ehrFileResponse.addResponseFile(out);
			ehrFileResponse.setStatus(EHRResponseStatus.COMPLETED);
		} else {
			if (out.length() == 0) {
				logger.warn("Request ended with no errors, but EHR returned no data!");
				ehrFileResponse.setMessage("No data produced by EHR.");
				ehrFileResponse.setStatus(EHRResponseStatus.FAILED);
			} else if (retCode == HttpStatus.SC_NOT_FOUND){
				logger.warn("The current request received a 404-NOT FOUND from EHR!");
				ehrFileResponse.setMessage("No data found on EHR.");
				ehrFileResponse.setStatus(EHRResponseStatus.FAILED);
			} else {
				String errorMsg = String.format("The current request received the following error code from EHR: %d", retCode);
				logger.error(errorMsg);
				ehrFileResponse.setMessage(errorMsg);
				ehrFileResponse.setStatus(EHRResponseStatus.FAILED);
			}
		}
		
		return ehrFileResponse;
	}
	
	
	/**
	 * Looks for additional header parameters in the configuration file
	 * 
	 * @return
	 */
	protected HeaderParam[] getAdditionalHeaderParams() {
		String headerAttrs = Configuration.getProperty(EHR_ADDITIONAL_HEADER);
		
		if (headerAttrs == null || headerAttrs.trim().length() == 0)
			return new HeaderParam[0];
		
		StringTokenizer st = new StringTokenizer(headerAttrs, ";");
		HeaderParam[] params = new HeaderParam[st.countTokens()];
		String tmp;
		int counter = 0;
		while (st.hasMoreTokens()) {
			tmp = st.nextToken();
			params[counter++] = new HeaderParam(
					tmp.substring(0, tmp.indexOf(":")),
					tmp.substring(tmp.indexOf(":") + 1));
		}
		
		return params;
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
	protected URI createServiceURI(String serviceName, String servicePath) throws Exception {
		String ehrProtocol = Configuration.getProperty(Configuration.EHR_PROTOCOL);
		String ehrHost = Configuration.getProperty(Configuration.EHR_HOST);
		String ehrPort = Configuration.getProperty(Configuration.EHR_PORT);
		
		String ehrContextPath = Configuration.getProperty(Configuration.EHR_CONTEXT_PATH);
		if (ehrContextPath != null && !ehrContextPath.isEmpty()) {
			servicePath = ehrContextPath + servicePath;
		}
		
		URL url = new URL(ehrProtocol, ehrHost, new Integer(ehrPort), servicePath);
		return url.toURI();
	}

}
