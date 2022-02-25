package eu.interopehrate.r2d.ehr.old.workflow;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRMWException;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Deprecated
class RequestToIHSActivity extends Activity {

	@Override
	protected void processRequest(EHRRequest ehrRequest) throws Exception {			
		// #1 Creates the URL for sendong the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder postURL = new StringBuilder(ihsBase);
		postURL.append("/requestConversion?resourceName=");
		StringBuilder getURL = new StringBuilder(ihsBase);
		getURL.append("/retrieveFHIRHealthRecord?call=");

		// #2 customise the URL
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			postURL.append("Encounter");
			getURL.append("encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			postURL.append("Encounter");
			getURL.append("encounter/").append("id").append("everything");
		} else {
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		}
		
		
		// #3 Creates and submits the HTTP request object
		EHRResponse ehrResponse = ehrRequest.getEhrResponse();
		Request postRequest = new Request.Builder()
                .url(postURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(ehrResponse.getContentType().getContentType()),
                        ehrResponse.getResponse()))
                .build();
		
		final OkHttpClient client = new OkHttpClient();
		Response httpResponse = null;
		try {
			logger.debug(String.format("Submitting POST request to IHS: %s", postURL.toString()));
			httpResponse = client.newCall(postRequest).execute();
		} catch (IOException ioe) {
			EHRResponse ihsResponse = new EHRResponse(ContentType.TEXT, EHRResponseStatus.FAILED);
			String errMsg = String.format("Error %s while sending POST request to IHS Server", ioe.getMessage());
			ihsResponse.setMessage(errMsg);
			ehrRequest.setIhsResponse(ihsResponse);

			throw new EHRMWException(errMsg);
		}		

		if (!httpResponse.isSuccessful()) {
			EHRResponse ihsResponse = new EHRResponse(ContentType.TEXT, EHRResponseStatus.FAILED);
			String errMsg = String.format("Error %d while sending POST request to IHS Server: %s", 
	    			httpResponse.code(), httpResponse.message());
			ihsResponse.setMessage(errMsg);
			ehrRequest.setIhsResponse(ihsResponse);
			
	    	throw new EHRMWException(errMsg);
		}
		
		// #4 If successful another call is needed to get the results
		Request getRequest = new Request.Builder()
                .url(getURL.toString())
                .get()
                .build();
		
		try {
			logger.debug(String.format("Submitting GET request to IHS: %s", getURL.toString()));
			httpResponse = client.newCall(getRequest).execute();
		} catch (IOException ioe) {
			// TODO: handles retry for service unavailable
	    	throw new EHRMWException(String.format("Error %s while sending GET request to IHS Server", ioe.getMessage()));
		}
		
		if (!httpResponse.isSuccessful()) {
	    	throw new EHRMWException(String.format("Error %d while sending GET request to IHS Server: %s", 
	    			httpResponse.code(), httpResponse.message()));
		}
		
		// store the result
		EHRResponse ihsResponse = new EHRResponse(ContentType.JSON_FHIR, EHRResponseStatus.COMPLETED);
		ihsResponse.setResponse(httpResponse.body().string());		
		ehrRequest.setIhsResponse(ihsResponse);
	}
		
}
