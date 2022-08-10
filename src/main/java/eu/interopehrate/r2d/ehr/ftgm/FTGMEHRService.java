package eu.interopehrate.r2d.ehr.ftgm;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.EncounterEverythingItem;
import eu.interopehrate.r2d.ehr.services.impl.RestEHRService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Ad Hoc implementation of the RestEHRService for the EHR 
 * of FTGM. 
 * 
 *  By default, when requesting the encounter everything, the EHR should reply 
 *  with a list of items (composed by a type and a URL), where each one represent 
 *  a portion of the health data produced during the encounter. The EHR of FTGM does 
 *  not do this, when requesting the encounter everything, it returns direclty 
 *  the CDA file containing all data produced during the requested encounter.
 *  
 *  So this subclass of RestEHRService simulates that also the encounter everything 
 *  returns a list containing always 1 item.
 *
 */
public class FTGMEHRService extends RestEHRService {
	private static final Logger logger = LoggerFactory.getLogger(FTGMEHRService.class);

	@Override
	public EHRFileResponse executeEncounterEverything(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
		
		// #1 retrieves the list of components that are part of the encounter everything
		List<EncounterEverythingItem> items = retrieveEncounterEverythingItems(theEncounterId, 
				theRequestId, ehrPatientId);
		if (items.size() == 0) {
			throw new IllegalStateException("No items found in EHR for encounter: " + theEncounterId);
		}
		
		// #1 retrieves the each individual component
		EHRFileResponse encounterEverythingResponse = new EHRFileResponse();
		ContentType mime = ContentType.parse(Configuration.getProperty(Configuration.EHR_MIME));
		encounterEverythingResponse.setContentType(mime);

		EHRFileResponse individualItemResponse;
		for (EncounterEverythingItem item : items) {
			logger.debug("Requesting element '{}' of encounter {}", item.getType(), theEncounterId);
			individualItemResponse = downloadFileFromEHR(new URI(item.getUri()), theRequestId);
			if (individualItemResponse.getStatus() == EHRResponseStatus.FAILED) {
				encounterEverythingResponse.setMessage(individualItemResponse.getMessage());
				encounterEverythingResponse.setStatus(EHRResponseStatus.FAILED);
				return encounterEverythingResponse;
			} else {
				encounterEverythingResponse.addResponseFile(individualItemResponse.getResponseFile(0));
			}
		}
		
		encounterEverythingResponse.setStatus(EHRResponseStatus.COMPLETED);
		return encounterEverythingResponse;
	}
	
	protected List<EncounterEverythingItem> retrieveEncounterEverythingItems(String theEncounterId, String theRequestId, 
			String ehrPatientId) throws Exception {
				
		// #1 builds service URL
		String servicePath = Configuration.getProperty(GET_ENCOUNTER_SERVICE_NAME + ".PATH");
		if (servicePath == null || servicePath.isEmpty())
			throw new IllegalStateException("Operation getEncounterEverything not supported by EHR.");

		// #2 placeholders replacement
		servicePath = servicePath.replace(PATIENT_ID_PLACEHOLDER, ehrPatientId);
		servicePath = servicePath.replace(ENCOUNTER_ID_PLACEHOLDER, theEncounterId);
		URI serviceURI = createServiceURI(servicePath);
		
		EncounterEverythingItem item = new EncounterEverythingItem();
		item.setType("whole");
		item.setDescription("Entire content of Encounter " + theEncounterId);
		item.setUri(serviceURI.toString());

		List<EncounterEverythingItem> items = new ArrayList<EncounterEverythingItem>();
		items.add(item);
		
		return items;
	}
	
}
