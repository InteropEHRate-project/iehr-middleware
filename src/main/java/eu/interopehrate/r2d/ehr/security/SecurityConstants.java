package eu.interopehrate.r2d.ehr.security;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *     
 *     Security constants
 */

public final class SecurityConstants {
	
	/**
	 * Name of the header attribute that stores the authentication token
	 */
	public final static String AUTH_HEADER = "Authorization";
	
	/**
	 * Prefix of the value of the header attribute named AUTH_HEADER
	 */
	public final static String OAUTH_PREFIX = "Bearer ";

	/**
	 * Prefix of the value of the header attribute named AUTH_HEADER
	 */
	public final static String BASIC_PREFIX = "Basic ";
	
	/**
	 * Header Parameter name for R2D request ID
	 */
	public final static String R2D_REQUEST_ID_PARAM_NAME = "R2D-Request-Id";
	
	/**
	 * Header Parameter name for eidas token
	 */
	public final static String R2D_REQUEST_CITIZEN_PARAM_NAME = "R2D-Citizen-Token";

	/**
	 * Header Parameter name for Citizen object
	 */
	public final static String CITIZEN_ATTR_NAME = "R2D-Citizen";
	

}
