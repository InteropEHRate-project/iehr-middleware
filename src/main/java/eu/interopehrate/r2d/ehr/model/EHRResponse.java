package eu.interopehrate.r2d.ehr.model;

import org.apache.http.entity.ContentType;

public class EHRResponse {
	
	private ContentType contentType;
	private EHRResponseStatus status = EHRResponseStatus.COMPLETED;
	private String response = "";
	private String message = "";
	
	public EHRResponse() {
		super();
	}

	public EHRResponse(ContentType contentType) {
		super();
		this.contentType = contentType;
	}
	
	public EHRResponse(ContentType contentType, String message) {
		super();
		this.contentType = contentType;
		this.message = message;
	}
	
	public EHRResponse(ContentType contentType, EHRResponseStatus status) {
		super();
		this.contentType = contentType;
		this.status = status;
	}

	public EHRResponse(ContentType contentType, EHRResponseStatus status, String message) {
		super();
		this.contentType = contentType;
		this.status = status;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		if (message != null)
			this.message = message;
	}

	public ContentType getContentType() {
		return contentType;
	}
	
	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}
	
	public String getResponse() {
		return response;
	}
	
	public void setResponse(String response) {
		if (response != null)
			this.response = response;
	}

	public EHRResponseStatus getStatus() {
		return status;
	}

	public void setStatus(EHRResponseStatus status) {
		this.status = status;
	}
	
}
