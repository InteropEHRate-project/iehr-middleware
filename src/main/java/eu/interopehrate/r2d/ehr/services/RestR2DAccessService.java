package eu.interopehrate.r2d.ehr.services;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestR2DAccessService implements R2DAccessService {

	private final OkHttpClient client;
	private final Logger logger = LoggerFactory.getLogger(RestR2DAccessService.class);
	
	public RestR2DAccessService() {
		// TODO: add proxy to configuration?
		client = new OkHttpClient.Builder()
			      .writeTimeout(2, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .build();
	}
	
	@Override
	public void sendSuccesfulResponse(EHRRequest ehrRequest, EHRResponse ihsResponse) throws Exception {
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
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .post(RequestBody.create(new byte[]{}, null))
                .build();
		
		// #4 Submits the request
		if (logger.isDebugEnabled())
			logger.debug(String.format("Invoking R2D Server callback: %s", serviceURL.toString()));

		try (Response httpResponse = client.newCall(request).execute()) {
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while invoking R2D Server callback: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}			
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking R2D Server callback", ioe.getMessage()));
			throw ioe;
		}
		
		
		/*
		Response httpResponse = null;
		try {
			if (logger.isDebugEnabled())
				logger.debug(String.format("Invoking R2D Server callback: %s", serviceURL.toString()));
			httpResponse = client.newCall(request).execute();
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while invoking R2D Server callback: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking R2D Server callback", ioe.getMessage()));
			throw ioe;
		}
		*/
		
	}

	
	@Override
	public void sendUnsuccesfulResponse(EHRRequest ehrRequest, String errorMsg) throws Exception {
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
                .post(RequestBody.create(errorMsg,
                        MediaType.parse(ContentType.TEXT.getContentType())))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();
		
		
		// #4 Submits the request
		if (logger.isDebugEnabled())
			logger.debug(String.format("Invoking R2D Server callback: %s", serviceURL.toString()));

		try (Response httpResponse = client.newCall(request).execute()) {	
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while invoking R2D Server callback: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking R2D Server callback", ioe.getMessage()));
			throw ioe;
		}		
		
		/*
		// #4 Submits the request
		Response httpResponse = null;
		try {
			if (logger.isDebugEnabled())
				logger.debug(String.format("Invoking R2D Server callback: %s", serviceURL.toString()));
			httpResponse = client.newCall(request).execute();
			// #5 Checks the response
			if (!httpResponse.isSuccessful()) {
				String errMsg = String.format("Error %d while invoking R2D Server callback: %s", 
		    			httpResponse.code(), httpResponse.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking R2D Server callback", ioe.getMessage()));
			throw ioe;
		}
		*/
	}

}
