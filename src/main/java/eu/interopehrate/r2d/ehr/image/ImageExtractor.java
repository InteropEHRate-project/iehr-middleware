package eu.interopehrate.r2d.ehr.image;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ImageExtractor extends DefaultHandler {

	
	public void extractImagesFromFile(File input) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		saxParser.parse(input, this);
	}

	@Override
	public void startElement(String uri, String localName, 
			String qName, Attributes attributes) throws SAXException {
		
		if (!"value".equals(localName))
			return;
		
		String mediaType = attributes.getValue("", "mediaType");
		if (!"application/dicom+zip".equals(mediaType))
			return;
		
		System.out.println();
	}
	
	
	
	
}
