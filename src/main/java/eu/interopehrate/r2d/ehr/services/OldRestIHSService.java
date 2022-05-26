package eu.interopehrate.r2d.ehr.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.StringTokenizer;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

@Deprecated
public class OldRestIHSService implements IHSService {
	
	private static final String FILE_EXT = ".json";
	private final OkHttpClient client;
	private final Logger logger = LoggerFactory.getLogger(OldRestIHSService.class);
	
	private String storagePath;

	public OldRestIHSService() {
		client = getClient();
	}

	
	private OkHttpClient getClient() {
		// Checks for proxy settings
		Proxy proxy = Proxy.NO_PROXY;
		String proxyEndpoint = Configuration.getProperty("ihs.proxy.endpoint");
		String proxyPort = Configuration.getProperty("ihs.proxy.port");
		if (proxyEndpoint != null && proxyEndpoint.trim().length() > 0) {
			proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyEndpoint, Integer.valueOf(proxyPort)));
		}
				
		// Retrieves storage path from config file
		storagePath = Configuration.getDBPath();
		if (!storagePath.endsWith("/"))
			storagePath += "/";

		// Creates the client
		Integer timeOutInMinutes = Integer.valueOf(Configuration.getProperty("ihs.timeoutInMinutes"));
		return new OkHttpClient.Builder()
			      .readTimeout(timeOutInMinutes, TimeUnit.MINUTES)
			      .writeTimeout(timeOutInMinutes, TimeUnit.MINUTES)
			      .retryOnConnectionFailure(true)
			      .proxy(proxy)
			      .build();	
	}
	
	
	/*
	 * Invokes the service for requesting a conversion
	 */
	@Override
	public void requestConversion(EHRRequest ehrRequest, EHRResponse ehrResponse) throws Exception {
		if (!ehrResponse.isOnFile() && ehrResponse.getResponse().isEmpty()) {
			throw new IllegalStateException("Response retrieved from EHR not found on file as expected!");
		}
		
		// builds CDA filename, it contains the CDA retrieved by EHR
		final String ehrFileName = storagePath + ehrResponse.getResponse();
		
		// #1 Creates the URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/requestConversion?resourceName=");

		// #2 Customize the URL depending on the type of operation requested
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			serviceURL.append("Encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			serviceURL.append(buildParamString(ehrFileName));
		} else
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		
		// #3 Creates the OKHttp object to submit the request
		if (logger.isDebugEnabled())
			logger.debug("Invoking service of IHS: {}", serviceURL.toString());
		
		MediaType mime = MediaType.parse(ContentType.APPLICATION_XML.toString());
		Request postRequest = new Request.Builder()
                .url(serviceURL.toString())
                .addHeader("Content-Type", ContentType.APPLICATION_XML.toString() + "; charset=UTF-8")
                .post(new FileRequestBody(new File(ehrFileName), mime))
                .build();
		
		// #4 Submit the request to the IHS requestConversion service
		try (Response response = client.newCall(postRequest).execute()) {
			// #5 Checks the response
			if (!response.isSuccessful()) {
				String errMsg = String.format("Error %d while invoking service of IHS: %s", 
						response.code(), response.message());

				logger.error(errMsg);
				throw new IOException(errMsg);
			}
		} catch (IOException ioe) {
			if (ioe.getCause() != null && ioe.getCause().getMessage().startsWith("\\n not found"))
				logger.warn("Socket hangup during execution of service requestConversion of IHS");
			else {	
				logger.error("Error '{}' while invoking requestConversion of IHS", ioe.getMessage());
				throw ioe;
			}
		}
	}

	
	/*
	 * Invokes the service for retrieving the result of a previously invoked conversion
	 */
	@Override
	public EHRResponse retrieveFHIRHealthRecord(EHRRequest ehrRequest) throws Exception {
		// #1 Creates the base URL for sending the request to IHS
		String ihsBase = Configuration.getProperty(Configuration.IHS_ENDPOINT);
		StringBuilder serviceURL = new StringBuilder(ihsBase);
		serviceURL.append("/retrieveFHIRHealthRecord?");
		// #2 starts adding URL parameters
		
		// #2.1 handles 'lang' parameter
		if (ehrRequest.getPreferredLanguage() != null) 
			serviceURL.append("lang=").append(ehrRequest.getPreferredLanguage()).append("&");
		
		// #2.2 handles 'call' parameter
		serviceURL.append("call=");
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			serviceURL.append("encounter");
		} else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			serviceURL.append(String.format("encounter/%s/everything", 
					ehrRequest.getParameter(EHRRequest.PARAM_RESOURCE_ID)));
		} else {
			throw new NotImplementedException("Operation " + ehrRequest.getOperation() +
					" not implemented.");
		}
		
		// #3 If successful another call is needed to get the results
		if (logger.isDebugEnabled())
			logger.debug(String.format("Invoking service of IHS: %s", serviceURL.toString()));

		Request getRequest = new Request.Builder()
                .url(serviceURL.toString())
                .addHeader("Accept", ContentType.APPLICATION_JSON.toString())
                .get()
                .build();
		
		// #4 Submits the request
		try (Response response = client.newCall(getRequest).execute()) {
			// #5 Checks the response
			if (response.isSuccessful()) {
				// Copy results to file
				final File out = new File(Configuration.getR2DADBPath() + ehrRequest.getR2dRequestId() + FILE_EXT);
				try (InputStream inStream = response.body().byteStream();
					 OutputStream outStream = new BufferedOutputStream(new FileOutputStream(out));) {
					IOUtils.copy(inStream, outStream);				
				}
				
				// create response 
				final EHRResponse ihsResponse = new EHRResponse(ContentType.APPLICATION_JSON);
				ihsResponse.setOnFile(true);
				ihsResponse.setResponse(ehrRequest.getR2dRequestId() + FILE_EXT);
				ihsResponse.setStatus(EHRResponseStatus.COMPLETED);
				logger.debug("Saved to file '{}' {} Kb received from IHS", 
						ihsResponse.getResponse(),
						NumberFormat.getInstance().format(out.length() / 1024D));
				return ihsResponse;
			} else {
				String errMsg = String.format("Error %d while invoking service of IHS: %s", 
		    			response.code(), response.message());

				logger.error(errMsg);
				throw new IOException(errMsg);				
			}
		} catch (IOException ioe) {
			logger.error(String.format("Error %s while invoking service of IHS", ioe.getMessage()));
			throw ioe;
		}
		
	}

	
	private class FileRequestBody extends RequestBody {
		private MediaType mediaType; 
		private File bodyContent;
		
		public FileRequestBody(File bodyContent, MediaType mediaType) {
			super();
			this.mediaType = mediaType;
			this.bodyContent = bodyContent;
		}

		@Override
		public MediaType contentType() {
			return this.mediaType;
		}

		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(bodyContent))) {
				long count = 0;
		        int n;
				final byte[] buffer = new byte[1024 * 5];
		        while (-1 != (n = input.read(buffer))) {
		            sink.write(buffer, 0, n);
		            count += n;
		        }
		        sink.write("\n".getBytes());
				logger.debug("Sent {} bytes to IHS", count);
			}
		}

		@Override
		public boolean isOneShot() {
			return true;
		}
	}
	
	/*
	 * Builds the param string needed when invoking the service for converting results. 
	 * This parameter is built searching specific string(s) in the CDA file containing the
	 * CDA Document to sent to IHS.
	 * 
	 * The list of codes to be searched is stored in the configuration file
	 */
	private String buildParamString(String cdaFileName) throws Exception {
		StringTokenizer codes = new StringTokenizer(Configuration.getProperty("ihs.mapping.codes"), ";");
		StringBuffer paramString = new StringBuffer();		
		Process process;
		String currentCode;
		boolean isFirst = true;

		while (codes.hasNext()) {
			currentCode = codes.next();
			process = Runtime.getRuntime().exec(String.format("grep -i %s %s", currentCode, cdaFileName));
			if (process.waitFor() == 0) {
				if (!isFirst)
					paramString.append(";");
				paramString.append(Configuration.getProperty("ihs.mapping.code." + currentCode));
				isFirst = false;
			}
		}
		
		return paramString.toString();
	}

}
