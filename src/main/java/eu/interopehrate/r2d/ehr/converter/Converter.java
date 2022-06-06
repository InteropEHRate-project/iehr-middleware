package eu.interopehrate.r2d.ehr.converter;

import java.io.File;

public interface Converter {

	public void convertToFile(File input, File output) throws Exception;
	
}
