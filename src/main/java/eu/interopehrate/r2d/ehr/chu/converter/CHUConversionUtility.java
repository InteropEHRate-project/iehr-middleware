package eu.interopehrate.r2d.ehr.chu.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import eu.interopehrate.r2d.ehr.Configuration;

public class CHUConversionUtility {

	// 2022-01-01
	private static SimpleDateFormat YYYY_MM_dd_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	static Encounter toEncounter(CHUEncounter chuEnc, Patient subject) throws ParseException {
		Encounter e = new Encounter();

		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Encounter-IEHR");
        e.setMeta(meta);
        e.setStatus(EncounterStatus.FINISHED);
        
		// id
		e.setId(chuEnc.getId());
		Identifier idf = new Identifier();
		idf.setId(chuEnc.getId());
		e.addIdentifier(idf);

        // language
        e.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        
		// class
 		e.setClass_(new Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "AMB", "ambulatory"));
		
		// effectiveDateTime		
		Period period = new Period();
		period.setStart(YYYY_MM_dd_FORMATTER.parse(chuEnc.getDate()), 
				TemporalPrecisionEnum.DAY);
		e.setPeriod(period);
		
		// new Coding("http://terminology.hl7.org/CodeSystem/service-type", "AMB", "ambulatory");
		
		// subject
		e.setSubject(new Reference(subject));
		
		return e;
	}
	
}
