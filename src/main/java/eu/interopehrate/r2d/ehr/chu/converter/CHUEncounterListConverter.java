package eu.interopehrate.r2d.ehr.chu.converter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ca.uhn.fhir.parser.IParser;

import java.lang.reflect.Type;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRMWServer;
import eu.interopehrate.r2d.ehr.converter.Converter;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: local converter for a list of encounters.
 */
public class CHUEncounterListConverter implements Converter {

	@Override
	public void convert(File input, File output, 
			Map<String, String> properties) throws Exception {
		
		// #1 Creates resulting Bundle
		Bundle bundle = new Bundle();
		bundle.setType(BundleType.SEARCHSET);
		bundle.setTimestamp(new Date());
		bundle.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		bundle.setId(UUID.randomUUID().toString());
		
		// #2 Creates the patient
		Patient p = new Patient();
		p.setId(properties.get(Converter.PATIENT_ID_KEY));
		Identifier idf = new Identifier();
		idf.setId(properties.get(Converter.PATIENT_ID_KEY));
		p.addIdentifier(idf);
		// bundle.addEntry().setResource(p);

		// Parsing input file
		Gson gson = new Gson();
		Type encounterListType = new TypeToken<List<CHUEncounter>>(){}.getType();
		List<CHUEncounter> encounters = gson.fromJson(new FileReader(input), encounterListType);
		
		Encounter fhirEnc;
		for (CHUEncounter e : encounters) {
			fhirEnc = CHUConversionUtility.toEncounter(e, p);
			bundle.addEntry().setResource(fhirEnc);
		}
		
		// #7 stores bundle to file
		final IParser parser = EHRMWServer.FHIR_CONTEXT.newJsonParser();
		parser.encodeResourceToWriter(bundle, new FileWriter(output));
	}

}
