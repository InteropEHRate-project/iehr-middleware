package eu.interopehrate.r2d.ehr.model;

import java.util.HashMap;
import java.util.Map;

public class EHRRequest {
	public static final String PARAM_FROM = "from";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_RESOURCE_ID = "resourceId";

	private String r2dRequestId;
	private Citizen citizen;
	private Map<String, Object> parameters = new HashMap<>();
	private R2DOperation operation;
	
	@Deprecated
	private EHRRequestStatus status = EHRRequestStatus.NEW;
	@Deprecated
	private EHRResponse ehrResponse;
	@Deprecated
	private EHRResponse ihsResponse;
	
	
	public EHRRequest() {}
	
	public EHRRequest(String r2dRequestId, Citizen theCitizen, R2DOperation operation) {
		super();
		this.r2dRequestId = r2dRequestId;
		this.citizen = theCitizen;
		this.operation = operation;
	}

	public R2DOperation getOperation() {
		return operation;
	}

	public void setOperation(R2DOperation operation) {
		this.operation = operation;
	}

	public String getR2dRequestId() {
		return r2dRequestId;
	}
	
	public void setR2dRequestId(String r2dRequestId) {
		this.r2dRequestId = r2dRequestId;
	}
	
	public Citizen getCitizen() {
		return citizen;
	}
	
	public void setCitizen(Citizen theCitizen) {
		this.citizen = theCitizen;
	}

	public void addParameter(String name, Object parameter) {
		parameters.put(name, parameter);
	}
	
	public Object getParameter(String name) {
		return parameters.get(name);
	}

	public boolean hasParameter(String name) {
		return parameters.containsKey(name);
	}

	@Deprecated
	public EHRRequestStatus getStatus() {
		return status;
	}

	@Deprecated
	public void setStatus(EHRRequestStatus status) {
		this.status = status;
	}

	@Deprecated
	public EHRResponse getEhrResponse() {
		return ehrResponse;
	}

	@Deprecated
	public boolean hasEhrResponse() {
		return ehrResponse != null;
	}

	@Deprecated
	public void setEhrResponse(EHRResponse ehrResponse) {
		this.ehrResponse = ehrResponse;
	}

	@Deprecated
	public EHRResponse getIhsResponse() {
		return ihsResponse;
	}

	@Deprecated
	public boolean hasIhsResponse() {
		return ihsResponse != null;
	}

	@Deprecated
	public void setIhsResponse(EHRResponse ihsResponse) {
		this.ihsResponse = ihsResponse;
	}

	@Override
	public String toString() {
		return "EHRRequest [r2dRequestId=" + r2dRequestId + ", citizen=" + citizen + ", status=" + status
				+ ", operation=" + operation + ", parameters=" + parameters + "]";
	}

}
