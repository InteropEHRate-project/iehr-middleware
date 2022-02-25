package eu.interopehrate.r2d.ehr.old.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.r2d.ehr.model.ContentType;
import eu.interopehrate.r2d.ehr.model.EHRMWException;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;

@Deprecated
class RequestToIHSSimulatorActivity extends Activity {

	private static FhirContext fhirContext;

	private final IParser parser;
	
	
	public RequestToIHSSimulatorActivity() {
		super();
		if (fhirContext == null)
			fhirContext = FhirContext.forR4();

    	parser = fhirContext.newJsonParser();
	}


	@Override
	protected void processRequest(EHRRequest request) throws Exception {		
		Thread.sleep(5000);

		EHRResponse response = null;
		if (request.getOperation() == R2DOperation.SEARCH_ENCOUNTER) {
			StringBuilder fileName = new StringBuilder(request.getCitizen().getPersonIdentifier());
			fileName.append("/FHIR/EncounterList.json");
			
			response = createResponse(fileName.toString());
		} else if (request.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING) {
			StringBuilder fileName = new StringBuilder(request.getCitizen().getPersonIdentifier());
			fileName.append("/FHIR/Encounter");
			
			String encounterId = (String)request.getParameter(EHRRequest.PARAM_RESOURCE_ID);
			fileName.append(encounterId).append("$everything.json");
			
			response = createResponse(fileName.toString());
		} else
			throw new NotImplementedException("Operation " + request.getOperation() +
					" not implemented.");
		
		// Adds result to context execution
		if (response != null) {
			if (response.getStatus() == EHRResponseStatus.FAILED) {
				throw new EHRMWException(response.getMessage());
			} 
			ehrRequest.setIhsResponse(response);
		}
	}
	
	
	@Override
	public String getName() {
		return "RequestToIHSTask";
	}


	private EHRResponse createResponse(String fileName) throws IOException {
		InputStream result = getClass().getClassLoader().getResourceAsStream(fileName.toString());

		EHRResponse response = new EHRResponse(ContentType.JSON_FHIR, EHRResponseStatus.COMPLETED);
		if (result == null) {
			response.setMessage("No content found.");
			response.setResponse(parser.encodeResourceToString(new Bundle()));
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
