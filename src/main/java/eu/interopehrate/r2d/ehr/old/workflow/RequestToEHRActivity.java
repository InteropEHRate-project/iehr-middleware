package eu.interopehrate.r2d.ehr.old.workflow;

import java.util.Date;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRMWException;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;
import eu.interopehrate.r2d.ehr.services.EHRService;

@Deprecated
class RequestToEHRActivity extends Activity {
	
	@Autowired
	private EHRService ehrPlugin;
	
	@Override
	protected void processRequest(EHRRequest request) throws Exception {		
		EHRResponse response = null;
		if (request.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			
			response = ehrPlugin.executeSearchEncounter(
					(Date)request.getParameter(EHRRequest.PARAM_FROM),
					request.getR2dRequestId(), 
					request.getCitizen());
			
		} else if (request.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			
			response = ehrPlugin.executeEncounterEverything(
					(String)request.getParameter(EHRRequest.PARAM_RESOURCE_ID), 
					request.getR2dRequestId(), 
					request.getCitizen());
		
		} else if (request.getOperation() == R2DOperation.PATIENT_SUMMARY) {
			
			response = ehrPlugin.executeGetPatientSummary(
					request.getR2dRequestId(), 
					request.getCitizen());
		
		} else
			throw new NotImplementedException("Operation " + request.getOperation() +
					" not implemented.");
		
		// Adds result to context execution
		if (response != null) {
			if (response.getStatus() == EHRResponseStatus.FAILED) {
				throw new EHRMWException(response.getMessage());
			} 
			ehrRequest.setEhrResponse(response);
		}
		
	}
	
}
