package eu.interopehrate.r2d.ehr.model;

public enum ContentType {
	
	JSON("application/json"), 
	XML("application/xml"), 
	XML_CDA("text/x-cda-r2+xml"), 
	TEXT("text/plain") ;
	
	private String contentType;

	
	private ContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

	public static ContentType getContentType(String contentType) {
		
		for (ContentType type : ContentType.values()) {
			if (type.getContentType().equals(contentType))
				return type;
		}
		
		return null;
	}
	
}
