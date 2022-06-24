package eu.interopehrate.r2d.ehr.services.impl;

import java.net.URI;
import java.util.Base64;

import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.security.SecurityConstants;
import eu.interopehrate.r2d.ehr.services.HeaderParam;
import eu.interopehrate.r2d.ehr.services.HttpInvoker;
import eu.interopehrate.r2d.ehr.services.R2DAccessService;

public class RestR2DAccessService implements R2DAccessService {

	@Autowired
	private HttpInvoker httpInvoker;

	@Override
	public void sendSuccesfulResponse(EHRRequest ehrRequest, EHRResponse ihsResponse) throws Exception {
		// #1 Creates the URL
		StringBuilder serviceURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		serviceURL.append("/callbacks/").append(ehrRequest.getR2dRequestId());
		serviceURL.append("/completed-succesfully");
		
		// #Reads the credentials
		String credentials = Configuration.getProperty(Configuration.EHR_MW_CREDENTIALS);
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());

		httpInvoker.executePost(new URI(serviceURL.toString()), 
				"", 
				ContentType.APPLICATION_JSON.toString(), 
				new HeaderParam(SecurityConstants.AUTH_HEADER, 
						SecurityConstants.BASIC_PREFIX + new String(encodedBytes)));
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

		httpInvoker.executePost(new URI(serviceURL.toString()), 
				errorMsg, 
				ContentType.APPLICATION_JSON.toString(), 
				new HeaderParam(SecurityConstants.AUTH_HEADER, 
						SecurityConstants.BASIC_PREFIX + new String(encodedBytes)));
	}
	
}
