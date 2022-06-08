package eu.interopehrate.r2d.ehr.converter.cda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRMWServer;
import eu.interopehrate.r2d.ehr.converter.Converter;


public class CDAEncounterEverythingConverter implements Converter {
	
	@Override
	public void convertToFile(File input, File output) throws Exception {
		// #1 Parse XML CDA input file
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		
		Document xmlDocument = builder.parse(new FileInputStream(input));
		// #2 Creates the XPath
		XPath xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(new CDANameSpaceResolver());

		// #3 Creates resulting Bundle
		Bundle bundle = new Bundle();
		bundle.setType(BundleType.SEARCHSET);
		bundle.setTimestamp(new Date());
		bundle.setLanguage(Configuration.getProperty(Configuration.EHR_LANGUAGE));
		bundle.setId(UUID.randomUUID().toString());


		// #4 Gets the patient from the CDA file
		Node patientNode = (Node)xPath.compile(
				"//cda:ClinicalDocument/cda:recordTarget/cda:patientRole")
				.evaluate(xmlDocument, XPathConstants.NODE);
		// #4.1 adds the patient to the bundle
		Patient subject = CDAConversionUtility.toPatient(patientNode);
		bundle.addEntry().setResource(subject);
		
		// #4.2 Gets the practitioner from the CDA file
		Node practitionerNode = (Node)xPath.compile(
				"//cda:ClinicalDocument/cda:author")
				.evaluate(xmlDocument, XPathConstants.NODE);
		
		// #4.3 adds the practitioner to the bundle
		Practitioner practitioner = CDAConversionUtility.toPractitioner(practitionerNode);
		bundle.addEntry().setResource(practitioner);
		
		// #4.4 Gets the encounter from the CDA file		
		Node encounterNode = (Node)xPath.compile(
				"//cda:ClinicalDocument/cda:componentOf/cda:encompassingEncounter")
				.evaluate(xmlDocument, XPathConstants.NODE);
		
		// #4.5 adds the patient to the bundle
		Encounter encounter = CDAConversionUtility.toEncounter(encounterNode, subject);
		bundle.addEntry().setResource(encounter);
				
		// #5 Gets the sections nodes list from the CDA file
		NodeList sections = (NodeList)xPath.compile("//cda:section")
				.evaluate(xmlDocument, XPathConstants.NODESET);
		
		Node codeNode;
		Node sectionNode;
		String sectionCode;
		for (int i = 0; i < sections.getLength(); i++) {
			sectionNode = sections.item(i);
			codeNode = CDAConversionUtility.getChildByName(sectionNode, "code");
			sectionCode = ((Element)codeNode).getAttribute("code");
			
			if ("30954-2".equals(sectionCode)) {
				addLaboratoryReportToBundle(sectionNode, bundle, subject, practitioner, encounter);				
			} else if ("29548-5".equals(sectionCode)) {
				addConditionToBundle(sectionNode, bundle, subject, practitioner, encounter);
			} if ("62387-6".equals(sectionCode)) {
				addImageReportsToBundle(sectionNode, bundle, subject, practitioner, encounter);
			} if ("8716-3".equals(sectionCode)) {
				addVitalSignsToBundle(sectionNode, bundle, subject, practitioner, encounter);
			} 
		}
		
		// #6 stores bundle to file
		final IParser parser = EHRMWServer.FHIR_CONTEXT.newJsonParser();
		parser.setPrettyPrint(true);
		//final IParser parser = FhirContext.forR4().newJsonParser().setPrettyPrint(true);
		parser.encodeResourceToWriter(bundle, new FileWriter(output));

	}
	
	
	private void addConditionToBundle(Node sectionNode, Bundle bundle, Patient subject,
			Practitioner practitioner, Encounter encounter) throws ParseException {
		
		NodeList entries = ((Element)sectionNode).getElementsByTagName("entry");
		Node obsNode;
		Condition cond;
		for (int i=0; i < entries.getLength(); i++) {
			obsNode = CDAConversionUtility.getChildByName(entries.item(i), "observation");
			cond = CDAConversionUtility.toCondition(obsNode, subject, practitioner, 
					encounter.getPeriod().getStart());
			cond.setEncounter(new Reference(encounter));
			bundle.addEntry().setResource(cond);			
		}
		
	}
	
	
	private void addVitalSignsToBundle(Node sectionNode, Bundle bundle, Patient subject,
			Practitioner practitioner, Encounter encounter) throws ParseException {
		
		// retrieves effective date and time
		NodeList descendants = ((Element)sectionNode).getElementsByTagName("effectiveTime");
		String effectiveTime = ((Element)descendants.item(0)).getAttribute("value");
		
		descendants = ((Element)sectionNode).getElementsByTagName("component");
		Node obsNode;
		Observation obs;
		for (int i=0; i < descendants.getLength(); i++) {
			obsNode = CDAConversionUtility.getChildByName(descendants.item(i), "observation");
			obs = CDAConversionUtility.toVitalSign(obsNode, effectiveTime, subject, practitioner);
			obs.setEncounter(new Reference(encounter));
			bundle.addEntry().setResource(obs);
		}
	}

	
	private void addLaboratoryReportToBundle(Node sectionNode, Bundle bundle, Patient subject,
			Practitioner practitioner, Encounter encounter) throws ParseException {
		// retrieves effective date and time
		NodeList descendants = ((Element)sectionNode).getElementsByTagName("effectiveTime");
		String effectiveTime = ((Element)descendants.item(0)).getAttribute("value");

		descendants = ((Element)sectionNode).getElementsByTagName("code");
		String code = ((Element)descendants.item(0)).getAttribute("code");
		String codeSystem = ((Element)descendants.item(0)).getAttribute("codeSystem");
		String label = ((Element)descendants.item(0)).getAttribute("displayName");
		
		DiagnosticReport labReport = CDAConversionUtility.createLaboratoryReport(
				subject, effectiveTime, codeSystem, code, label, practitioner, encounter);
		
		bundle.addEntry().setResource(labReport);

		descendants = ((Element)sectionNode).getElementsByTagName("component");
		Node obsNode;
		Observation obs;
		for (int i=0; i < descendants.getLength(); i++) {
			obsNode = CDAConversionUtility.getChildByName(descendants.item(i), "observation");
			obs = CDAConversionUtility.toLaboratory(obsNode, effectiveTime, subject, practitioner);
			obs.setEncounter(new Reference(encounter));
			labReport.addResult(new Reference(obs));
			bundle.addEntry().setResource(obs);
		}		
	}

	
	private void addImageReportsToBundle(Node sectionNode, Bundle bundle, Patient subject,
			Practitioner practitioner, Encounter encounter) throws ParseException {
		// Each entries represent a diagnostic examination
		NodeList entries = ((Element)sectionNode).getElementsByTagName("entry");
		for (int i=0; i < entries.getLength(); i++) {
			addImageReportToBundle(entries.item(i), bundle, subject,
					practitioner, encounter);
		}		
	}
	
	
	private void addImageReportToBundle(Node entryNode, Bundle bundle, Patient subject,
			Practitioner practitioner, Encounter encounter) throws ParseException {
		
		Node actNode = CDAConversionUtility.getChildByName(entryNode, "act");
		Node codeNode = CDAConversionUtility.getChildByName(actNode, "code");
		// 8872 ECG, 8952 Echo
		String code = ((Element)codeNode).getAttribute("code");
		if (!"8872".equals(code) && !"8952".equals(code))
			return;

		String codeSystem = ((Element)codeNode).getAttribute("codeSystem");
		String label = ((Element)codeNode).getAttribute("displayName");
		
		// time of the execution of the activity
		Node timeNode = CDAConversionUtility.getChildByName(actNode, "effectiveTime");
		String effectiveTime = ((Element)timeNode).getAttribute("value");

		DiagnosticReport imgReport = CDAConversionUtility.createImageReport(
				subject, effectiveTime, codeSystem, code, label, practitioner, encounter);		
		
		// retrieves effective date and time
		NodeList observations = ((Element)entryNode).getElementsByTagName("observation");
		Node obsNode;
		Node valueNode;
		Observation obs;
		for (int i=0; i < observations.getLength(); i++) {
			obsNode = observations.item(i);
			valueNode = CDAConversionUtility.getChildByName(obsNode, "value");
			if (((Element)valueNode).hasAttribute("mediaType")) {
				Media media = CDAConversionUtility.toMedia(obsNode, subject, practitioner, encounter);
				imgReport.addMedia().setLink(new Reference(media));
				bundle.addEntry().setResource(media);
			} else {
				obs = CDAConversionUtility.toBasicObservation(obsNode, effectiveTime, subject, practitioner);
				obs.setEncounter(new Reference(encounter));
				imgReport.addResult(new Reference(obs));
				bundle.addEntry().setResource(obs);
			}
		}
		
		bundle.addEntry().setResource(imgReport);
		
	}
}
