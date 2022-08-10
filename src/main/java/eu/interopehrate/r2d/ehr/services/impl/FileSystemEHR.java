package eu.interopehrate.r2d.ehr.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.entity.ContentType;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.services.EHRService;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Dummy implementation of the EHRService based on retrieving files
 * from file system. It was used only for testing purposes.
 */

public class FileSystemEHR implements EHRService {

	private final static String PATIENT_ID = "patient0";
	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		EHRResponse ehrResponse = new EHRResponse();
		ehrResponse.setContentType(ContentType.TEXT_PLAIN);
		ehrResponse.setStatus(EHRResponseStatus.COMPLETED);
		ehrResponse.setResponse(PATIENT_ID);

		return ehrResponse;
	}

	@Override
	public EHRFileResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		return null;
	}

	@Override
	public EHRFileResponse executeSearchEncounter(Date theFromDate, String theRequestId, String ehrPatientId)
			throws Exception {
		
		return null;
	}

	@Override
	public EHRFileResponse executeEncounterEverything(String theEncounterId, String theRequestId, String ehrPatientId)
			throws Exception {
		// out
		String outFileName = Configuration.getDBPath() + "/" + theRequestId + ".xml";
		File outFile = new File(outFileName);
		
		try (InputStream in = FileSystemEHR.class.getClassLoader().getResourceAsStream(
				PATIENT_ID + "/" + theEncounterId + ".xml");
		     OutputStream out = new FileOutputStream(outFile)) {
			IOUtils.copy(in, out);
		} 
		
		EHRFileResponse ehrResponse = new EHRFileResponse();
		ehrResponse.setContentType(ContentType.APPLICATION_XML);
		ehrResponse.setStatus(EHRResponseStatus.COMPLETED);
		ehrResponse.addResponseFile(outFile);
		
		return ehrResponse;
	}

}
