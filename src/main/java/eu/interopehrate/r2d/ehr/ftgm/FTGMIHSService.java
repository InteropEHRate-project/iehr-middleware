package eu.interopehrate.r2d.ehr.ftgm;

import java.io.File;

import org.apache.commons.text.StringTokenizer;

import eu.interopehrate.r2d.ehr.Configuration;
import eu.interopehrate.r2d.ehr.model.EHRRequest;
import eu.interopehrate.r2d.ehr.services.impl.RestIHSService;

public class FTGMIHSService extends RestIHSService {

	
	
	/*
	 * Builds the param string needed when invoking the service for converting results. 
	 * This parameter is built searching specific string(s) inside the CDA file 
	 * containing the CDA Document that must be sent to IHS.
	 * 
	 * The list of codes to be searched is stored in the configuration file
	 */
	protected String detectConversionTargetForEncounterEverything(EHRRequest ehrRequest, 
			File ehrFile) throws Exception {
		StringTokenizer codes = new StringTokenizer(Configuration.getProperty(Configuration.IHS_MAPPING_CODES), ";");
		StringBuffer paramString = new StringBuffer();		
		Process process;
		String currentCode;
		boolean isFirst = true;

		final String cdaFileName = ehrFile.getAbsolutePath();
		while (codes.hasNext()) {
			currentCode = codes.next();
			process = Runtime.getRuntime().exec(String.format("grep -i %s %s", currentCode, cdaFileName));
			if (process.waitFor() == 0) {
				if (!isFirst)
					paramString.append(";");
				paramString.append(Configuration.getProperty("ihs.mapping.code." + currentCode));
				isFirst = false;
			}
		}
		
		return paramString.toString();
	}
	
}
