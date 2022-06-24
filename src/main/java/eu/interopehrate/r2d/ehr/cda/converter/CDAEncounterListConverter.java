package eu.interopehrate.r2d.ehr.cda.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRMWServer;
import eu.interopehrate.r2d.ehr.converter.Converter;


public class CDAEncounterListConverter implements Converter {
	
	@Override
	public void convert(File input, File output, 
			Map<String, String> properties) throws Exception {
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


		// #4 Gets the patient from CDA file
		Node patientNode = (Node)xPath.compile(
				"//cda:ClinicalDocument/cda:recordTarget/cda:patientRole")
				.evaluate(xmlDocument, XPathConstants.NODE);
		// #4.1 adds the patient to the bundle
		Patient subject = CDAConversionUtility.toPatient(patientNode);
		// bundle.addEntry().setResource(subject);
		
		// #5 Gets the encounters nodes from CDA file
		NodeList nodes = (NodeList)xPath.compile("//cda:encounter")
				.evaluate(xmlDocument, XPathConstants.NODESET);
		
		// #6 add Encounters to the bundle
		addEncountersToBundle(nodes, bundle, subject);
		
		// #7 stores bundle to file
		final IParser parser = EHRMWServer.FHIR_CONTEXT.newJsonParser();
		parser.encodeResourceToWriter(bundle, new FileWriter(output));

	}
	
	
	private void addEncountersToBundle(NodeList encounters, Bundle bundle, Patient subject) throws ParseException {
		Encounter e;
	    for (int i = 0; i < encounters.getLength(); i++) {
		      e = CDAConversionUtility.toEncounter(encounters.item(i), subject);		      
		      bundle.addEntry().setResource(e);
	    }
	}

}
