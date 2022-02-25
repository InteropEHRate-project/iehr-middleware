package eu.interopehrate.r2d.ehr.model;

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
