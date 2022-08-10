package eu.interopehrate.r2d.ehr.services;

import java.util.Date;

import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of the EHRService to execute the following services:
 * 1) authenticate citizen to EHR
 * 2) download of the patient summary
 * 3) download of the list of the encounters of the patient
 * 4) download of the health data produced during an encounter
 * 
 * @SeeAlso: RestEHRService.java
 * 
 */

public interface EHRService {
	public static final String GET_PATIENT_SERVICE_NAME = "GET_PATIENT";
	public static final String GET_PATIENT_SUMMARY_SERVICE_NAME = "GET_PATIENT_SUMMARY";
	public static final String SEARCH_ENCOUNTER_SERVICE_NAME = "SEARCH_ENCOUNTER";
	public static final String GET_ENCOUNTER_SERVICE_NAME = "GET_ENCOUNTER_EVERYTHING";

	/**
	 * 
	 * @param theRequestId
	 * @param theCitizen
	 * 
	 * @throws Exception
	 */
	EHRResponse executeGetPatient(String theRequestId, 
			Citizen theCitizen) throws Exception;

	/**
	 * 
	 * @param theRequestId
	 * @param ehrPatientId
	 * 
	 * @return
	 * @throws Exception
	 */
	EHRFileResponse executeGetPatientSummary (String theRequestId, 
			String ehrPatientId) throws Exception;

	
	/**
	 * 
	 * @param from
	 * @param theRequestId
	 * @param ehrPatientId
	 * 
	 * @return
	 * @throws Exception
	 */
	EHRFileResponse executeSearchEncounter(
			Date theFromDate, 
			String theRequestId, 
			String ehrPatientId) throws Exception;
	
		
	/**
	 * 
	 * @param theEncounterId
	 * @param theRequestId
	 * @param ehrPatientId
	 * 
	 * @return
	 * @throws Exception
	 */
	EHRFileResponse executeEncounterEverything(
			String theEncounterId, 
			String theRequestId, 
			String ehrPatientId) throws Exception;
}
