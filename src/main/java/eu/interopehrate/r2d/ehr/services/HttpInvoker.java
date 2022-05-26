package eu.interopehrate.r2d.ehr.services;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public interface HttpInvoker {

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
