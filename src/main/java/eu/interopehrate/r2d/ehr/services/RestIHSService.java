package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestIHSService implements IHSService {
	
	private final OkHttpClient client;
	private final Log logger = LogFactory.getLog(RestIHSService.class);
	
	public RestIHSService() {
		// TODO: add proxy to configuration?
		client = new OkHttpClient.Builder()
			      .readTimeout(5, TimeUnit.MINUTES)
			      .writeTimeout(5, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .build();
	}

	@Override
	public void requestConversion(EHRRequest ehrRequest, String cdaBundle) throws Exception {
		// #1 Creates the URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/requestConversion?resourceName=");

		// #2 Customize the URL
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER)
			serviceURL.append("Encounter");
		else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			// TODO: analizzare il codice LOINC contenuto nel CDA ritornato
			String resourceType = determineReportType(cdaBundle);
			serviceURL.append(resourceType);
		} else
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		
		// #3 Creates the OKHttp object to submit the request
		Request postRequest = new Request.Builder()
                .url(serviceURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(ContentType.XML_CDA.getContentType()), cdaBundle))
                .build();
		
		// #4 Submits the request
		Response httpResponse = null;
		try {
			logger.debug(String.format("Invoking service of IHS: %s", serviceURL.toString()));
			httpResponse = client.newCall(postRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error '%s' while invoking service of IHS", ioe.getMessage()));
			throw ioe;
		}
		
		// #5 Checks the response
		if (!httpResponse.isSuccessful()) {
			String errMsg = String.format("Error %d while invoking service of IHS: %s", 
	    			httpResponse.code(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);
		}
	}

	
	@Override
	public EHRResponse retrieveConversionResult(EHRRequest ehrRequest) throws Exception {
		// #1 Creates the URL for sendong the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/retrieveFHIRHealthRecord?");
		// handles 'lang' parameter
		if (ehrRequest.getPreferredLanguage() != null) 
			serviceURL.append("lang=").append(ehrRequest.getPreferredLanguage()).append("&");
		
		// handles 'call' parameter
		serviceURL.append("call=");

		// #2 customize the URL
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			serviceURL.append("encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			serviceURL.append("encounter/");
			serviceURL.append(ehrRequest.getParameter(EHRRequest.PARAM_RESOURCE_ID));
			serviceURL.append("/everything");
		} else {
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		}
		
		// #3 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(serviceURL.toString())
                .get()
                .build();
		
		// #4 Submits the request
		Response httpResponse = null;
		try {
			logger.debug(String.format("Invoking service of IHS: %s", serviceURL.toString()));
			httpResponse = client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of IHS", ioe.getMessage()));
			throw ioe;
		}
		
		// #5 Checks the response
		if (httpResponse.isSuccessful()) {
			EHRResponse ihsResponse = new EHRResponse(ContentType.JSON_FHIR, EHRResponseStatus.COMPLETED);
			ihsResponse.setResponse(httpResponse.body().string());
			return ihsResponse;
		} else {
			String errMsg = String.format("Error %d while invoking service of IHS: %s", 
	    			httpResponse.code(), httpResponse.message());

			logger.error(errMsg);
			throw new IOException(errMsg);				
		}
		
	}
	
	
	private String determineReportType(String cdaBundle) {
		if (cdaBundle.contains("code=\"30954-2\""))
			return "DiagnosticReport-LabResult";
		
		return null;
	}

}
