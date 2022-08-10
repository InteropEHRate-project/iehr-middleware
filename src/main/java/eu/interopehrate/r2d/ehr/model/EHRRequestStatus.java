package eu.interopehrate.r2d.ehr.model;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: enumeration defining the available status
 * of processing of an instance of EHRRequest.
 */
public enum EHRRequestStatus {

	NEW, 
	TO_EHR,
	EHR_FAILED,
	TO_IHS,
	IHS_FAILED,
	EXECUTED,
	R2D_FAILED,
	COMPLETED;
	
}
