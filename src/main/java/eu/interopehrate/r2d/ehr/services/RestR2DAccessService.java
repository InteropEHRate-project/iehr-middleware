package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestR2DAccessService implements R2DAccessService {

	private final OkHttpClient client;
	private final Log logger = LogFactory.getLog(RestR2DAccessService.class);
	
	public RestR2DAccessService() {
		// TODO: add proxy to configuration?
		client = new OkHttpClient.Builder()
			      .writeTimeout(20, TimeUnit.SECONDS)
			      .retryOnConnectionFailure(true)
			      .build();
	}
	
	@Override
	public void sendSuccesfulResponse(EHRRequest ehrRequest, String fhirBundle) throws Exception {
		// #1 Creates the URL
		StringBuilder serviceURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		serviceURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		serviceURL.append("/completed-succesfully");
		
		// #Reads the credentials
		String credentials = Configuration.getProperty(Configuration.EHR_MW_CREDENTIALS);
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());
		
		// #3 Creates and submits the HTTP request object
		Request request = new Request.Builder()
                .url(serviceURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(ContentType.JSON_FHIR.getContentType()), fhirBundle))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();
		
		// #4 Submits the request
		Response httpResponse = null;
		try {
			logger.debug(String.format("Submitting request to IHS: %s", serviceURL.toString()));
			httpResponse = client.newCall(request).execute();
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while sending request to R2D Server: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while sending request to R2D Server", ioe.getMessage()));
			throw ioe;
		}
	}

	
	@Override
	public void sendUnsuccesfulResponse(EHRRequest ehrRequest, String errorMsg) throws Exception {
		logger.debug("Sending this error msg to R2D Access: " + errorMsg);
		// #1 Creates the URL
		StringBuilder serviceURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		serviceURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		serviceURL.append("/completed-unsuccesfully");
		
		// #Reads the credentials
		String credentials = Configuration.getProperty(Configuration.EHR_MW_CREDENTIALS);
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());
		
		// #3 Creates and submits the HTTP request object
		Request request = new Request.Builder()
                .url(serviceURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(ContentType.TEXT.getContentType()), errorMsg))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();
		
		// #4 Submits the request
		Response httpResponse = null;
		try {
			logger.debug(String.format("Submitting request to IHS: %s", serviceURL.toString()));
			httpResponse = client.newCall(request).execute();
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while sending request to R2D Server: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while sending request to R2D Server", ioe.getMessage()));
			throw ioe;
		}
	}

}
