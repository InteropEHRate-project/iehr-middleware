package eu.interopehrate.r2d.ehr.model;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: This class represent an item belonging to the
 * list of elements composing an Encounter. 
 * 
 * When invoking the encounter/everything on the EHR, the result is
 * a list of URLs (each one defined by a type) listing all the content
 * produced during an encounter. 
 * 
 * Every URL, will be submitted to download every item of the encounter.
 */
public class EncounterEverythingItem {
	
	private String type;
	private String description;
	private String uri;
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getUri() {
		return uri;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
}
