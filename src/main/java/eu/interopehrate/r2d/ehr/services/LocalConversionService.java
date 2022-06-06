package eu.interopehrate.r2d.ehr.services;

import java.io.File;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.EHRContextProvider;
import eu.interopehrate.r2d.ehr.converter.Converter;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.model.EHRResponse;
import eu.interopehrate.r2d.ehr.model.EHRResponseStatus;
import eu.interopehrate.r2d.ehr.model.R2DOperation;

public class LocalConversionService {

	private static final String FILE_EXT = ".json";
	private final Logger logger = LoggerFactory.getLogger(LocalConversionService.class);
	
	public EHRResponse convert(EHRRequest ehrRequest, EHRResponse ehrResponse) throws Exception {
		// #1 retrieves the name of the bean delegated to the conversion
		String converterName = getConverterName(ehrRequest);
		
		// #2 instantiates the converter bean
		Converter converter = (Converter)EHRContextProvider.
				getApplicationContext().getBean(converterName);
		
		// #3 build input file name
		String storagePath = Configuration.getDBPath();
		if (!storagePath.endsWith("/"))
			storagePath += "/";
		final File input = new File(storagePath + ehrResponse.getResponse());
		
		// #4 builds output file name
		final File output = new File(Configuration.getR2DADBPath() + ehrRequest.getR2dRequestId() + FILE_EXT);

		// #5 invokes conversion
		try {
			logger.info("Invoking local conversion service...");
			converter.convertToFile(input, output);
			// create response 
			final EHRResponse conversionResponse = new EHRResponse(ContentType.APPLICATION_JSON);
			conversionResponse.setOnFile(true);
			conversionResponse.setResponse(ehrRequest.getR2dRequestId() + FILE_EXT);
			conversionResponse.setStatus(EHRResponseStatus.COMPLETED);
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
