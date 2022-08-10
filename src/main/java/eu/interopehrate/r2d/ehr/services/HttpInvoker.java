package eu.interopehrate.r2d.ehr.services;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import eu.interopehrate.r2d.ehr.model.HeaderParam;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: defines an interface for a class submitting  
 * GET and / or POST request over HTTP. This class has been 
 * defined to decouple the proxy classes requesting a REST
 * service from the HTTP client used to implement the request
 * (Apache HttpClient or OKHttp or ).
 * 
 * @SeeAlso: ApacheHttpInvoker.java
 */

public interface HttpInvoker {

	/**
	 * 
	 * @param uri
	 * @param headerParams
	 * @return
	 * @throws IOException
	 */
	public int executeGet(URI uri, HeaderParam... headerParams) throws IOException;		

	
	/**
	 * 
	 * @param uri
	 * @param output
	 * @param headerParams
	 * @return
	 * @throws IOException
	 */
	public int executeGet(URI uri, File output, HeaderParam... headerParams) throws IOException;		

	
	/**
	 * 
	 * @param uri
	 * @param output
	 * @param headerParams
	 * @return
	 * @throws IOException
	 */
	public int executeGet(URI uri, OutputStream output, HeaderParam... headerParams) throws IOException;
	
	
	/**
	 * 
	 * @param uri
	 * @param body
	 * @param contentType
	 * @return
	 * @throws IOException
	 */
	public int executePost(URI uri, File body, String contentType) throws IOException;
	
	
	/**
	 * 
	 * @param uri
	 * @param body
	 * @param contentType
	 * @param headerParams
	 * @return
	 * @throws IOException
	 */
	public int executePost(URI uri, File body, String contentType, HeaderParam... headerParams) throws IOException;

	
	/**
	 * 
	 * @param uri
	 * @param body
	 * @param contentType
	 * @param headerParams
	 * @return
	 * @throws IOException
	 */
	public int executePost(URI uri, String body, String contentType, HeaderParam... headerParams) throws IOException;

}
