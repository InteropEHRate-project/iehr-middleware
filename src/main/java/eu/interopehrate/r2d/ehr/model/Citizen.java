package eu.interopehrate.r2d.ehr.model;

import java.time.LocalDate;

public class Citizen {

	public String firstName;
	public String familyName;
	public LocalDate dateOfBirth;
	public String personIdentifier;
	public String gender;
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getFamilyName() {
		return familyName;
	}
	
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}
	
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}
	
	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	
	public String getPersonIdentifier() {
		return personIdentifier;
	}
	
	public void setPersonIdentifier(String personIdentifier) {
		this.personIdentifier = personIdentifier;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
}
