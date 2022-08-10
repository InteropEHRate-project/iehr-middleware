package eu.interopehrate.r2d.ehr.model;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: generic wrapper exception thrown by several methods of
 * classes of the EHR Middleware.
 */
public class EHRMWException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5281069473154952180L;

	public EHRMWException() {
		super();
	}

	public EHRMWException(String message, Throwable cause) {
		super(message, cause);
	}

	public EHRMWException(String message) {
		super(message);
	}

	public EHRMWException(Throwable cause) {
		super(cause);
	}

}
