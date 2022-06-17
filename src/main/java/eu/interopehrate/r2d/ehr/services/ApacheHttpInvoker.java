package eu.interopehrate.r2d.ehr.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.NumberFormat;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;

public class ApacheHttpInvoker implements HttpInvoker {
	private final Logger logger = LoggerFactory.getLogger(ApacheHttpInvoker.class);

	
	
	@Override
	public int executeGet(URI uri, OutputStream output, HeaderParam... headerParams) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Invoking GET service: {}", uri.toString());

		try (CloseableHttpClient httpclient = createClient()) {
			// Creates and config GET request
			HttpGet httpGet = new HttpGet(uri);
			httpGet.setConfig(createRequestConfig());
			
			// Adds request header params
			for (HeaderParam param : headerParams)
				httpGet.addHeader(param.getName(), param.getValue());
			
			// Executes request
			try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
				if (logger.isDebugEnabled())
					logger.debug("Service returned the following code: {}", response.getStatusLine().getStatusCode());

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || 
					response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
					// Copy results to file
					try (InputStream inStream = response.getEntity().getContent();
						 OutputStream outStream = new BufferedOutputStream(output);) {
						IOUtils.copy(inStream, outStream);
					}
				}
				
				return response.getStatusLine().getStatusCode();
			}
		} 
	}



	@Override
	public int executeGet(URI uri, File output, HeaderParam... headerParams) throws IOException {		
		if (logger.isDebugEnabled())
			logger.debug("Invoking GET service: {}", uri.toString());

		try (CloseableHttpClient httpclient = createClient()) {
			// Creates and config GET request
			HttpGet httpGet = new HttpGet(uri);
			httpGet.setConfig(createRequestConfig());
			
			// Adds request header params
			for (HeaderParam param : headerParams)
				httpGet.addHeader(param.getName(), param.getValue());
			
			// Executes request
			try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
				if (logger.isDebugEnabled())
					logger.debug("Service returned the following code: {}", response.getStatusLine().getStatusCode());

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					// Copy results to file
					try (InputStream inStream = response.getEntity().getContent();
						 OutputStream outStream = new BufferedOutputStream(new FileOutputStream(output));) {
						IOUtils.copy(inStream, outStream);
					}
					
					logger.debug("Saved to file '{}' {} Kb received from the service.", output.getName(),
							NumberFormat.getInstance().format(output.length() / 1024D));
					
					return HttpStatus.SC_OK;
				} else {
					return response.getStatusLine().getStatusCode();
				}
			}
		} 
	}

	
	
	@Override
	public int executePost(URI uri, File body, String contentType) throws IOException {
		return this.executePost(uri, body, contentType, new HeaderParam[0]);
	}

	
	@Override
	public int executePost(URI uri, File body, String contentType, HeaderParam... headerParams) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Invoking POST service: {}", uri.toString());

		try (CloseableHttpClient httpclient = createClient()) {
			// Creates and config GET request
			HttpPost httpPost = new HttpPost(uri);
			httpPost.setConfig(createRequestConfig());
			
			// sets header parameter
			for (HeaderParam param : headerParams )
				httpPost.addHeader(param.getName(), param.getValue());

			// sets body mime type
			ContentType mime = ContentType.parse(contentType);
			// set body
			httpPost.setEntity(new FileEntity(body, mime));
			
			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				if (logger.isDebugEnabled())
					logger.debug("Service returned the following code: {}", response.getStatusLine().getStatusCode());

				return response.getStatusLine().getStatusCode();
			} catch (NoHttpResponseException nre) { 
				// TODO: remove if IHS solves issue
				logger.warn("Unexpected closure of socket. Service did not provide a return code!");
				return HttpStatus.SC_OK;
			} 
		}
	}
	

	@Override
	public int executePost(URI uri, String body, String contentType, HeaderParam... headerParams) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Invoking POST service: {}", uri.toString());

		try (CloseableHttpClient httpclient = createClient()) {
			// Creates and config GET request
			HttpPost httpPost = new HttpPost(uri);
			httpPost.setConfig(createRequestConfig());
			
			// sets body mime
			ContentType mime = ContentType.parse(contentType);
			for (HeaderParam param : headerParams )
				httpPost.addHeader(param.getName(), param.getValue());

			// set body
			if (body == null)
				body = "Generic error on R2DAccessServer.";
			
			httpPost.setEntity(new StringEntity(body, mime));
			
			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				if (logger.isDebugEnabled())
					logger.debug("Service returned the following code: {}", response.getStatusLine().getStatusCode());

				return response.getStatusLine().getStatusCode();
			} catch (NoHttpResponseException nre) { 
				// TODO: remove if IHS solves issue
				logger.warn("Unexpected closure of socket. Service did not provide a return code!");
				return HttpStatus.SC_OK;
			} 
		}
	}
	
	
	private CloseableHttpClient createClient() {
		return HttpClients.custom()
		.disableAutomaticRetries()
		.evictExpiredConnections()
		.build();
//		return HttpClients.createSystem();
	}
	
	
	private RequestConfig createRequestConfig() {
		// Creates configured Get request
		final Builder configBuilder = RequestConfig.custom();
		// configures timeout
		int timeout = Integer.valueOf(Configuration.getProperty(Configuration.IHS_TIMEOUT)) * 60000;
		configBuilder
			.setSocketTimeout(timeout)
			.setConnectTimeout(timeout)
			.setConnectionRequestTimeout(timeout);
		
		return configBuilder.build();
	}

}
