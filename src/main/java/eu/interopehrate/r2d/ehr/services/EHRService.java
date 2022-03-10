package eu.interopehrate.r2d.ehr.services;

import java.util.Date;

import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public interface EHRService {

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
	EHRResponse executeGetPatientSummary (String theRequestId, 
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
	EHRResponse executeSearchEncounter(
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
	EHRResponse executeEncounterEverything(
			String theEncounterId, 
			String theRequestId, 
			String ehrPatientId) throws Exception;
		
	
}
