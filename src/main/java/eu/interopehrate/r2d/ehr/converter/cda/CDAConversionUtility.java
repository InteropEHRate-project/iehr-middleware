package eu.interopehrate.r2d.ehr.converter.cda;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Media.MediaStatus;
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

public class CDAConversionUtility {
	public static final String HTTP_UNITSOFMEASURE_ORG = "http://unitsofmeasure.org";
    public static final String ICD9_SYSTEM = "http://hl7.org/fhir/sid/icd-9-cm"; // 2.16.840.1.113883.6.2
    public static final String LOINC_SYSTEM = "http://loinc.org"; // 2.16.840.1.113883.6.1

    private static final String DEFAULT_LANG = "en";
	private static SimpleDateFormat YYYYMdd_FORMATTER = new SimpleDateFormat("yyyyMMdd");
	// 20210520090000
	private static SimpleDateFormat yyyyMMddHHmmss_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss");

	
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
		        
		Meta profile = new Meta();
        profile.addProfile("http://interopehrate.eu/fhir/StructureDefinition/DiagnosticReport-LaboratoryReport-IEHR");
        labRep.setMeta(profile);
		labRep.setId("labRep_" + code.replace("-", "_") + "_" + effectiveTime.substring(0, 14));
        labRep.setStatus(DiagnosticReportStatus.FINAL);
		// sets the code
        
		Coding coding = new Coding(toCodingSystemURL(codeSystem), code, label);
		labRep.setCode(new CodeableConcept(coding));
        // sets the category
        Coding cat = new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB", "");
        labRep.addCategory(new CodeableConcept(cat));
        labRep.setSubject(new Reference(subject));
        labRep.setLanguage("en");
        labRep.addPerformer(new Reference(author));
        labRep.addResultsInterpreter(new Reference(author));
        labRep.setEncounter(new Reference(encounter));
		
		return labRep;
	}
	

	static DiagnosticReport createImageReport(Patient subject, String effectiveTime,
			String codeSystem, String code, String label, Practitioner author, Encounter encounter) {
		DiagnosticReport imgRep = new DiagnosticReport();
		
		Meta profile = new Meta();
        profile.addProfile("http://interopehrate.eu/fhir/StructureDefinition/DiagnosticReport-ImagingReport-IEHR");
        imgRep.setMeta(profile);
        imgRep.setId("imgRep_" + code.replace("-", "_") + "_" + effectiveTime.substring(0, 14));
        imgRep.setStatus(DiagnosticReportStatus.FINAL);
		// sets the code
		Coding coding = new Coding(toCodingSystemURL(codeSystem), code, label);
		imgRep.setCode(new CodeableConcept(coding));
        // sets the category        
        Coding cat = new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "RAD", "");
        imgRep.addCategory(new CodeableConcept(cat));
        imgRep.setSubject(new Reference(subject));
        imgRep.setLanguage("en");
        imgRep.addPerformer(new Reference(author));
        imgRep.addResultsInterpreter(new Reference(author));
        imgRep.setEncounter(new Reference(encounter));
		
		return imgRep;
	}
	
	
	static Observation toLaboratory(Node obsNode, String effectiveTime, 
			Patient subject, Practitioner practitioner) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/uv/ips/StructureDefinition/Observation-results-laboratory-uv-ips");
		obs.setMeta(meta);
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
		Coding coding = new Coding(toCodingSystemURL(codeSystem), code, label);
		obs.setCode(new CodeableConcept(coding));
		
		obs.setId("obs_" + code.replace("-", "_") + "_" + effectiveTime.substring(0, 14));
		
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
			Patient subject, Practitioner practitioner) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
		meta.addProfile("http://hl7.org/fhir/StructureDefinition/vitalsigns");
		obs.setMeta(meta);
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));

		obs.addCategory(new CodeableConcept(new Coding(
        		"http://terminology.hl7.org/CodeSystem/observation-category", 
        		"vital-signs", 
        		"Parametri Vitali")));
        
        // date
        DateTimeType date = new DateTimeType();
        date.setValue(yyyyMMddHHmmss_FORMATTER.parse(effectiveTime.substring(0, 14)));
        obs.setEffective(date);

		// code
		Node codeNode = getChildByName(vSignNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
		Coding coding = new Coding(toCodingSystemURL(codeSystem), code, label);
		obs.setCode(new CodeableConcept(coding));
		
		obs.setId("obs_" + code.replace("-", "_") + "_" + effectiveTime.substring(0, 14));
		
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
	
	
	public static Observation toBasicObservation(Node obsNode, String effectiveTime, 
			Patient subject, Practitioner practitioner) throws ParseException {
		Observation obs = new Observation();
		Meta meta = new Meta();
		meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Observation-IEHR");
		obs.setMeta(meta);
		obs.setStatus(ObservationStatus.FINAL);
		obs.setSubject(new Reference(subject));
        
        // date
        DateTimeType date = new DateTimeType();
        date.setValue(yyyyMMddHHmmss_FORMATTER.parse(effectiveTime.substring(0, 14)));
        obs.setEffective(date);

		// code
		Node codeNode = getChildByName(obsNode, "code");
		String code = ((Element)codeNode).getAttribute("code");
		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
		Coding coding = new Coding(toCodingSystemURL(codeSystem), code, label);
		obs.setCode(new CodeableConcept(coding));
		
		obs.setId("obs_" + code.replace("-", "_") + "_" + effectiveTime.substring(0, 14));
		
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
	
	
	static Encounter toEncounter(Node cdaEncounter, Patient subject) throws ParseException {
		Encounter e = new Encounter();
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Encounter-IEHR");
        e.setMeta(meta);
        e.setStatus(EncounterStatus.FINISHED);
        
        // language
        e.setLanguage(DEFAULT_LANG);
        
		// class
		e.setClass_(new Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "AMB", "ambulatory"));

		// id
		Node id = getChildByName(cdaEncounter, "id");
		e.setId(((Element)id).getAttribute("extension"));
		Identifier idf = new Identifier();
		idf.setId(((Element)id).getAttribute("extension"));
		e.addIdentifier(idf);
		
		// effectiveDateTime 20220224		
		Node effectiveTime = getChildByName(cdaEncounter, "effectiveTime");
		Period period = new Period();
		
		if (((Element)effectiveTime).hasAttribute("value")) {
			period.setStart(
					YYYYMdd_FORMATTER.parse(((Element)effectiveTime).getAttribute("value")),
					TemporalPrecisionEnum.DAY);
			e.setPeriod(period);
		} else {
			Node lowNode = getChildByName(effectiveTime, "low");
			period.setStart(YYYYMdd_FORMATTER.parse(((Element)lowNode).getAttribute("value")));
			e.setPeriod(period);
		}

		// subject
		e.setSubject(new Reference(subject));
		
		return e;
	}
	
	
	public static Media toMedia(Node mediaNode, Patient subject, 
			Practitioner practitioner, Encounter encounter) {
		Media media = new Media();
        Node codeNode = getChildByName(mediaNode, "code");
        String code = ((Element)codeNode).getAttribute("code");
        media.setId("media_" + code.replace("-", "_") + "_" + System.currentTimeMillis());
		
		// profile
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Media-IEHR");
        media.setMeta(meta);
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
			// System.out.println(children.item(j));
			if (childName.equals(children.item(j).getLocalName()))
				return children.item(j);
		}

		return null;
	}

}
