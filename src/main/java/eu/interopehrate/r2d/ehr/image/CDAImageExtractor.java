package eu.interopehrate.r2d.ehr.image;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import eu.interopehrate.r2d.ehr.Configuration;

public class CDAImageExtractor extends DefaultHandler implements ImageExtractor {
	private static final double KILOBYTE = 1024D;
	private static final String DICOM_ZIP_MIME = "application/dicom+zip";
	private static final String DICOM_MIME = "application/dicom";
	private static final String XML_HL7_MIME = "application/xml+hl7";
	
	private Logger logger = LoggerFactory.getLogger(CDAImageExtractor.class);
	// attrs needed for CDA file parsing
	private boolean namespaceBegin = false;
	private String currentNamespace;
	private String currentNamespaceUri;
	@SuppressWarnings("unused")
	private Locator locator;
	private boolean parsingValue = false;
	private boolean foundMediaType = false;
	private boolean placeholderWritten = true;
	private int imageCounter;

	// attrs needed for request management
	private String requestId;
	private String ehrMWStoragePath;
	private String r2daStoragePath;
	private String fileExtension;
		
	// attrs needed for file management
	private PrintWriter reducedFileWriter;
	private PrintWriter imageFileWriter;
	private Encoder encoder;

	@Override
	public void extractImages(String requestId) throws Exception {
		this.requestId = requestId;

		// Retrieves storage path from config file
		ehrMWStoragePath = Configuration.getDBPath();
		if (!ehrMWStoragePath.endsWith("/"))
			ehrMWStoragePath += "/";
		
		r2daStoragePath = Configuration.getR2DADBPath();
		if (!r2daStoragePath.endsWith("/"))
			r2daStoragePath += "/";
		
		// Retrieves file extension
		fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;

		// retrieves Base64Encoder
		encoder = Base64.getEncoder();
				
		String fileToReduceName = requestId + fileExtension;
		// Creates input file
		File fileToReduce = new File(ehrMWStoragePath + fileToReduceName);
		
		// Checks if the file contains DICOM images
		if (!needsToExtractImages(fileToReduce.getAbsolutePath())) {
			logger.info("File: {} does not need to be reduced, it does not contain images", 
					fileToReduceName);
		}
		
		// Creates reduced file name
		File reducedFile = new File(ehrMWStoragePath + requestId + "_reduced" + fileExtension);
		reducedFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(reducedFile)));
		
		try (InputStream input = new BufferedInputStream(new FileInputStream(fileToReduce));) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser saxParser = factory.newSAXParser();
			logger.info("Starting reduction of file: {}, inital size: {} Kb", 
					fileToReduceName, NumberFormat.getInstance().format(fileToReduce.length() / KILOBYTE));
			saxParser.parse(input, this);
			logger.info("Reduction of file ended, current size: {} Kb", 
					NumberFormat.getInstance().format(reducedFile.length() / KILOBYTE));
		} catch (Exception e) {
			throw e;
		} 
		
	}

	
	private boolean needsToExtractImages(String cdaFileName) throws Exception {
		Process process = Runtime.getRuntime().exec(String.format("grep -i %s %s", DICOM_ZIP_MIME, cdaFileName));
		if (process.waitFor() == 0)
			return true;
		
		process = Runtime.getRuntime().exec(String.format("grep -i %s %s", DICOM_MIME, cdaFileName));
		if (process.waitFor() == 0)
			return true;

		process = Runtime.getRuntime().exec(String.format("grep -i %s %s", XML_HL7_MIME, cdaFileName));
		if (process.waitFor() == 0)
			return true;
		
		return false;
	}
	
	
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	@Override
	public void startDocument() {
		reducedFileWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	}

	@Override
	public void endDocument() {
		reducedFileWriter.flush();
		reducedFileWriter.close();
	}

	
	@Override
	public void startPrefixMapping(String prefix, String uri) {
		namespaceBegin = true;
		currentNamespace = prefix;
		currentNamespaceUri = uri;
	}

	
	@Override
	public void endPrefixMapping(String prefix) {}

	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
		if ("value".equals(qName))
			this.parsingValue = true;
		
		reducedFileWriter.print("<" + qName);
		if (namespaceBegin) {
			reducedFileWriter.print(" xmlns:" + currentNamespace + "=\"" + currentNamespaceUri + "\"");
			namespaceBegin = false;
		}
		for (int i = 0; i < atts.getLength(); i++) {
			reducedFileWriter.print(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
			
			
			if ("mediaType".equals(atts.getQName(i))) {
				foundMediaType = true;
				imageCounter++;
			}
		}
		reducedFileWriter.print(">");
	}

	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		reducedFileWriter.print("</" + qName + ">");
		if ("value".equals(qName)) {
			parsingValue = false;
			foundMediaType = false;
			placeholderWritten = false;
			if (imageFileWriter != null) {
				imageFileWriter.flush();
				imageFileWriter.close();
				imageFileWriter = null;
			}				
		}
	}

	
	@Override
	public void characters(char[] ch, int start, int length) {
		if (parsingValue && foundMediaType) {
			if (!placeholderWritten) {
				String imageName = String.format("imagePlaceholder%d", imageCounter);
				reducedFileWriter.print(encoder.encodeToString(imageName.getBytes()));
				placeholderWritten = true;
				// creates new output file for storing the image
				try {
					imageFileWriter = new PrintWriter(new BufferedWriter(
							new FileWriter(r2daStoragePath + requestId + "_" + imageName)));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			// write image char to file
			for (int i = start; i < start + length; i++)
				imageFileWriter.print(ch[i]);
		} else {
			StringBuilder sb = new  StringBuilder();
			for (int i = start; i < start + length; i++)
				sb.append(ch[i]);
			
			reducedFileWriter.print(StringEscapeUtils.escapeXml11(sb.toString()));						
		}
	}

	
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
		System.out.println("ignorableWhitespace");
		for (int i = start; i < start + length; i++)
			reducedFileWriter.print(ch[i]);
	}

	@Override
	public void processingInstruction(String target, String data) {
		reducedFileWriter.print("<?" + target + " " + data + "?>");
	}

	@Override
	public void skippedEntity(String name) {
		reducedFileWriter.print("&" + name + ";");
	}

}