package eu.interopehrate.r2d.ehr.model;

public enum ContentType {
	
	JSON_FHIR("application/json"), 
	XML("text/xml"), 
	XML_CDA("text/x-cda-r2+xml"), 
	TEXT("text/plain") ;
	
	private String contentType;

	
	private ContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

}
