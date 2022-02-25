package eu.interopehrate.r2d.ehr.old.workflow;

import java.io.IOException;
import java.util.Base64;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRMWException;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRRequestStatus;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Deprecated
public class SendToR2DTask extends Activity {

	@Override
	protected void processRequest(EHRRequest ehrRequest) throws Exception {
		if (ehrRequest.getStatus() != EHRRequestStatus.EXECUTED && 
			ehrRequest.getStatus() != EHRRequestStatus.EHR_FAILED && 
			ehrRequest.getStatus() != EHRRequestStatus.IHS_FAILED)
			throw new IllegalStateException(String.format("The status %s of the request %s "
					+ "does not allow to elaborate it. ", ehrRequest.getStatus(), ehrRequest.getR2dRequestId()));
		
		
		String credentials = Configuration.getProperty(Configuration.EHR_MW_CREDENTIALS);
		
		/*
		Response response = null;
		if (ehrRequest.getStatus() != EHRRequestStatus.EXECUTED) {
			response = requestCompleted(ehrRequest, credentials);
		} else if (ehrRequest.getStatus() != EHRRequestStatus.EHR_FAILED) {
			response = requestCompletedWithError(ehrRequest, credentials);			
		} else if (ehrRequest.getStatus() != EHRRequestStatus.IHS_FAILED) {
			response = requestCompletedWithError(ehrRequest, credentials);
		} 
		*/
		
		
		// #1 Creates callback url
		StringBuilder forwardURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		forwardURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		forwardURL.append(ehrRequest.getStatus() == EHRRequestStatus.EXECUTED 
				? "/completed-succesfully" : "/completed-unsuccesfully");
		
		// #2 creates body 
		String postBody = "Unexpected error while sending response to R2DAccess Server";
		String mimeType = "";
		EHRResponse ehrResponse;
		if (ehrRequest.getStatus() == EHRRequestStatus.EXECUTED) {
			ehrResponse = ehrRequest.getIhsResponse();
			postBody = ehrResponse.getResponse();
			mimeType = ehrResponse.getContentType().getContentType();
		} else {
			mimeType = ContentType.TEXT.getContentType();
			if (ehrRequest.getStatus() == EHRRequestStatus.EHR_FAILED)
				ehrResponse = ehrRequest.getEhrResponse();
			else
				ehrResponse = ehrRequest.getIhsResponse();
			if (ehrResponse != null)
				postBody = ehrResponse.getMessage();
		}
		
		// #3 Adds credentials to post
		// String credentials = Configuration.getProperty(Configuration.EHR_MW_CREDENTIALS);
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());
		
		// #4 Creates and submits the HTTP request object
		Request request = new Request.Builder()
                .url(forwardURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(mimeType), postBody))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();

		// #5 submit POST request to R2DAccess Server
		Response response = null;
		final OkHttpClient client = new OkHttpClient();
		try {
			response = client.newCall(request).execute();
		} catch (IOException ioe) {
			// TODO: handles retry for service unavailable
	    	throw new EHRMWException(String.format("Error %s while sending response to R2DAccess Server", ioe.getMessage()));
		}
		
		// Verifies the return code to determine if transmission was OK
	    if (!response.isSuccessful()) {
	    	throw new EHRMWException(String.format("Error %d while sending response to R2DAccess Server: %s", 
	    			response.code(), response.message()));
	    }
	}
	

	public Response requestCompleted(EHRRequest ehrRequest, String credentials) throws IOException {
		StringBuilder forwardURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		forwardURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		forwardURL.append("/completed-succesfully");
		
		EHRResponse ehrResponse = ehrRequest.getIhsResponse();
		String mimeType = ehrResponse.getContentType().getContentType();

		// #3 Adds credentials to post
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());
		
		// #4 Creates and submits the HTTP request object
		Request request = new Request.Builder()
                .url(forwardURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(mimeType), ehrResponse.getResponse()))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();
		
		return (new OkHttpClient()).newCall(request).execute();
	}
	
		
	public Response requestCompletedWithError(EHRRequest ehrRequest, String errorMsg, String credentials) throws IOException {
		StringBuilder forwardURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		forwardURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		forwardURL.append("/completed-unsuccesfully");
		
		
		// #3 Adds credentials to post
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());

		// #4 Creates and submits the HTTP request object
		Request request = new Request.Builder()
                .url(forwardURL.toString())
                .post(RequestBody.create(
                        MediaType.parse(""), ""))
                .addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes))
                .build();
		
		return (new OkHttpClient()).newCall(request).execute();
	}

	
}
