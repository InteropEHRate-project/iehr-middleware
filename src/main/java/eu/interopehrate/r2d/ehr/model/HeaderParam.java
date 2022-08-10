package eu.interopehrate.r2d.ehr.model;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: this class represents an header param to be added to
 * an HTTP request executed by the HttpInvoker.
 */
public class HeaderParam {
	
	private String name;
	private String value;
	
	public HeaderParam(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
