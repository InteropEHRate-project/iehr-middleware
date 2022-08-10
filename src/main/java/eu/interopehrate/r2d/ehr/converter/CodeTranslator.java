package eu.interopehrate.r2d.ehr.converter;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of a code translator.
 */
public interface CodeTranslator {
	
	String translateCodeLabel(String system, String code);

}
