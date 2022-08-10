package eu.interopehrate.r2d.ehr.converter;

import java.io.File;
import java.util.Map;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of a local converter.
 */
public interface Converter {

	public static final String PATIENT_ID_KEY = "PATIENT_ID";
	
	/**
	 * 
	 * @param input: input file containing data to be parsed
	 * @param output: output file used to store results
	 * @param properties: additional properties provided to the converter
	 * 
	 * @throws Exception
	 */
	public void convert(File input, File output, 
			Map<String, String> properties) throws Exception;
	
}
