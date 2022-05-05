package eu.interopehrate.r2d.ehr.model;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Locale.LanguageRange;

public class EHRRequest {
	public static final String PARAM_FROM = "from";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_RESOURCE_ID = "resourceId";

	private String r2dRequestId;
	private Citizen citizen;
	private String preferredLanguages;
	private String preferredLanguage;
	private Map<String, Object> parameters = new HashMap<>();
	private R2DOperation operation;	
	
	public EHRRequest() {}
	
	public EHRRequest(String r2dRequestId, Citizen theCitizen, String preferredLanguages) {
		super();
		this.r2dRequestId = r2dRequestId;
		this.citizen = theCitizen;
		this.preferredLanguages = preferredLanguages;
		
		if (this.preferredLanguages != null && this.preferredLanguages.trim().length() > 1) {
			List<LanguageRange> langs = Locale.LanguageRange.parse(preferredLanguages);
			preferredLanguage = langs.get(0).getRange();
			preferredLanguage = preferredLanguage.substring(0, 2);
		}

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
	
	public String getPreferredLanguages() {
		return preferredLanguages;
	}
	
	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguages(String language) {
		this.preferredLanguages = language;
	}

	@Override
	public String toString() {
		return "EHRRequest [r2dRequestId=" + r2dRequestId + ", citizen=" + citizen + ", preferredLanguages=" + preferredLanguages
				+ ", parameters=" + parameters + ", operation=" + operation + "]";
	}

}
