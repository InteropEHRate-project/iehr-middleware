package eu.interopehrate.r2d.ehr.cda.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CarePlan.CarePlanIntent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Media.MediaStatus;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.MedicationStatement.MedicationStatementStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import eu.interopehrate.r2d.ehr.Configuration;

public class CDAConversionUtility {
	public static final String HTTP_UNITSOFMEASURE_ORG = "http://unitsofmeasure.org";
	
    public static final String ICD9_SYSTEM = "http://hl7.org/fhir/sid/icd-9-cm"; // 2.16.840.1.113883.6.2
    public static final String LOINC_SYSTEM = "http://loinc.org"; // 2.16.840.1.113883.6.1

	private static SimpleDateFormat YYYYMMdd_FORMATTER = new SimpleDateFormat("yyyyMMdd");
	// 20210520090000
	private static SimpleDateFormat yyyyMMddHHmmss_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss");

	private static CDACodeTranslator translator = new CDACodeTranslator();
	
	static Patient toPatient(Node cdaPatient) {
		Patient p = new Patient();
		
		Node id = getChildByName(cdaPatient, "id");
		p.setId(((Element)id).getAttribute("extension"));
		Identifier idf = new Identifier();
		idf.setId(((Element)id).getAttribute("extension"));
		p.addIdentifier(idf);

		return p;
	}
	
	
	static Practitioner toPractitioner(Node cdaPatient) {
		Practitioner p = new Practitioner();
		
		NodeList ids = ((Element)cdaPatient).getElementsByTagName("id");
		
		p.setId(((Element)ids.item(0)).getAttribute("extension"));
		Identifier idf = new Identifier();
		idf.setId(((Element)ids.item(0)).getAttribute("extension"));
		p.addIdentifier(idf);
		
		// name
		HumanName hName = new HumanName();
		NodeList descendants = ((Element)cdaPatient).getElementsByTagName("given");
		if (descendants.getLength() > 0)
			hName.addGiven(descendants.item(0).getFirstChild().getNodeValue());
		
		// surname
		descendants = ((Element)cdaPatient).getElementsByTagName("family");
		if (descendants.getLength() > 0)
			hName.setFamily(descendants.item(0).getFirstChild().getNodeValue());
		
		p.addName(hName);

		return p;
	}
	
	
	static DiagnosticReport createLaboratoryReport(Patient subject, String effectiveTime,
			String codeSystem, String code, String label, Practitioner author, Encounter encounter) {
		DiagnosticReport labRep = new DiagnosticReport();
		// id
		labRep.setId("labRep_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		labRep.addIdentifier(new Identifier().setValue(labRep.getIdElement().getIdPart()));

		// profile
		Meta profile = new Meta();
        profile.addProfile("http://interopehrate.eu/fhir/StructureDefinition/DiagnosticReport-LaboratoryReport-IEHR");
        labRep.setMeta(profile);
 
        // status
		labRep.setStatus(DiagnosticReportStatus.FINAL);
        
		// sets the code
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);		
		labRep.setCode(new CodeableConcept(coding));
		
        // sets the category
        Coding cat = new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB", "");
        labRep.addCategory(new CodeableConcept(cat));
        labRep.setSubject(new Reference(subject));
        labRep.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        labRep.addPerformer(new Reference(author));
        labRep.addResultsInterpreter(new Reference(author));
        labRep.setEncounter(new Reference(encounter));
		
		return labRep;
	}
	

	static DiagnosticReport createImageReport(Patient subject, String effectiveTime,
			String codeSystem, String code, String label, Practitioner author, Encounter encounter) {
		DiagnosticReport imgRep = new DiagnosticReport();
		// id
        imgRep.setId("imgRep_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
        imgRep.addIdentifier(new Identifier().setValue(encounter.getIdElement().getIdPart()));
		
		Meta profile = new Meta();
        profile.addProfile("http://interopehrate.eu/fhir/StructureDefinition/DiagnosticReport-ImagingReport-IEHR");
        imgRep.setMeta(profile);
        imgRep.setStatus(DiagnosticReportStatus.FINAL);
		// sets the code
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		imgRep.setCode(new CodeableConcept(coding));
        // sets the category        
        Coding cat = new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "RAD", "");
        imgRep.addCategory(new CodeableConcept(cat));
        imgRep.setSubject(new Reference(subject));
        imgRep.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        imgRep.addPerformer(new Reference(author));
        imgRep.addResultsInterpreter(new Reference(author));
        imgRep.setEncounter(new Reference(encounter));
		
		return imgRep;
	}
	
	
	static Observation toLaboratory(Node obsNode, String effectiveTime, Patient subject, 
			Practitioner practitioner, DiagnosticReport diagnosticReport) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/uv/ips/StructureDefinition/Observation-results-laboratory-uv-ips");
		obs.setMeta(meta);
        obs.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));
		
        CodeableConcept cc = new CodeableConcept(new Coding(ObservationCategory.LABORATORY.getSystem(),
        		ObservationCategory.LABORATORY.toCode(), 
        		ObservationCategory.LABORATORY.getDisplay()));
        obs.addCategory(cc);
        
        // date
        DateTimeType date = new DateTimeType();
        date.setValue(yyyyMMddHHmmss_FORMATTER.parse(effectiveTime.substring(0, 14)));
        obs.setEffective(date);

		// code
		Node codeNode = getChildByName(obsNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		obs.setCode(new CodeableConcept(coding));
		
		if (diagnosticReport != null)
			obs.setId("obs_" + code.replace("-", "_") + "_" + diagnosticReport.getIdElement().getIdPart());
		else
			obs.setId("obs_" + code.replace("-", "_"));
		obs.addIdentifier(new Identifier().setValue(obs.getIdElement().getIdPart()));
		
		// value
		Node valueNode = getChildByName(obsNode, "value");
		Quantity value = new Quantity();
		value.setValue(Double.valueOf(((Element)valueNode).getAttribute("value")));
		value.setUnit(((Element)valueNode).getAttribute("unit"));
		value.setSystem(HTTP_UNITSOFMEASURE_ORG);
		value.setCode(((Element)valueNode).getAttribute("unit"));
		obs.setValue(value);
		
		// performer
		obs.addPerformer(new Reference(practitioner));
		
		return obs;
	}
	
	
	static Observation toVitalSign(Node vSignNode, String effectiveTime, 
			Patient subject, Practitioner practitioner, Encounter encounter) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
		meta.addProfile("http://hl7.org/fhir/StructureDefinition/vitalsigns");
		obs.setMeta(meta);
        obs.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));

		obs.addCategory(new CodeableConcept(new Coding(
        		"http://terminology.hl7.org/CodeSystem/observation-category", 
        		"vital-signs", 
        		"Vital Signs")));
        
        // date
        DateTimeType date = new DateTimeType();
        date.setValue(yyyyMMddHHmmss_FORMATTER.parse(effectiveTime.substring(0, 14)));
        obs.setEffective(date);

		// code
		Node codeNode = getChildByName(vSignNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		obs.setCode(new CodeableConcept(coding));

		obs.setId("obs_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		obs.addIdentifier(new Identifier().setValue(obs.getIdElement().getIdPart()));
		
		// value
		Node valueNode = getChildByName(vSignNode, "value");
		Quantity value = new Quantity();
		value.setValue(Double.valueOf(((Element)valueNode).getAttribute("value")));
		value.setUnit(((Element)valueNode).getAttribute("unit"));
		value.setSystem(HTTP_UNITSOFMEASURE_ORG);
		value.setCode(((Element)valueNode).getAttribute("unit"));
		obs.setValue(value);
		
		// performer
		obs.addPerformer(new Reference(practitioner));

		return obs;
	}
	
	
	static Observation toBasicObservation(Node obsNode, String effectiveTime, 
			Patient subject, Practitioner practitioner, DiagnosticReport diagnosticReport) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
		meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Observation-IEHR");
		obs.setMeta(meta);
        obs.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));
        
		// code
		Node codeNode = getChildByName(obsNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		obs.setCode(new CodeableConcept(coding));
		
		// id
		if (diagnosticReport != null)
			obs.setId("obs_" + code.replace("-", "_") + "_" + diagnosticReport.getIdElement().getIdPart());
		else
			obs.setId("obs_" + code.replace("-", "_"));

		obs.addIdentifier(new Identifier().setValue(obs.getIdElement().getIdPart()));

		// date
        DateTimeType date = new DateTimeType();
        date.setValue(yyyyMMddHHmmss_FORMATTER.parse(effectiveTime.substring(0, 14)));
        obs.setEffective(date);
		
		// value
		Node valueNode = getChildByName(obsNode, "value");
		Quantity value = new Quantity();
		value.setValue(Double.valueOf(((Element)valueNode).getAttribute("value")));
		value.setUnit(((Element)valueNode).getAttribute("unit"));
		value.setSystem(HTTP_UNITSOFMEASURE_ORG);
		value.setCode(((Element)valueNode).getAttribute("unit"));
		obs.setValue(value);
		
		// performer
		obs.addPerformer(new Reference(practitioner));
		
		return obs;
	}
	
	public static Observation toPhysicalObservation(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		Observation obs = toTextualReason(sectionNode, subject, practitioner, encounter);

		// code
		Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? "Reason for visit Narrative" : transLabel);
		obs.setCode(new CodeableConcept(coding));

		return obs;
	}
	
	
	public static Observation toReason(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		Observation obs = toTextualReason(sectionNode, subject, practitioner, encounter);

		// code
		Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? "Physical findings Narrative" : transLabel);
		obs.setCode(new CodeableConcept(coding));
	
		return obs;
	}
	
	
	private static Observation toTextualReason(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		Observation obs = new Observation();
		Meta meta = new Meta();
		meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Observation-IEHR");
		obs.setMeta(meta);
        obs.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));
        
		// code
		Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		obs.setCode(new CodeableConcept(coding));

		// id 
		obs.setId("obs_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		obs.addIdentifier(new Identifier().setValue(obs.getIdElement().getIdPart()));

		// date
        DateTimeType date = new DateTimeType();
        date.setValue(encounter.getPeriod().getStart());
        obs.setEffective(date);
				
		// performer
		obs.addPerformer(new Reference(practitioner));
		
		// note
        Node textNode = getChildByName(sectionNode, "text");
        obs.addNote().setText(textNode.getFirstChild().getNodeValue());
		
		return obs;
	}
	
	
	static Encounter toEncounter(Node cdaEncounter, Patient subject) throws ParseException {
		Encounter e = new Encounter();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Encounter-IEHR");
        e.setMeta(meta);
        e.setStatus(EncounterStatus.FINISHED);
        
        // language
        e.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        
		// class
		e.setClass_(new Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "AMB", "ambulatory"));

		// id
		Node id = getChildByName(cdaEncounter, "id");		
		e.setId(((Element)id).getAttribute("extension"));
	    e.addIdentifier(new Identifier().setValue(((Element)id).getAttribute("extension")));
		
		// effectiveDateTime 20220224		
		Node effectiveTime = getChildByName(cdaEncounter, "effectiveTime");
		Period period = new Period();
		
		if (((Element)effectiveTime).hasAttribute("value")) {
			period.setStart(
					YYYYMMdd_FORMATTER.parse(((Element)effectiveTime).getAttribute("value")),
					TemporalPrecisionEnum.DAY);
			e.setPeriod(period);
		} else {
			Node lowNode = getChildByName(effectiveTime, "low");
			period.setStart(YYYYMMdd_FORMATTER.parse(((Element)lowNode).getAttribute("value")));
			e.setPeriod(period);
		}

		// subject
		e.setSubject(new Reference(subject));
		
		return e;
	}
	
	
	public static Media toMedia(Node mediaNode, Patient subject, Practitioner practitioner,
			Encounter encounter, DiagnosticReport diagnosticReport) {
		Media media = new Media();
        Node codeNode = getChildByName(mediaNode, "code");
        String code = ((Element)codeNode).getAttribute("code");
        if (diagnosticReport != null)
            media.setId("media_" + code.replace("-", "_") + "_" + diagnosticReport.getIdElement().getIdPart());
        else
        	media.setId("media_" + code.replace("-", "_"));
        
        media.addIdentifier(new Identifier().setValue(media.getIdElement().getIdPart()));
		
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Media-IEHR");
        media.setMeta(meta);
        media.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        media.setStatus(MediaStatus.COMPLETED);
        
        media.setSubject(new Reference(subject));
        media.setOperator(new Reference(practitioner));
        media.setEncounter(new Reference(encounter));
        
        // text
        Node textNode = getChildByName(mediaNode, "text");
        if (textNode.getFirstChild() != null) {
	        String text = textNode.getFirstChild().getNodeValue();
	        if (text != null) {
		        text = text.replace('\n', ' ');
		        text = text.replaceAll("  ", "");
		        media.addNote().setText(text);
	        }
        }
        
        // data
        Node valueNode = getChildByName(mediaNode, "value");
        if (valueNode.getFirstChild() != null) {
        	Attachment attachment = new Attachment();
        	attachment.setContentType(((Element)valueNode).getAttribute("mediaType"));
        	
        	//attachment
        	Base64BinaryType image = new Base64BinaryType();
        	image.setValueAsString(valueNode.getFirstChild().getNodeValue());
        	attachment.setDataElement(image);
        	attachment.setSize(image.getValue().length);
        	media.setContent(attachment);
        }        
        
		
		return media;
	}
	
	
	public static Condition toCondition(Node obsNode, Patient subject, 
			Practitioner practitioner, Encounter encounter) {
		Condition c = new Condition();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Condition-IEHR");
        c.setMeta(meta);
        c.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        c.setSubject(new Reference(subject));
        // code
        Node codeNode = getChildByName(obsNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? label : transLabel);
		c.setCode(new CodeableConcept(coding));
		
		// id
		c.setId("cond_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		c.addIdentifier(new Identifier().setValue(c.getIdElement().getIdPart()));
		
		// clinical status
		coding = new Coding("http://hl7.org/fhir/ValueSet/condition-clinical", "active", "Active");
		c.setClinicalStatus(new CodeableConcept(coding));
		
		// verification status
		coding = new Coding("http://hl7.org/fhir/ValueSet/condition-ver-status", "confirmed", "Confirmed");
		c.setVerificationStatus(new CodeableConcept(coding));
		// onset date
		c.setOnset(new DateTimeType(encounter.getPeriod().getStart()));
        
		return c;
	}
	

	public static Condition toAnamnesi(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		Condition anamnesi = toDiagnosticConclusion(sectionNode, subject, practitioner, encounter);
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Condition-IEHR");
        anamnesi.setMeta(meta);

        // code
        Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? "History general Narrative - Reported" : transLabel);
		anamnesi.setCode(new CodeableConcept(coding));

		// clinical status
		Coding c = new Coding("http://hl7.org/fhir/ValueSet/condition-clinical", "active", "Active");
		anamnesi.setClinicalStatus(new CodeableConcept(c));
		
		// verification status
		c = new Coding("http://hl7.org/fhir/ValueSet/condition-ver-status", "confirmed", "Confirmed");
		anamnesi.setVerificationStatus(new CodeableConcept(c));

        return anamnesi;
	}
	
	
	public static Condition toDiagnosticConclusion(Node sectionNode, Patient subject, 
			Practitioner practitioner, Encounter encounter) {
		
		Condition c = new Condition();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/DiagnosticConclusion-IEHR");
        c.setMeta(meta);
        c.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        c.setSubject(new Reference(subject));
        // code
        Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? "Conclusions [Interpretation] Document" : transLabel);
		c.setCode(new CodeableConcept(coding));

		// id
		c.setId("cond_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		c.addIdentifier(new Identifier().setValue(c.getIdElement().getIdPart()));

		// onset date
		c.setOnset(new DateTimeType(encounter.getPeriod().getStart()));
		// conclusions
        Node textNode = getChildByName(sectionNode, "text");
		c.addNote().setText(textNode.getFirstChild().getNodeValue());
        
		return c;
	}
	
	
	public static CarePlan toTreatmentPlan(Node sectionNode, Patient subject, 
			Practitioner practitioner, Encounter encounter) {
		CarePlan cP = new CarePlan();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/TreatmentPlan-IEHR");
        cP.setMeta(meta);
        cP.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        cP.setSubject(new Reference(subject));
        // code
        Node codeNode = getChildByName(sectionNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
        String systemURL = toCodingSystemURL(codeSystem);
        String transLabel = translator.translateCodeLabel(systemURL, code);
		Coding coding = new Coding(systemURL, code, transLabel == null ? "Plan of care note" : transLabel);
		cP.addCategory(new CodeableConcept(coding));

		// id
		cP.setId("careplan_" + code.replace("-", "_") + "_" + encounter.getIdElement().getIdPart());
		cP.addIdentifier(new Identifier().setValue(cP.getIdElement().getIdPart()));
		// onset date
		cP.setCreated(encounter.getPeriod().getStart());
		// intent
		cP.setIntent(CarePlanIntent.PLAN);
		// author
		cP.setAuthor(new Reference(practitioner));
		// encounter
		cP.setEncounter(new Reference(encounter));
		// instructions
        Node textNode = getChildByName(sectionNode, "text");

        String text = textNode.getFirstChild().getNodeValue();
        if (text != null) {
	        text = text.replace('\n', ' ');
	        text = text.replaceAll("  ", "");
			cP.setDescription(text);	        
        }
        
		return cP;
	}
	

	public static AllergyIntolerance toAllergyIntolerance(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		AllergyIntolerance ai = new AllergyIntolerance();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/AllergyIntolerance-IEHR");
        ai.setMeta(meta);
        ai.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        ai.setPatient(new Reference(subject));	
        
        // id
        ai.setId("ai_373067005_" + encounter.getIdElement().getIdPart());
        ai.addIdentifier(new Identifier().setValue(ai.getIdElement().getIdPart()));
		
        // code is fixed
        Coding c = new Coding("http://snomed.info/sct", "373067005", "no allergy");
		ai.setCode(new CodeableConcept(c));

		// clinical status
		c = new Coding("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "active", "Active");
		ai.setClinicalStatus(new CodeableConcept(c));
		
		// verification status
		c = new Coding("http://hl7.org/fhir/ValueSet/allergyintolerance-verification", "confirmed", "Confirmed");
		ai.setVerificationStatus(new CodeableConcept(c));
		
		// text
        Node textNode = getChildByName(sectionNode, "text");
		ai.addNote().setText(textNode.getFirstChild().getNodeValue());
		
		// encounter
		// ai.setEncounter(new Reference(encounter));
		
		// author
		// ai.setRecorder(new Reference(practitioner));
		
		// recordedDate
		//ai.setRecordedDate(encounter.getPeriod().getStart())
		
		return ai;
	}

	public static MedicationStatement toMedication(Node sectionNode, Patient subject, Practitioner practitioner,
			Encounter encounter) {
		MedicationStatement ms = new MedicationStatement();
		
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/MedicationStatement-IEHR");
        ms.setMeta(meta);
        ms.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
        ms.setSubject(new Reference(subject));	
        ms.setStatus(MedicationStatementStatus.ACTIVE);
        
        // id
        ms.setId("medstm_" + randomString() + "_" + encounter.getIdElement().getIdPart());
        ms.addIdentifier(new Identifier().setValue(ms.getIdElement().getIdPart()));
        
        // effective
        ms.setEffective(encounter.getPeriod().getStartElement());
        
        // dosage
        Dosage d = new Dosage();
        Node textNode = getChildByName(sectionNode, "text");
		d.setText(textNode.getFirstChild().getNodeValue());
		ms.addDosage(d);
		
		return ms;
	}

	private static String toCodingSystemURL(String codeSystemCode) {
		if ("2.16.840.1.113883.6.2".equals(codeSystemCode))
			return ICD9_SYSTEM;
		
		if ("2.16.840.1.113883.6.1".equals(codeSystemCode))
			return LOINC_SYSTEM;
		
		return null;
	}

		
	static Node getChildByName(Node parent, String childName) {
		NodeList children = parent.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			if (childName.equals(children.item(j).getLocalName()))
				return children.item(j);
		}

		return null;
	}

	
	static String randomString() {
	    int leftLimit = 48; // numeral '0'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;
	    Random random = new Random();

	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();
	    
	    return generatedString;
	}

	
}
