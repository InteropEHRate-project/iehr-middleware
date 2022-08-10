package eu.interopehrate.r2d.ehr.cda.converter;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: name space resolver for XML/CDA files
 */
public class CDANameSpaceResolver implements NamespaceContext {
	public String getNamespaceURI(String prefix) {
		return "urn:hl7-org:v3";
	}

	public String getPrefix(String namespaceURI) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}
}
