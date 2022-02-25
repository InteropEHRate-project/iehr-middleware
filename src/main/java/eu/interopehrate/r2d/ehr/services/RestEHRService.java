package eu.interopehrate.r2d.ehr.services;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRResponse;

public class RestEHRService implements EHRService {
	private static final Log logger = LogFactory.getLog(RestEHRService.class);

	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		return null;
	}

	
	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, Citizen theCitizen) throws Exception {
		return null;
	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, String theRequestId, 
			Citizen theCitizen) throws Exception {
		logger.debug(String.format("Executing Encounter.search..."));
		
		// builds callback URL
		StringBuilder callbackURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		callbackURL.append("/callbacks/").append(theRequestId);
		
		return null;
	}
	

	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			Citizen theCitizen) throws Exception {
		logger.debug(String.format("Executing Encounter/%s/$everything", theEncounterId));

		// builds callback URL
		StringBuilder callbackURL = new StringBuilder(Configuration.getR2DServicesContextPath());
		callbackURL.append("/callbacks/").append(theRequestId);
		
		return null;
	}
	
}
