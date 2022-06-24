package eu.interopehrate.r2d.ehr.services.impl;

import java.io.File;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.converter.Converter;
import eu.interopehrate.r2d.ehr.model.EHRFileResponse;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;

public class LocalConversionService {

	private static final String FILE_EXT = ".json";
	private final Logger logger = LoggerFactory.getLogger(LocalConversionService.class);
	
	public EHRResponse convert(EHRRequest ehrRequest, EHRFileResponse ehrResponse,
			Map<String, String> properties) throws Exception {
		// #1 retrieves the name of the bean delegated to the conversion
		String converterName = getConverterName(ehrRequest);
		
		// #2 instantiates the converter bean
		Converter converter = (Converter)EHRContextProvider.
				getApplicationContext().getBean(converterName);
		
		// read generated file extension
		String fileExtension = Configuration.getProperty(Configuration.EHR_FILE_EXT);
		if (!fileExtension.startsWith("."))
			fileExtension = "." + fileExtension;
				
		// #4 builds output file name
		final File output = new File(Configuration.getR2DADBPath() + ehrRequest.getR2dRequestId() + FILE_EXT);

		// #5 invokes conversion
		try {
			logger.info("Invoking local conversion service...");
			converter.convert(ehrResponse.getFirstResponseFile(), output, properties);
			// create response 
			final EHRFileResponse conversionResponse = new EHRFileResponse();
			conversionResponse.setContentType(ContentType.APPLICATION_JSON);
			conversionResponse.addResponseFile(output);
			conversionResponse.setStatus(EHRResponseStatus.COMPLETED);
			logger.info("Local conversion ended succesfully");
			return conversionResponse;
		} catch (Exception e) {
			logger.error("Error '{}' while invoking local conversion service", e.getMessage());
			throw e;			
		}
	}
	
	
	private String getConverterName(EHRRequest ehrRequest) {
		String propertyName = "";
		if (ehrRequest.getOperation() == R2DOperation.SEARCH_ENCOUNTER)
			propertyName = "conversion.encounterlist";
		else if (ehrRequest.getOperation() == R2DOperation.ENCOUNTER_EVERYTHING)
			propertyName = "conversion.encounterEverything";
		else if (ehrRequest.getOperation() == R2DOperation.PATIENT_SUMMARY)
			propertyName = "conversion.patientSummary";
		
		propertyName += ".bean";
		
		String converterName = Configuration.getProperty(propertyName);
		if (converterName == null || converterName.isEmpty())
			throw new IllegalArgumentException(
					String.format("Local converter %s not propely configured.", propertyName));
		
		return converterName;
	}
}
