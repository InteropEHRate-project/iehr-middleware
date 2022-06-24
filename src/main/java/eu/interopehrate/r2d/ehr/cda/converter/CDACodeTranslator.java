package eu.interopehrate.r2d.ehr.cda.converter;

import java.io.IOException;
import java.util.Properties;

import eu.interopehrate.r2d.ehr.converter.CodeTranslator;

public class CDACodeTranslator implements CodeTranslator {
	
private static Properties translations = new Properties();
	
	static {
		try {
			translations.load(
				CDACodeTranslator.class.getClassLoader().getResourceAsStream("cda_translations_en.properties")
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public String translateCodeLabel(String system, String code) {
		if (system.startsWith("http://"))
			system = system.substring("http://".length());
		
		if (translations.containsKey(system + "|" + code))
			return translations.getProperty(system + "|" + code);
		else 
			return null;
	}

}
