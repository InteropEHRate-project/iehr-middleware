package eu.interopehrate.r2d.ehr.converter;

public interface CodeTranslator {
	
	String translateCodeLabel(String system, String code);

}
