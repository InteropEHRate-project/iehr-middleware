package eu.interopehrate.r2d.ehr.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.http.entity.ContentType;

import eu.interopehrate.r2d.ehr.model.Citizen;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;

public class FileSystemEHRService implements EHRService {
	private static final int SLEEP_PERIOD = 10000;
	
	@Override
	public EHRResponse executeGetPatient(String theRequestId, Citizen theCitizen) throws Exception {
		return null;
	}

	@Override
	public EHRResponse executeGetPatientSummary(String theRequestId, String ehrPatientId) throws Exception {
		Thread.sleep(SLEEP_PERIOD);

		StringBuilder fileName = new StringBuilder(ehrPatientId);
		fileName.append("/CDA/PatientSummary.xml");
		
		return createResponse(fileName.toString());
	}

	
	@Override
	public EHRResponse executeSearchEncounter(Date theFromDate, 
			String theRequestId, String ehrPatientId) throws Exception {

		Thread.sleep(SLEEP_PERIOD);
		
		StringBuilder fileName = new StringBuilder(ehrPatientId);
		fileName.append("/CDA/EncounterList.xml");
		
		return createResponse(fileName.toString());
	}

	
	@Override
	public EHRResponse executeEncounterEverything(String theEncounterId, 
			String theRequestId, String ehrPatientId) throws Exception {

		Thread.sleep(SLEEP_PERIOD);

		StringBuilder fileName = new StringBuilder(ehrPatientId);
		fileName.append("/CDA/Encounter").append(theEncounterId).append("$everything.xml");
		
		return createResponse(fileName.toString());
	}
	
	
	private EHRResponse createResponse(String fileName) throws IOException {
		InputStream result = getClass().getClassLoader().getResourceAsStream(fileName.toString());

		EHRResponse response = new EHRResponse(ContentType.APPLICATION_XML, EHRResponseStatus.COMPLETED);
		if (result == null) {
			response.setMessage("No content found.");
			response.setResponse("");
		} else {
			StringBuilder fileContent = readFile(result);
			response.setResponse(fileContent.toString());
		}
		
		return response;
	}
	
	/**
	 * Read a file from the FS
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private StringBuilder readFile(InputStream is) throws IOException {
		
		StringBuilder sb = new StringBuilder(1024*10);
		String tmp;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		try {
			while ((tmp = reader.readLine()) != null) {
				sb.append(tmp);
			}
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			reader.close();
		}
		
		return sb;
	}

}
