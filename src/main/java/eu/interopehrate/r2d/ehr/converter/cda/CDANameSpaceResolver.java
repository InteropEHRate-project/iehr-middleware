package eu.interopehrate.r2d.ehr.converter.cda;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

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
